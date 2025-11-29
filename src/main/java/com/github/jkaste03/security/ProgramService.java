package com.github.jkaste03.security;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.dto.request.ProgramCreateRequest;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.exceptions.ForbiddenException;
import com.github.jkaste03.exceptions.ResourceNotFoundException;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.repository.ProgramRepository;
import com.github.jkaste03.service.ProgramResponseMapper;

@Service
public class ProgramService {

    private static final String PROBLEM_NOT_FOUND_PROGRAM = "problem.not-found.program";
    private static final String PROBLEM_FORBIDDEN_PROGRAM_ACCESS = "problem.forbidden.program-access";

    private final ProgramRepository programRepository;
    private final ProgramResponseMapper programResponseMapper;

    public ProgramService(ProgramRepository programRepository, ProgramResponseMapper programResponseMapper) {
        this.programRepository = programRepository;
        this.programResponseMapper = programResponseMapper;
    }

    @Transactional(readOnly = true)
    public @NonNull ProgramResponse getProgramDetails(long id, long userId) {
        Program program = requireOwnedProgram(id, userId);
        return programResponseMapper.toDetailed(program);
    }

    private @NonNull Program requireOwnedProgram(long programId, long userId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(PROBLEM_NOT_FOUND_PROGRAM, programId));

        if (!program.getDevice().getOwner().getId().equals(userId)) {
            throw new ForbiddenException(PROBLEM_FORBIDDEN_PROGRAM_ACCESS, programId);
        }
        return program;
    }

    @Transactional
    public @NonNull ProgramResponse updateProgram(long id, ProgramCreateRequest updateRequest, long userId) {
        Program program = requireOwnedProgram(id, userId);

        program.setName(updateRequest.name());
        program.setPublic(updateRequest.isPublic());

        return programResponseMapper.toDetailed(program);
    }

    @Transactional
    public void deleteProgram(long id, long userId) {
        Program program = requireOwnedProgram(id, userId);
        program.getDevice().removeProgram(program);
        programRepository.delete(program);
    }
}
