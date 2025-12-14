package com.github.jkaste03.controller;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.jkaste03.controller.hateoas.ProgramModelAssembler;
import com.github.jkaste03.dto.request.ProgramUpdateRequest;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.security.UserPrincipal;
import com.github.jkaste03.service.ProgramService;

import jakarta.validation.Valid;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/remote-program-control/api/v1/programs")
public class ProgramController {

    private final ProgramService programService;
    private final ProgramModelAssembler programModelAssembler;

    public ProgramController(ProgramService programService, ProgramModelAssembler programModelAssembler) {
        this.programService = programService;
        this.programModelAssembler = programModelAssembler;
    }

    @GetMapping("{id}")
    public EntityModel<ProgramResponse> getProgramDetails(@PathVariable long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        String userId = principal.id();
        return programModelAssembler.toModel(programService.getProgramDetails(id, userId));
    }

    @PutMapping("{id}")
    public EntityModel<ProgramResponse> updateProgram(@PathVariable long id,
            @Valid @RequestBody ProgramUpdateRequest updateRequest,
            @AuthenticationPrincipal UserPrincipal principal) {
        String userId = principal.id();

        return programModelAssembler.toModel(programService.updateProgram(id, updateRequest, userId));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        String userId = principal.id();

        programService.deleteProgram(id, userId);
        return ResponseEntity.noContent().build();
    }
}