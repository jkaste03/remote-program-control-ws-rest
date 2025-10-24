package com.github.jkaste03.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "userid")
	private Long id;

	@Column(name = "firstname", nullable = false, length = 50)
	private String firstName;

	@Column(name = "lastname", nullable = false, length = 50)
	private String lastName;

	@Column(nullable = false, length = 50, unique = true)
	private String email;

	// @Column(nullable = false, length = 64)
	// @JsonIgnore
	// private String password;
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private final Set<Role> roles = new HashSet<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference(value = "user-devices")
	private final Set<Device> devices = new HashSet<>();

	protected User() {
		/* for JPA */ }

	public User(String email, String firstName, String lastName) {
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public Long getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Set<Role> getRoles() {
		return Collections.unmodifiableSet(roles);
	}

	public void setRoles(Set<Role> roles) {
		this.roles.clear();
		if (roles != null) {
			roles.forEach(this::addRole);
		}
	}

	public void addRole(Role role) {
		if (role != null) {
			this.roles.add(role);
		}
	}

	public void removeRole(Role role) {
		this.roles.remove(role);
	}

	public Set<Device> getDevices() {
		return Collections.unmodifiableSet(devices);
	}

	public void setDevices(Set<Device> devices) {
		this.devices.clear();
		devices.forEach(this::addDevice);
	}

	public void addDevice(Device device) {
		if (device == null || devices.contains(device)) {
			return;
		}
		devices.add(device);
		device.setUser(this);
	}

	public void removeDevice(Device device) {
		if (device == null) {
			return;
		}
		if (devices.remove(device)) {
			device.setUser(null);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof User user)) {
			return false;
		}
		return Objects.equals(email, user.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(email);
	}

	@Override
	public String toString() {
		return "User{id=%d, email='%s'}".formatted(id, email);
	}
}
