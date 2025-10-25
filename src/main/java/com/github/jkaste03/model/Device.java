package com.github.jkaste03.model;

import java.time.Instant;

import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "devices", indexes = {
		@Index(name = "idx_device_user_id", columnList = "user_id") // raskere søk per bruker
})
public class Device {

	public enum Status {
		ONLINE,
		OFFLINE
	}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "device_seq")
	@SequenceGenerator(name = "device_seq", sequenceName = "device_seq", allocationSize = 50)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_device_user"))
	@NotNull
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Status status = Status.OFFLINE;

	@Column(name = "last_seen")
	private Instant lastSeen;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "mac_address", length = 17)
	private String macAddress;

	@Column(name = "public_ip", length = 45)
	private String publicIp;

	protected Device() {
		/* for JPA */ }

	public Device(String name, User user) {
		this.name = name;
		this.user = user;
	}

	public Device(String name, User user, Status status, Instant lastSeen, String macAddress, String publicIp) {
		this(name, user);
		this.status = status;
		this.lastSeen = lastSeen;
		this.macAddress = macAddress;
		this.publicIp = publicIp;
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

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Instant getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(Instant lastSeen) {
		this.lastSeen = lastSeen;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public void markSeen(Instant seenAt) {
		this.lastSeen = seenAt == null ? Instant.now() : seenAt;
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
		Device other = (Device) o;
		if (this.id == null || other.id == null) {
			return false;
		}
		return this.id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return (id != null) ? id.hashCode() : 31;
	}
}
