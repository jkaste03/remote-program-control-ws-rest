package com.github.jkaste03.model;

import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

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

	@Column(nullable = false, length = 50, unique = true)
	private String email;

	// @Column(nullable = false, length = 64)
	// @JsonIgnore
	// private String password;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role"))
	@NotNull
	private Role role;

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
