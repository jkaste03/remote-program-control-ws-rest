package com.github.jkaste03.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.jkaste03.model.Device;

public interface DeviceRepository extends JpaRepository<Device, Long> {
}