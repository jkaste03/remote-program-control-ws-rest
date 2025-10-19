package com.github.jkaste03.repository;

import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.Role;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByAllowedRolesIn(Set<Role> roles);
}
