package com.github.jkaste03.model;

import org.hibernate.Hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "program_seq")
    @SequenceGenerator(name = "program_seq", sequenceName = "program_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allowed_role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_program_allowed_role"))
    @NotNull
    private Role allowedRole;

    protected Program() {
        /* for JPA */ }

    public Program(String name, Role allowedRole) {
        this.name = name;
        this.allowedRole = allowedRole;
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

    public Role getAllowedRole() {
        return allowedRole;
    }

    public void setAllowedRole(Role allowedRole) {
        this.allowedRole = allowedRole;
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
        return "Program [id=" + id + ", name=" + name + ", allowedRole=" + allowedRole + "]";
    }
}
