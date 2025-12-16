package com.github.jkaste03.service;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.github.jkaste03.model.Program;

@Service
public class ProgramAccessService {

    /**
     * Sjekker om en bruker har tilgang til et program basert på:
     * 1. Eierskap av enheten
     * 2. Om programmet er public
     * 3. Om brukeren ligger i listen over allowedUsers
     */
    public boolean hasAccess(Program program, UUID userId) {

        // 1. Eier av enheten har alltid tilgang
        if (Objects.equals(program.getDevice().getOwner().getId(), userId)) {
            return true;
        }

        // 2. Hvis programmet er public (OBS: Vurder om public skal gi kjøre-tilgang
        // eller bare lese-tilgang)
        if (program.isPublic()) {
            return true;
        }

        // 3. Sjekk om brukeren er eksplisitt gitt tilgang
        return program.getAllowedUsers().stream()
                .anyMatch(u -> Objects.equals(u.getId(), userId));
    }
}