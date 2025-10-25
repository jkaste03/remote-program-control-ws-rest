package com.github.jkaste03.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "device_seq")
	@SequenceGenerator(name = "device_seq", sequenceName = "device_seq", allocationSize = 50)
	private Long id;

	@Column(nullable = false, length = 30, unique = true)
	private String name;

	public Role() {
	}

	public Role(String name) {
		this.name = name;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	private boolean hasSameName(Role other) {
		return other != null && Objects.equals(name, other.name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Role role)) {
			return false;
		}
		return hasSameName(role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "Role [id=" + id + ", name=" + name + "]";
	}
}
