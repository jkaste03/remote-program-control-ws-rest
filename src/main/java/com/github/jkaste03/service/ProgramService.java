package com.github.jkaste03.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.dto.request.ProgramCreateUpdateRequest;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.exceptions.ConflictException;
import com.github.jkaste03.exceptions.ForbiddenException;
import com.github.jkaste03.exceptions.ResourceNotFoundException;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.Program.Status;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.ProgramRepository;
import com.github.jkaste03.repository.UserRepository;

@Service
public class ProgramService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    private static final String PROBLEM_NOT_FOUND_PROGRAM = "problem.not-found.program";
    private static final String PROBLEM_FORBIDDEN_PROGRAM_ACCESS = "problem.forbidden.program-access";
    private static final String PROBLEM_CONFLICT_START_IN_PROGRESS_OR_STARTED = "program.conflict.start-in-progress-or-started";
    private static final String PROBLEM_CONFLICT_STOP_IN_PROGRESS_OR_STOPPED = "program.conflict.stop-in-progress-or-stopped";
    private static final String PROBLEM_NOT_FOUND_USERS = "problem.not-found.users";

    private final ProgramRepository programRepository;
    private final ProgramResponseMapper programResponseMapper;
    private final ProgramUniquenessValidator programUniquenessValidator;
    private final ProgramAccessService programAccessService;
    private final SseService sseService;
    private final UserRepository userRepository;

    @Autowired
    @Lazy
    private ProgramService self; // For å kalle oss selv med transaksjon

    public ProgramService(ProgramRepository programRepository, ProgramResponseMapper programResponseMapper,
            ProgramUniquenessValidator programUniquenessValidator, ProgramAccessService programAccessService,
            SseService sseService, UserRepository userRepository) {
        this.programRepository = programRepository;
        this.programResponseMapper = programResponseMapper;
        this.programUniquenessValidator = programUniquenessValidator;
        this.programAccessService = programAccessService;
        this.sseService = sseService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public @NonNull ProgramResponse getProgramDetails(long id, UUID userId) {
        Program program = requireOwnedProgram(id, userId);
        return programResponseMapper.toDetailed(program);
    }

    @Transactional
    public @NonNull ProgramResponse updateProgram(long id, ProgramCreateUpdateRequest updateRequest, UUID userId) {
        Program program = requireOwnedProgram(id, userId);

        programUniquenessValidator.requireUniqueNameAndFilenameForDevice(program.getDevice(), updateRequest.filename(),
                updateRequest.name(), program);

        program.setName(updateRequest.name());
        program.setFilename(updateRequest.filename());
        program.setPublic(updateRequest.isPublic());

        // Sjekk allowedUserIds
        if (!updateRequest.allowedUserIds().isEmpty()) {
            List<User> foundUsers = userRepository.findAllById(updateRequest.allowedUserIds());

            // SJEKK: Fant vi like mange brukere som vi spurte etter?
            if (foundUsers.size() != updateRequest.allowedUserIds().size()) {
                // Alternativ A: Kast exception (Anbefalt)
                // Du kan lage en ny melding i messages.properties for dette, f.eks:
                // problem.not-found.users=One or more user IDs were not found.
                throw new ResourceNotFoundException(PROBLEM_NOT_FOUND_USERS);
            }

            program.clearAllowedUsers();
            foundUsers.forEach(program::allow);
        } else {
            // Hvis listen er tom, betyr det "fjern alle"
            program.clearAllowedUsers();
        }

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

        // Throw exception if not stopped
        if (!program.getStatus().equals(Status.STOPPED)) {
            throw new ConflictException(PROBLEM_CONFLICT_START_IN_PROGRESS_OR_STARTED, program.getName());
        }

        program.setStatus(Status.TRYING_TO_START);
        programRepository.save(program);

        // Definer hva som skal skje hvis det feiler asynkront (timeout)
        Runnable onAsyncFailure = () -> self.revertProgramStatus(program.getId(), Status.STOPPED);

        // Kall SSE-tjenesten
        // Hvis denne feiler synkront (IOException), kastes SseDeliveryException. Da
        // ruller hele denne metoden tilbake, og status "TRYING_TO_START" blir aldri
        // lagret fast.
        sseService.sendEvent(
                program.getDevice(),
                SseEvent.START_PROGRAM,
                program.getFilename(),
                onAsyncFailure // <--- Sender med oppryddings-jobben
        );

        return programResponseMapper.toSummary(program);
    }

    // Ny hjelpemetode for å rulle tilbake status asynkront
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revertProgramStatus(long programId, Status status) {
        programRepository.findById(programId).ifPresent(p -> {
            log.warn("Reverting status for program {} to {}", p.getName(), status);
            p.setStatus(status);
            programRepository.save(p);
        });
    }

    @Transactional
    public ProgramResponse stopProgram(long id, UUID userId, boolean isAdmin) {
        Program program = requireAccessibleProgram(id, userId, isAdmin);

        // Throw exception if not started
        if (!program.getStatus().equals(Status.STARTED)) {
            throw new ConflictException(PROBLEM_CONFLICT_STOP_IN_PROGRESS_OR_STOPPED, program.getName());
        }
        program.setStatus(Status.TRYING_TO_STOP);
        programRepository.save(program);

        // Kall SSE-tjenesten
        // Hvis denne feiler synkront (IOException), kastes SseDeliveryException. Da
        // ruller hele denne metoden tilbake, og status "TRYING_TO_STOP" blir aldri
        // lagret fast.
        sseService.sendEvent(
                program.getDevice(),
                SseEvent.STOP_PROGRAM,
                program.getFilename());

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
