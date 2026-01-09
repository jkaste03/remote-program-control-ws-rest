package com.github.jkaste03.service;

public enum SseEvent {
    START_PROGRAM,
    STOP_PROGRAM,
    SHUTDOWN;

    public String eventName() {
        return name();
    }
}
