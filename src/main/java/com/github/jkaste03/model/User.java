package com.github.jkaste03.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.NaturalId;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "device_seq")
	@SequenceGenerator(name = "device_seq", sequenceName = "device_seq", allocationSize = 50)
	private Long id;

	@Column(name = "firstname", nullable = false, length = 50)
	private String firstName;

	@Column(name = "lastname", nullable = false, length = 50)
	private String lastName;

	@NaturalId
	@Column(nullable = false, length = 120, unique = true)
	private String email;

	// @Column(nullable = false, length = 64)
	// @JsonIgnore
	// private String password;

	@OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST, orphanRemoval = true, fetch = FetchType.LAZY)
	private Set<UserRole> roles = new HashSet<>();

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

	// --- Helper-metoder (hold begge sider i sync) ---
	public boolean addRole(Role role) {
		boolean exists = roles.stream().anyMatch(ur -> ur.getRole().equals(role));
		if (exists)
			return false;
		roles.add(new UserRole(this, role));
		return true;
	}

	public boolean removeRole(Role role) {
		return roles.removeIf(ur -> ur.getRole().equals(role));
	}

	// Ikke gi ut den muterbare samlingen direkte:
	public Set<UserRole> getUserRoles() {
		return Set.copyOf(roles);
	}

	// Praktisk “view” for lesing/DTO:
	@Transient
	public Set<String> getRoleNames() {
		return roles.stream().map(ur -> ur.getRole().getName()).collect(java.util.stream.Collectors.toSet());
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
