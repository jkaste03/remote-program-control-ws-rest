/**
 * 
 */
package com.github.jkaste03.repository;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

import com.github.jkaste03.model.User;

/**
 * 
 */
public interface UserRepository extends CrudRepository<User, Long> {
	Optional<User> findByEmail(String email);
}