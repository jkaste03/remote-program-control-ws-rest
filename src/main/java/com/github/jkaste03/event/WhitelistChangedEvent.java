package com.github.jkaste03.event;

import org.springframework.context.ApplicationEvent;

import com.github.jkaste03.model.Program;

public class WhitelistChangedEvent extends ApplicationEvent {
    private final Program program;

    public WhitelistChangedEvent(Object source, Program program) {
        super(source);
        this.program = program;
    }

    public Program getProgram() {
        return program;
    }
}
