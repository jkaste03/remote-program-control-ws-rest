package com.github.jkaste03.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.Program;
import com.github.jkaste03.repository.ProgramRepository;

import java.util.List;
import java.util.Set;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;

    public ProgramService(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    @Transactional(readOnly = true)
    public List<Program> getProgramsByRoles(Set<String> userRoles) {
        return programRepository.findAllByAllowedRoleNames(userRoles);
    }
}
