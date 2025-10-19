package com.github.jkaste03.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.jkaste03.service.ProgramService;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.Role;
import com.github.jkaste03.security.CurrentUserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/remote-program-control/api/v1/programs")
public class ProgramController {
    private final ProgramService programService;
    private final CurrentUserService currentUserService;

    public ProgramController(ProgramService programService, CurrentUserService currentUserService) {
        this.programService = programService;
        this.currentUserService = currentUserService;
    }

    @GetMapping()
    public ResponseEntity<List<Program>> getPrograms() {
        Set<Role> roles = currentUserService.getRoles();
        List<Program> programs = programService.getProgramsByRoles(roles);
        return new ResponseEntity<>(programs, HttpStatus.OK);
    }
}
