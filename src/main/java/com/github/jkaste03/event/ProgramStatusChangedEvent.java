package com.github.jkaste03.event;

import com.github.jkaste03.model.Program;
import org.springframework.context.ApplicationEvent;

public class ProgramStatusChangedEvent extends ApplicationEvent {
    private final Program program;

    public ProgramStatusChangedEvent(Object source, Program program) {
        super(source);
        this.program = program;
    }

    public Program getProgram() {
        return program;
    }
}
