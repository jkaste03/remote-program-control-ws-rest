package com.github.jkaste03.security;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.dto.response.DeviceResponse;
import com.github.jkaste03.model.Device;
import com.github.jkaste03.repository.ProgramRepository;

@Service
public class ProgramService {
    private final ProgramRepository programRepository;

    public ProgramService(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }
}
