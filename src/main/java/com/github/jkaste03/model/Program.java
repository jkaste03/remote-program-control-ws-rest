package com.github.jkaste03.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "program_seq")
    @SequenceGenerator(name = "program_seq", sequenceName = "program_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "program_roles", joinColumns = @JoinColumn(name = "program_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private final Set<Role> allowedRoles = new HashSet<>();

    protected Program() {
        /* for JPA */ }

    public Program(String name) {
        this.name = name;
    }

    public Program(String name, Set<Role> allowedRoles) {
        this(name);
        setAllowedRoles(allowedRoles);
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
        return Collections.unmodifiableSet(allowedRoles);
    }

    public void setAllowedRoles(Set<Role> allowedRoles) {
        this.allowedRoles.clear();
        if (allowedRoles != null) {
            allowedRoles.forEach(this::addAllowedRole);
        }
    }

    public void addAllowedRole(Role role) {
        this.allowedRoles.add(role);
    }

    public void removeAllowedRole(Role role) {
        this.allowedRoles.remove(role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (!Hibernate.getClass(this).equals(Hibernate.getClass(o))) {
            return false;
        }
        Program other = (Program) o;
        if (this.id == null || other.id == null) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return (id != null) ? id.hashCode() : 31;
    }

    @Override
    public String toString() {
        return "Program{name='%s', allowedRoles=%d}".formatted(name, allowedRoles.size());
    }
}
