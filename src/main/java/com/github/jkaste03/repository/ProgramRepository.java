package com.github.jkaste03.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.github.jkaste03.model.Program;

public interface ProgramRepository extends CrudRepository<Program, Long> {
    List<Program> findAllByDeviceId(Long deviceId);
}
