// package com.github.jkaste03.controller;

// import com.github.jkaste03.dto.ProgramDto;
// import com.github.jkaste03.security.SecurityUtils;
// import com.github.jkaste03.service.ProgramService;

// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.Authentication;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import java.util.List;
// import java.util.Set;

// @RestController
// @RequestMapping("/remote-program-control/api/v1/programs")
// public class ProgramController {

// private final ProgramService programService;

// public ProgramController(ProgramService programService) {
// this.programService = programService;
// }

// @GetMapping
// public ResponseEntity<List<ProgramDto>> getPrograms(Authentication
// authentication) {
// Set<String> userRoleNames = SecurityUtils.extractRoleNames(authentication);

// List<ProgramDto> programs = programService.getProgramsByRoles(userRoleNames);
// return ResponseEntity.ok(programs);
// }
// }
