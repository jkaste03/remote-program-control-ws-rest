package com.github.jkaste03.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.github.jkaste03.model.Device;

import org.springframework.data.jpa.repository.EntityGraph;

public interface DeviceRepository extends JpaRepository<Device, Long> {
     boolean existsByOwnerIdAndName(UUID ownerId, String name);

     boolean existsByOwnerIdAndMacAddress(UUID ownerId, String macAddress);

     @EntityGraph(attributePaths = { "programs", "programs.allowedUsers", "owner" })
     @Query("select d from Device d")
     List<Device> findAllWithPrograms();

     @EntityGraph(attributePaths = { "programs", "programs.allowedUsers", "owner" })
     @Query("""
               select d from Device d
               where d.owner.id = :userId
                  or exists (
                       select 1 from Program p
                       where p.device = d and p.isPublic = true
                  )
                  or exists (
                       select 1 from Program p join p.allowedUsers u
                       where p.device = d and u.id = :userId
                  )
               """)
     List<Device> findDevicesUserCanInteractWithPrefetched(UUID userId);
}
