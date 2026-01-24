package com.github.jkaste03.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.Hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "programs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_program_device_filename", columnNames = { "device_id", "filename" }),
        @UniqueConstraint(name = "uk_program_device_name", columnNames = { "device_id", "name" })
})
public class Program {

    public enum Status {
        TRYING_TO_START,
        STARTED,
        STOPPED,
        TRYING_TO_STOP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "program_seq")
    @SequenceGenerator(name = "program_seq", sequenceName = "program_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32) // longest enum literal is TRYING_TO_START (15 chars)
    private Status status = Status.STOPPED;

    @ManyToMany
    @JoinTable(name = "program_allowed_users", joinColumns = @JoinColumn(name = "program_id", foreignKey = @ForeignKey(name = "fk_prog_allowed_prog")), inverseJoinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_prog_allowed_user")), indexes = {
            @Index(name = "idx_prog_allowed_program_id", columnList = "program_id"),
            @Index(name = "idx_prog_allowed_user_id", columnList = "user_id")
    })
    private Set<User> allowedUsers = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false, foreignKey = @ForeignKey(name = "fk_program_device"))
    @NotNull
    private Device device;

    protected Program() {
        /* for JPA */ }

    public Program(String filename) {
        this.filename = filename;
        this.name = filename;
    }

    public Program(String filename, boolean isPublic) {
        this.filename = filename;
        this.name = filename;
        this.isPublic = isPublic;
    }

    public Program(String filename, String name, boolean isPublic) {
        this.filename = filename;
        this.name = name;
        this.isPublic = isPublic;
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String name) {
        this.filename = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device newDevice) {
        if (this.device == newDevice) {
            return; // ingenting å gjøre
        }

        if (this.device != null) {
            this.device.getPrograms().remove(this);
        }

        this.device = newDevice;

        // Legg til på ny device (unngå duplikater)
        if (newDevice != null && !newDevice.getPrograms().contains(this)) {
            newDevice.getPrograms().add(this);
        }
    }

    public Set<User> getAllowedUsers() {
        return allowedUsers;
    }

    public void allow(User u) {
        allowedUsers.add(u);
    }

    public void disallow(User u) {
        allowedUsers.remove(u);
    }

    public void clearAllowedUsers() {
        allowedUsers.clear();
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
        return "Program [id=" + id + ", filename=" + filename + ", name=" + name + ", isPublic=" + isPublic + "]";
    }
}
