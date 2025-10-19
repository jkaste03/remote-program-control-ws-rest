package com.github.jkaste03.controller;

import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.Role;
import com.github.jkaste03.security.UserDetailsImpl;
import com.github.jkaste03.service.ProgramService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/remote-program-control/api/v1/programs")
public class ProgramController {
    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @GetMapping()
    public ResponseEntity<List<Program>> getPrograms(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        Set<Role> roles = currentUser.getRoles();
        List<Program> programs = programService.getProgramsByRoles(roles);
        return new ResponseEntity<>(programs, HttpStatus.OK);
    }
}
