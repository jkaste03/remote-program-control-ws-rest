package com.github.jkaste03.service;

import java.util.Objects;
import java.util.UUID;

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
    private final ProgramAccessService programAccessService;
    private final SseService sseService;

    public ProgramService(ProgramRepository programRepository,
            ProgramResponseMapper programResponseMapper,
            ProgramUniquenessValidator programUniquenessValidator,
            ProgramAccessService programAccessService,
            SseService sseService) {
        this.programRepository = programRepository;
        this.programResponseMapper = programResponseMapper;
        this.programUniquenessValidator = programUniquenessValidator;
        this.programAccessService = programAccessService;
        this.sseService = sseService;
    }

    @Transactional(readOnly = true)
    public @NonNull ProgramResponse getProgramDetails(long id, UUID userId) {
        Program program = requireOwnedProgram(id, userId);
        return programResponseMapper.toDetailed(program);
    }

    @Transactional
    public @NonNull ProgramResponse updateProgram(long id, ProgramUpdateRequest updateRequest, UUID userId) {
        Program program = requireOwnedProgram(id, userId);

        programUniquenessValidator.requireUniqueNameAndFilenameForDevice(program.getDevice(), updateRequest.filename(),
                updateRequest.name(), program);

        program.setName(updateRequest.name());
        program.setFilename(updateRequest.filename());
        program.setPublic(updateRequest.isPublic());

        return programResponseMapper.toDetailed(program);
    }

    @Transactional
    public void deleteProgram(long id, UUID userId) {
        Program program = requireOwnedProgram(id, userId);
        program.getDevice().removeProgram(program);
        programRepository.delete(program);
    }

    private @NonNull Program requireOwnedProgram(long programId, UUID userId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(PROBLEM_NOT_FOUND_PROGRAM, programId));

        if (!Objects.equals(program.getDevice().getOwner().getId(), userId)) {
            throw new ForbiddenException(PROBLEM_FORBIDDEN_PROGRAM_ACCESS, programId);
        }
        return program;
    }

    @Transactional
    public void startProgram(long id, UUID userId, boolean isAdmin) {
        Program program = requireAccessibleProgram(id, userId, isAdmin);

        // Send beskjed til maskinen via SSE. Data-objektet her kan være hva som helst,
        // f.eks. bare filnavnet eller en liten JSON
        sseService.sendEventToDevice(program.getDevice().getId(), "START_PROGRAM", program.getFilename());

        program.setStatus(Program.Status.STARTED);
        programRepository.save(program);
    }

    @Transactional
    public void stopProgram(long id, UUID userId, boolean isAdmin) {
        Program program = requireAccessibleProgram(id, userId, isAdmin);

        sseService.sendEventToDevice(program.getDevice().getId(), "STOP_PROGRAM", program.getFilename());

        program.setStatus(Program.Status.STOPPED);
        programRepository.save(program);
    }

    private Program requireAccessibleProgram(long programId, UUID userId, boolean isAdmin) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(PROBLEM_NOT_FOUND_PROGRAM, programId));

        if (!isAdmin && !programAccessService.hasAccess(program, userId)) {
            throw new ForbiddenException(PROBLEM_FORBIDDEN_PROGRAM_ACCESS, programId);
        }
        return program;
    }
}
