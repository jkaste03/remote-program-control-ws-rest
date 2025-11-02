/**
 * 
 */
package com.github.jkaste03.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.github.jkaste03.model.Role;

/**
 * 
 */
public interface RoleRepository extends CrudRepository<Role, Long> {
    Optional<Role> findByName(String name);

}