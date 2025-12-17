package com.github.jkaste03.service;

import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.dto.request.ProgramUpdateRequest;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.exceptions.ConflictException;
import com.github.jkaste03.exceptions.ForbiddenException;
import com.github.jkaste03.exceptions.ResourceNotFoundException;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.Program.Status;
import com.github.jkaste03.repository.ProgramRepository;

@Service
public class ProgramService {

    private static final String COMMAND_START_PROGRAM = "START_PROGRAM";
    private static final String COMMAND_STOP_PROGRAM = "STOP_PROGRAM";

    private static final String PROBLEM_NOT_FOUND_PROGRAM = "problem.not-found.program";
    private static final String PROBLEM_FORBIDDEN_PROGRAM_ACCESS = "problem.forbidden.program-access";
    private static final String PROBLEM_CONFLICT_START_IN_PROGRESS_OR_STARTED = "program.conflict.start-in-progress-or-started";
    private static final String PROBLEM_CONFLICT_STOP_IN_PROGRESS_OR_STOPPED = "program.conflict.stop-in-progress-or-stopped";

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
    public ProgramResponse startProgram(long id, UUID userId, boolean isAdmin) {
        Program program = requireAccessibleProgram(id, userId, isAdmin);

        // SJEKK: Er det allerede startet?
        if (EnumSet.of(Status.TRYING_TO_START, Status.STARTED).contains(program.getStatus())) {
            throw new ConflictException(PROBLEM_CONFLICT_START_IN_PROGRESS_OR_STARTED, program.getName());
        }

        program.setStatus(Status.TRYING_TO_START);
        programRepository.save(program);
        // Send beskjed til maskinen via SSE. Data-objektet her kan være hva som helst,
        // f.eks. bare filnavnet eller en liten JSON
        sseService.sendEventToDevice(program.getDevice().getId(), COMMAND_START_PROGRAM, program.getFilename());

        return programResponseMapper.toSummary(program);
    }

    @Transactional
    public ProgramResponse stopProgram(long id, UUID userId, boolean isAdmin) {
        Program program = requireAccessibleProgram(id, userId, isAdmin);

        // SJEKK: Er det allerede stoppet?
        if (EnumSet.of(Status.TRYING_TO_STOP, Status.STOPPED).contains(program.getStatus())) {
            throw new ConflictException(PROBLEM_CONFLICT_STOP_IN_PROGRESS_OR_STOPPED, program.getName());
        }
        program.setStatus(Status.TRYING_TO_STOP);
        programRepository.save(program);

        sseService.sendEventToDevice(program.getDevice().getId(), COMMAND_STOP_PROGRAM, program.getFilename());

        return programResponseMapper.toSummary(program);
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
