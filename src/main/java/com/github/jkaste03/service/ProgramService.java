package com.github.jkaste03.service;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.dto.request.ProgramUpdateRequest;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.exceptions.ForbiddenException;
import com.github.jkaste03.exceptions.ResourceNotFoundException;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.repository.ProgramRepository;

@Service
public class ProgramService {

    private static final String PROBLEM_NOT_FOUND_PROGRAM = "problem.not-found.program";
    private static final String PROBLEM_FORBIDDEN_PROGRAM_ACCESS = "problem.forbidden.program-access";

    private final ProgramRepository programRepository;
    private final ProgramResponseMapper programResponseMapper;
    private final ProgramUniquenessValidator programUniquenessValidator;

    public ProgramService(ProgramRepository programRepository,
            ProgramResponseMapper programResponseMapper,
            ProgramUniquenessValidator programUniquenessValidator) {
        this.programRepository = programRepository;
        this.programResponseMapper = programResponseMapper;
        this.programUniquenessValidator = programUniquenessValidator;
    }

    @Transactional(readOnly = true)
    public @NonNull ProgramResponse getProgramDetails(long id, long userId) {
        Program program = requireOwnedProgram(id, userId);
        return programResponseMapper.toDetailed(program);
    }

    private @NonNull Program requireOwnedProgram(long programId, long userId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(PROBLEM_NOT_FOUND_PROGRAM, programId));

        if (!Objects.equals(program.getDevice().getOwner().getId(), userId)) {
            throw new ForbiddenException(PROBLEM_FORBIDDEN_PROGRAM_ACCESS, programId);
        }
        return program;
    }

    @Transactional
    public @NonNull ProgramResponse updateProgram(long id, ProgramUpdateRequest updateRequest, long userId) {
        Program program = requireOwnedProgram(id, userId);

        programUniquenessValidator.requireUniqueNameAndFilenameForDevice(program.getDevice(), updateRequest.filename(),
                updateRequest.name(), program);

        program.setName(updateRequest.name());
        program.setFilename(updateRequest.filename());
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
