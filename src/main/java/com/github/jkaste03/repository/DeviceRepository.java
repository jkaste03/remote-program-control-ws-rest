package com.github.jkaste03.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.jkaste03.model.Device;
import com.github.jkaste03.view.DeviceProgramView;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    @Query("""
                select
                  d.id   as deviceId,
                  d.name as deviceName,
                  p.id   as programId,
                  p.name as programName,
                  r.name as roleName
                from Device d
                join d.programs p
                join p.allowedRole r
                where lower(r.name) in :roleNames
                order by d.id, p.id
            """)
    List<DeviceProgramView> findDevicesWithProgramsByRoleNames(@Param("roleNames") Set<String> roleNamesLowercased);
}