package com.github.jkaste03.service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    private static final long WAIT_TIMEOUT_SECONDS = 45;
    private static final int MAX_ATTEMPTS = 2;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> dashboardEmitters = new ConcurrentHashMap<>();

    private final Map<Long, PendingCommand> pendingCommands = new ConcurrentHashMap<>();

    private final WakeOnLanService wolService;
    private final DeviceService deviceService;

    public SseService(WakeOnLanService wolService, DeviceService deviceService) {
        this.wolService = wolService;
        this.deviceService = deviceService;
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

    // TODO: Consider @TransactionalEventListener if needed
    @TransactionalEventListener
    public void handleDeviceStatusChanged(DeviceStatusChangedEvent event) {
        Device device = event.getDevice();
        broadcastToDashboards("DEVICE_STATUS", Map.of(
                "deviceId", device.getId(),
                "status", device.getStatus()));
    }

    @TransactionalEventListener
    public void handleProgramStatusChanged(ProgramStatusChangedEvent event) {
        Program program = event.getProgram();
        broadcastToDashboards("PROGRAM_STATUS", Map.of(
                "programId", program.getId(),
                "deviceId", program.getDevice().getId(),
                "status", program.getStatus()));
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

                pendingCommands.put(device.getId(), new PendingCommand(
                        device, event, data, 1, Instant.now(), onFailure));
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
        PendingCommand command = pendingCommands.remove(deviceId);
        if (command != null) {
            log.info("Found pending command for device {}. Executing now.", deviceId);
            // Since we now know that the emitter exists (the method is called from
            // addEmitter), we can safely get it again.
            SseEmitter emitter = emitters.get(deviceId);
            if (emitter != null) {
                sendToEmitter(deviceId, emitter, command.event(), command.data());
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    public void monitorPendingCommands() {
        Instant now = Instant.now();

        pendingCommands.forEach((deviceId, cmd) -> {
            long secondsSinceLastAttempt = ChronoUnit.SECONDS.between(cmd.lastAttempt(), now);

            if (secondsSinceLastAttempt > WAIT_TIMEOUT_SECONDS) {
                if (cmd.attemptCount() < MAX_ATTEMPTS) {
                    // Retry
                    log.info("Timeout waiting for device {}. Attempt {}/{}", deviceId, cmd.attemptCount() + 1,
                            MAX_ATTEMPTS);

                    wolService.sendMagicPacket(
                            cmd.device().getMacAddress(),
                            cmd.device().getPublicIp(),
                            cmd.device().getWakeOnLanPort());

                    // Update state
                    pendingCommands.put(deviceId, new PendingCommand(
                            cmd.device(), cmd.event(), cmd.data(), cmd.attemptCount() + 1, Instant.now(),
                            cmd.onFailure()));
                } else {
                    // Give up
                    log.warn("Device {} did not connect after {} attempts. Giving up command.", deviceId,
                            MAX_ATTEMPTS);
                    pendingCommands.remove(deviceId);

                    // Run callback
                    if (cmd.onFailure() != null) {
                        cmd.onFailure().run();
                    }
                }
            }
        });
    }

    @Scheduled(fixedRate = 30000) // Send heartbeat every 30 seconds
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
            @NonNull SseEvent event,
            @NonNull Object data,
            int attemptCount,
            Instant lastAttempt,
            Runnable onFailure // Run on timeout
    ) {
    }
}