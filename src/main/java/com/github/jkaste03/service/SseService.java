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
import com.github.jkaste03.event.WhitelistChangedEvent;
import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.repository.DeviceRepository;
import com.github.jkaste03.repository.ProgramRepository;

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    private static final long WAIT_TIMEOUT_SECONDS = 45;
    private static final long GIVE_UP_TIMEOUT_SECONDS = 150; // 2.5 minutes from the first WoL attempt
    private static final int MAX_ATTEMPTS = 2;
    private static final long SHUTDOWN_DELAY_SECONDS = 30;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<Long, Instant> emitterConnectedAt = new ConcurrentHashMap<>();
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

    public synchronized void addEmitter(long deviceId, SseEmitter emitter) {
        Instant connectedAt = Instant.now();

        // Replace any previous stream for this device and close it. The old
        // stream's callbacks must not remove the new stream from the map.
        SseEmitter previousEmitter = emitters.put(deviceId, emitter);
        emitterConnectedAt.put(deviceId, connectedAt);

        deviceService.updateDeviceStatus(deviceId, Device.Status.ONLINE);
        log.info("Device {} connected via SSE. Status set to ONLINE.", deviceId);

        Runnable removeAndSetOffline = () -> removeEmitterIfCurrent(deviceId, emitter);

        emitter.onCompletion(removeAndSetOffline);
        emitter.onTimeout(removeAndSetOffline);
        emitter.onError(e -> removeAndSetOffline.run());

        log.info("Device {} connected via SSE", deviceId);

        if (previousEmitter != null && previousEmitter != emitter) {
            log.info("Closing previous SSE connection for device {}", deviceId);
            previousEmitter.complete();
        }

        // Check: is there a pending command for this device?
        checkPendingAndExecute(deviceId);
    }

    /**
     * Remove a stream only if it is still the active stream for this device.
     * This prevents an older connection's callback from deleting a newer one.
     */
    private synchronized void removeEmitterIfCurrent(long deviceId, SseEmitter emitter) {
        if (!emitters.remove(deviceId, emitter)) {
            log.debug("Ignoring cleanup for superseded SSE connection of device {}", deviceId);
            return;
        }

        emitterConnectedAt.remove(deviceId);
        deviceService.updateDeviceStatus(deviceId, Device.Status.OFFLINE);
        log.info("Device {} disconnected. Status set to OFFLINE.", deviceId);
    }

    public void addDashboardEmitter(SseEmitter emitter) {
        String id = UUID.randomUUID().toString();
        dashboardEmitters.put(id, emitter);

        Runnable remove = () -> dashboardEmitters.remove(id);

        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
    }

    public void sendWhitelistChanged(Program program) {
        SseEmitter emitter = emitters.get(program.getDevice().getId());
        if (emitter != null) {
            sendToEmitter(program.getDevice().getId(), emitter, SseEvent.WHITELIST_CHANGED,
                    Map.of("programId", program.getId(), "filename", program.getFilename()));
        }
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

    @TransactionalEventListener
    public void handleWhitelistChanged(WhitelistChangedEvent event) {
        sendWhitelistChanged(event.getProgram());
    }

    private void checkAndScheduleShutdown(Long deviceId) {
        Instant scheduledAt = Instant.now();
        scheduler.schedule(() -> {
            try {
                // 1. Sjekk om enheten fortsatt er ONLINE
                if (!emitters.containsKey(deviceId)) {
                    log.info("Device {} is offline, skipping shutdown check.", deviceId);
                    return;
                }

                // 1b. Hvis en ny tilkobling skjedde etter at vi planla dette, hopp over
                Instant connectedAt = emitterConnectedAt.get(deviceId);
                if (connectedAt != null && connectedAt.isAfter(scheduledAt)) {
                    log.info("Device {} reconnected after shutdown was scheduled. Skipping shutdown.", deviceId);
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
            try {
                sendToEmitter(device.getId(), emitter, event, data);
                return;
            } catch (SseDeliveryException ex) {
                // Treat as offline if a fresh disconnect happened during send
                if (event != SseEvent.START_PROGRAM) {
                    throw ex;
                }
                log.info("Delivery to device {} failed mid-send. Falling back to WoL queue.", device.getId());
            }
        } else {
            // Device is OFFLINE
        }

        // Offline handling for START_PROGRAM
        if (event == SseEvent.START_PROGRAM) {
            log.info("Device {} is offline. Initiating WoL flow.", device.getId());
            wolService.sendMagicPacket(device.getMacAddress(), device.getPublicIp(), device.getWakeOnLanPort());

            long programId = extractProgramId(data);
            PendingKey key = new PendingKey(device.getId(), programId);
            Instant firstAttempt = Instant.now();
            pendingCommands.put(key, new PendingCommand(
                    device, programId, event, data, 1, firstAttempt, firstAttempt, onFailure));
        } else {
            // STOP or others -> fail fast
            log.warn("Cannot send event '{}' to offline device {}", event, device.getId());
            throw new SseDeliveryException("Device is offline. Cannot perform " + event.eventName());
        }
    }

    private void sendToEmitter(long deviceId, SseEmitter emitter, @NonNull SseEvent event, @NonNull Object data) {
        try {
            emitter.send(SseEmitter.event().name(event.eventName()).data(data));
            log.info("Sent event '{}' to device {}", event, deviceId);
        } catch (IOException e) {
            log.warn("Failed to send event to device {}. Removing emitter if it is still current.", deviceId);
            removeEmitterIfCurrent(deviceId, emitter);
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
            long secondsSinceFirstAttempt = ChronoUnit.SECONDS.between(cmd.firstAttempt(), now);

            if (secondsSinceFirstAttempt > GIVE_UP_TIMEOUT_SECONDS) {
                // Give up after the total time allowed for the device to come online
                log.warn("Device {} program {} did not connect after {} attempts. Giving up command.", key.deviceId(),
                        cmd.programId(),
                        cmd.attemptCount());
                pendingCommands.remove(key);

                // Run callback
                if (cmd.onFailure() != null) {
                    cmd.onFailure().run();
                }
            } else if (cmd.attemptCount() < MAX_ATTEMPTS && secondsSinceLastAttempt > WAIT_TIMEOUT_SECONDS) {
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
                        cmd.firstAttempt(), cmd.onFailure()));
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
                log.info("Heartbeat failed for device {}. Marking as OFFLINE if this stream is still current.", deviceId);
                removeEmitterIfCurrent(deviceId, emitter);
            }
        });
    }

    @Scheduled(fixedRate = 30000)
    public void sendDashboardHeartbeat() {
        dashboardEmitters.forEach((id, emitter) -> {
            try {
                emitter.send(
                        SseEmitter.event().comment("keepalive"));
            } catch (Exception e) {
                dashboardEmitters.remove(id, emitter);
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
            Instant firstAttempt,
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
