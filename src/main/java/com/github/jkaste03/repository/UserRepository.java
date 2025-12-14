/**
 * 
 */
package com.github.jkaste03.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.jkaste03.model.User;

/**
 * 
 */
public interface UserRepository extends JpaRepository<User, String> {
}