package com.github.jkaste03.controller;

import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.jkaste03.controller.hateoas.ProgramModelAssembler;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.security.ProgramService;
import com.github.jkaste03.security.UserPrincipal;

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
        long userId = principal.id();

        return programModelAssembler.toModel(programService.getProgramDetails(id, userId));
    }
}
