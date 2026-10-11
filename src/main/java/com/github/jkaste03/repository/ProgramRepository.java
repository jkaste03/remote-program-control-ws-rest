package com.github.jkaste03.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;

import com.github.jkaste03.model.Program;

public interface ProgramRepository extends CrudRepository<Program, Long> {
    List<Program> findAllByDeviceId(Long deviceId);

    Optional<Program> findByDeviceIdAndFilename(Long deviceId, String filename);

    List<Program> findAllByAllowedUsersId(UUID userId);

    @Query("select p from Program p where p.device.owner.id = :userId")
    List<Program> findAllByDeviceOwnerId(UUID userId);
}
