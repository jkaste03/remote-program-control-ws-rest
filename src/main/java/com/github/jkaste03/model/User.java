package com.github.jkaste03.model;

import java.util.Objects;
import java.util.UUID;

import org.hibernate.Hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "users")
public class User {

	@Id
	@Column(name = "id", length = 36) // UUID har 36 tegn
	private UUID id;

	@Column(nullable = false, length = 255, unique = true)
	private String email;

	@Column(nullable = false, length = 255, unique = true)
	private String username;

	// @Column(nullable = false, length = 64)
	// @JsonIgnore
	// private String password;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role"))
	@NotNull
	private Role role;

	protected User() {
		/* for JPA */ }

	public User(UUID id, String email, String username, Role role) {
		this.id = id;
		this.email = email;
		this.username = username;
		this.role = role;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
			return false;
		}
		User other = (User) o;
		return Objects.equals(this.id, other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	@Override
	public String toString() {
		return "User{id=%s, email='%s'}".formatted(id, email);
	}
}
