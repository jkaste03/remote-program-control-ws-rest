package com.github.jkaste03.repository;

import com.github.jkaste03.model.Program;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    // Krever at Program har many-to-many til Role med "allowedRoles" og Role har
    // "name"
    @Query("""
            select distinct p from Program p
            join p.allowedRoles r
            where upper(r.name) in :roleNames
            """)
    List<Program> findAllByAllowedRoleNames(@Param("roleNames") Set<String> roleNamesUpper);
}
