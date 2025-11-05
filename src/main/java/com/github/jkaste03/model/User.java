package com.github.jkaste03.model;

import org.hibernate.Hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_auth_provider_subject", columnNames = { "auth_provider", "auth_subject" }),
}, indexes = {
		@Index(name = "idx_user_auth", columnList = "auth_provider,auth_subject")
})
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
	@SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 50)
	private Long id;

	@Column(nullable = false, length = 120, unique = true)
	private String email;

	@Column(nullable = false, length = 80, unique = true)
	private String username;

	@Column(name = "auth_provider", length = 200, nullable = true) // settes ved første innlogging
	private String authProvider; // = jwt.getIssuer().toString()

	@Column(name = "auth_subject", length = 200, nullable = true)
	private String authSubject; // = jwt.getSubject()

	// @Column(nullable = false, length = 64)
	// @JsonIgnore
	// private String password;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role"))
	private Role role;

	protected User() {
		/* for JPA */ }

	public User(String email, String username, Role role) {
		this.email = email;
		this.username = username;
		this.role = role;
	}

	public User(String email, String username, String authProvider, String authSubject, Role role) {
		this(email, username, role);
		this.authProvider = authProvider;
		this.authSubject = authSubject;
	}

	public void linkIdentityIfMissing(String issuer, String subject) {
		if (this.authProvider == null)
			this.authProvider = issuer;
		if (this.authSubject == null)
			this.authSubject = subject;
	}

	public Long getId() {
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
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (!Hibernate.getClass(this).equals(Hibernate.getClass(o))) {
			return false;
		}
		User other = (User) o;
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
		return "User{id=%d, email='%s'}".formatted(id, email);
	}
}
