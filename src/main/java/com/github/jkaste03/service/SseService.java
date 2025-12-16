package com.github.jkaste03.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    // Mapper deviceId -> Emitter.
    // ConcurrentHashMap er viktig for trådsikkerhet.
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void addEmitter(long deviceId, SseEmitter emitter) {
        emitters.put(deviceId, emitter);

        // Rydd opp når koblingen avsluttes (viktig for å unngå minnelekkasje)
        emitter.onCompletion(() -> emitters.remove(deviceId));
        emitter.onTimeout(() -> emitters.remove(deviceId));
        emitter.onError(e -> emitters.remove(deviceId));
    }

    public void sendEventToDevice(long deviceId, @NonNull String eventName, @NonNull Object data) {
        SseEmitter emitter = emitters.get(deviceId);
        if (emitter == null) {
            log.debug("No listener registered for device {}", deviceId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException _) {
            log.warn("Could not send event to device {}. Removing emitter.", deviceId);
            emitters.remove(deviceId);
        }
    }
}