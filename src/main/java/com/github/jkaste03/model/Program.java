/**
 * 
 */
package com.github.jkaste03.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * @author tdoy
 */
@Entity
@Table(name = "programs")
public class Program {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // Roles that are allowed to access/use this program
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "program_roles", joinColumns = @JoinColumn(name = "program_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> allowedRoles = new HashSet<>();

    public Program() {
        // JPA default constructor
    }

    public Program(String name, Set<Role> allowedRoles) {
        this.name = name;
        this.allowedRoles = allowedRoles;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Role> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(Set<Role> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public void addAllowedRole(Role role) {
        this.allowedRoles.add(role);
    }

    public void removeAllowedRole(Role role) {
        this.allowedRoles.remove(role);
    }
}
