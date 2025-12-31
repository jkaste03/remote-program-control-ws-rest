package com.github.jkaste03.event;

import com.github.jkaste03.model.Device;
import org.springframework.context.ApplicationEvent;

public class DeviceStatusChangedEvent extends ApplicationEvent {
    private final Device device;

    public DeviceStatusChangedEvent(Object source, Device device) {
        super(source);
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }
}
