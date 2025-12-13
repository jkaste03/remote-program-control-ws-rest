package com.github.jkaste03.service;

import java.util.Objects;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.github.jkaste03.exceptions.ConflictException;
import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Program;

@Component
public class ProgramUniquenessValidator {

    private static final String PROBLEM_PROGRAM_FILENAME_CONFLICT = "program.device-filename-conflict";
    private static final String PROBLEM_PROGRAM_NAME_CONFLICT = "program.device-name-conflict";

    public void requireUniqueNameAndFilenameForDevice(Device device, String desiredFilename, String desiredName,
            Program currentProgram) {
        if (device == null) {
            return;
        }

        String filenameCandidate = desiredFilename != null ? desiredFilename : currentFilename(currentProgram);
        if (hasDuplicate(device, currentProgram, filenameCandidate, Program::getFilename)) {
            throw new ConflictException(PROBLEM_PROGRAM_FILENAME_CONFLICT, filenameCandidate, device.getName());
        }

        String nameCandidate = desiredName != null ? desiredName : currentName(currentProgram);
        if (hasDuplicate(device, currentProgram, nameCandidate, Program::getName)) {
            throw new ConflictException(PROBLEM_PROGRAM_NAME_CONFLICT, nameCandidate, device.getName());
        }
    }

    private String currentFilename(Program currentProgram) {
        return currentProgram != null ? currentProgram.getFilename() : null;
    }

    private String currentName(Program currentProgram) {
        return currentProgram != null ? currentProgram.getName() : null;
    }

    private boolean hasDuplicate(Device device, Program currentProgram, String desiredValue,
            Function<Program, String> extractor) {
        if (desiredValue == null || device.getPrograms() == null) {
            return false;
        }

        return device.getPrograms()
                .stream()
                .filter(existing -> !isSameEntity(existing, currentProgram))
                .map(extractor)
                .filter(Objects::nonNull)
                .anyMatch(desiredValue::equals);
    }

    private boolean isSameEntity(Program existing, Program currentProgram) {
        return currentProgram != null && Objects.equals(existing.getId(), currentProgram.getId());
    }

    public void requireUniqueNameAndFilenameForDevice(Device device,
            String filename) {
        requireUniqueNameAndFilenameForDevice(device, filename, filename, null);
    }
}
