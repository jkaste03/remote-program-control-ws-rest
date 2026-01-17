package com.github.jkaste03.service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.github.jkaste03.event.DeviceStatusChangedEvent;
import com.github.jkaste03.event.ProgramStatusChangedEvent;
import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.repository.DeviceRepository;
import com.github.jkaste03.repository.ProgramRepository;

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    private static final long WAIT_TIMEOUT_SECONDS = 45;
    private static final long GIVE_UP_TIMEOUT_SECONDS = 300; // 5 minutes
    private static final int MAX_ATTEMPTS = 2;
    private static final long SHUTDOWN_DELAY_SECONDS = 30;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> dashboardEmitters = new ConcurrentHashMap<>();

    private final Map<PendingKey, PendingCommand> pendingCommands = new ConcurrentHashMap<>();

    private final WakeOnLanService wolService;
    private final DeviceService deviceService;
    private final ProgramRepository programRepository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SseService(WakeOnLanService wolService, DeviceService deviceService,
            ProgramRepository programRepository) {
        this.wolService = wolService;
        this.deviceService = deviceService;
        this.programRepository = programRepository;
    }

    public void addEmitter(long deviceId, SseEmitter emitter) {
        emitters.put(deviceId, emitter);

        // 1. Enheten har koblet til -> Sett ONLINE
        deviceService.updateDeviceStatus(deviceId, Device.Status.ONLINE);
        log.info("Device {} connected via SSE. Status set to ONLINE.", deviceId);

        // 2. Rydd opp og sett OFFLINE når koblingen dør
        Runnable removeAndSetOffline = () -> {
            emitters.remove(deviceId);
            deviceService.updateDeviceStatus(deviceId, Device.Status.OFFLINE);
            log.info("Device {} disconnected. Status set to OFFLINE.", deviceId);
        };

        // Rydd opp når koblingen avsluttes (viktig for å unngå minnelekkasje)
        emitter.onCompletion(removeAndSetOffline);
        emitter.onTimeout(removeAndSetOffline);
        emitter.onError(e -> removeAndSetOffline.run());

        log.info("Device {} connected via SSE", deviceId);

        // Check: is there a pending command for this device?
        checkPendingAndExecute(deviceId);
    }

    public void addDashboardEmitter(SseEmitter emitter) {
        String id = UUID.randomUUID().toString();
        dashboardEmitters.put(id, emitter);

        Runnable remove = () -> dashboardEmitters.remove(id);

        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
    }

    @TransactionalEventListener
    public void handleDeviceStatusChanged(DeviceStatusChangedEvent event) {
        Device device = event.getDevice();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("deviceId", device.getId());
        payload.put("status", device.getStatus());
        if (device.getLastSeen() != null) {
            payload.put("lastSeen", device.getLastSeen().toString());
        }

        broadcastToDashboards("DEVICE_STATUS", payload);
    }

    @TransactionalEventListener
    public void handleProgramStatusChanged(ProgramStatusChangedEvent event) {
        Program program = event.getProgram();
        broadcastToDashboards("PROGRAM_STATUS", Map.of(
                "programId", program.getId(),
                "deviceId", program.getDevice().getId(),
                "status", program.getStatus()));

        // Sjekk om vi skal initiere shutdown
        if (program.getStatus() == Program.Status.STOPPED) {
            checkAndScheduleShutdown(program.getDevice().getId());
        }
    }

    private void checkAndScheduleShutdown(Long deviceId) {
        scheduler.schedule(() -> {
            try {
                // 1. Sjekk om enheten fortsatt er ONLINE
                if (!emitters.containsKey(deviceId)) {
                    log.info("Device {} is offline, skipping shutdown check.", deviceId);
                    return;
                }

                // 2. Sjekk om ALLE programmer er STOPPED
                // Vi bruker repository direkte for å unngå potensiell transaksjonstrøbbel fra
                // DeviceService
                // i en asynkron tråd. readOnly=true på repos er default nok her.
                boolean anyRunning = programRepository.findAllByDeviceId(deviceId).stream()
                        .anyMatch(p -> p.getStatus() == Program.Status.STARTED
                                || p.getStatus() == Program.Status.TRYING_TO_START
                                || p.getStatus() == Program.Status.TRYING_TO_STOP);

                if (!anyRunning) {
                    SseEmitter emitter = emitters.get(deviceId);
                    if (emitter != null) {
                        log.info("All programs stopped for device {}. Sending SHUTDOWN command.", deviceId);
                        sendToEmitter(deviceId, emitter, SseEvent.SHUTDOWN, "{}");
                    }
                }
            } catch (Exception e) {
                log.error("Error during shutdown check/execution for device " + deviceId, e);
            }
        }, SHUTDOWN_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void broadcastToDashboards(String eventName, Object data) {
        dashboardEmitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                // Emitter is dead, remove it
                dashboardEmitters.remove(id);
            }
        });
    }

    public void sendEvent(Device device, @NonNull SseEvent event, @NonNull Object data) {
        sendEvent(device, event, data, null);
    }

    /**
     * Try to send event. If the device is offline and the event is a start program
     * command, WoL procedure is initiated.
     */
    public void sendEvent(Device device, @NonNull SseEvent event, @NonNull Object data, Runnable onFailure) {
        SseEmitter emitter = emitters.get(device.getId());

        if (emitter != null) {
            sendToEmitter(device.getId(), emitter, event, data);
        } else {
            // Device is OFFLINE

            if (event == SseEvent.START_PROGRAM) {
                // 2. Start command -> Queue and initiate WoL
                log.info("Device {} is offline. Initiating WoL flow.", device.getId());
                wolService.sendMagicPacket(device.getMacAddress(), device.getPublicIp(), device.getWakeOnLanPort());

                long programId = extractProgramId(data);
                PendingKey key = new PendingKey(device.getId(), programId);
                pendingCommands.put(key, new PendingCommand(
                        device, programId, event, data, 1, Instant.now(), onFailure));
            } else {
                // 3. STOP command (or others) -> Fail fast!
                // Vi kan ikke stoppe en PC som ikke responderer. Kaster exception for å rulle
                // tilbake transaksjonen.
                log.warn("Cannot send event '{}' to offline device {}", event, device.getId());
                throw new SseDeliveryException("Device is offline. Cannot perform " + event.eventName());
            }
        }
    }

    private void sendToEmitter(long deviceId, SseEmitter emitter, @NonNull SseEvent event, @NonNull Object data) {
        try {
            emitter.send(SseEmitter.event().name(event.eventName()).data(data));
            log.info("Sent event '{}' to device {}", event, deviceId);
        } catch (IOException e) {
            log.warn("Failed to send event to device {}. Removing emitter.", deviceId);
            emitters.remove(deviceId);
            deviceService.updateDeviceStatus(deviceId, Device.Status.OFFLINE);
            // Throw exception to make ProgramService's transaction rollback
            throw new SseDeliveryException("Failed to deliver SSE to device " + deviceId);
        }
    }

    private void checkPendingAndExecute(long deviceId) {
        SseEmitter emitter = emitters.get(deviceId);
        if (emitter == null) {
            return;
        }

        pendingCommands.entrySet().removeIf(entry -> {
            if (entry.getKey().deviceId() != deviceId) {
                return false;
            }
            PendingCommand command = entry.getValue();
            log.info("Found pending command for device {} program {}. Executing now.", deviceId, command.programId());
            sendToEmitter(deviceId, emitter, command.event(), command.data());
            return true;
        });
    }

    @Scheduled(fixedRate = 5000)
    public void monitorPendingCommands() {
        Instant now = Instant.now();

        pendingCommands.forEach((key, cmd) -> {
            long secondsSinceLastAttempt = ChronoUnit.SECONDS.between(cmd.lastAttempt(), now);

            if (cmd.attemptCount() < MAX_ATTEMPTS && secondsSinceLastAttempt > WAIT_TIMEOUT_SECONDS) {
                // Retry (e.g. send a second WoL after 45 seconds)
                log.info("Timeout waiting for device {} program {}. Attempt {}/{}", key.deviceId(), cmd.programId(),
                        cmd.attemptCount() + 1,
                        MAX_ATTEMPTS);

                wolService.sendMagicPacket(
                        cmd.device().getMacAddress(),
                        cmd.device().getPublicIp(),
                        cmd.device().getWakeOnLanPort());

                // Update state
                pendingCommands.put(key, new PendingCommand(
                        cmd.device(), cmd.programId(), cmd.event(), cmd.data(), cmd.attemptCount() + 1, Instant.now(),
                        cmd.onFailure()));
            } else if (cmd.attemptCount() >= MAX_ATTEMPTS && secondsSinceLastAttempt > GIVE_UP_TIMEOUT_SECONDS) {
                // Give up after extended wait following the final attempt
                log.warn("Device {} program {} did not connect after {} attempts. Giving up command.", key.deviceId(),
                        cmd.programId(),
                        MAX_ATTEMPTS);
                pendingCommands.remove(key);

                // Run callback
                if (cmd.onFailure() != null) {
                    cmd.onFailure().run();
                }
            }
        });
    }

    @Scheduled(fixedRate = 60000) // Send heartbeat every 60 seconds
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }
        log.debug("Sending heartbeat to {} devices", emitters.size());
        emitters.forEach((deviceId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("PING").data("ping"));
            } catch (Exception e) {
                log.info("Heartbeat failed for device {}. Marking as OFFLINE.", deviceId);
                // Remove and update status immediately
                emitters.remove(deviceId);
                deviceService.updateDeviceStatus(deviceId, Device.Status.OFFLINE);
            }
        });
    }

    // Legg til en Exception-klasse for å signalisere sendefeil
    public static class SseDeliveryException extends RuntimeException {
        public SseDeliveryException(String message) {
            super(message);
        }
    }

    // Simple record for pending command state
    private record PendingCommand(
            Device device,
            long programId,
            @NonNull SseEvent event,
            @NonNull Object data,
            int attemptCount,
            Instant lastAttempt,
            Runnable onFailure // Run on timeout
    ) {
    }

    private record PendingKey(long deviceId, long programId) {
    }

    private long extractProgramId(Object data) {
        if (data instanceof com.github.jkaste03.dto.response.ProgramCommandDto dto) {
            return dto.id();
        }
        log.warn("Unable to extract program id from pending command payload {}", data);
        return -1L;
    }
}
