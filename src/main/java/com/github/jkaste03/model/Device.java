package com.github.jkaste03.model;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "devices")
public class Device {

	public enum Status {
		ONLINE,
		OFFLINE
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	@JsonBackReference(value = "user-devices")
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
		if (this == o) {
			return true;
		}
		if (!(o instanceof Device device)) {
			return false;
		}
		return id != null && Objects.equals(id, device.id);
	}

	@Override
	public int hashCode() {
		return id == null ? 0 : id.hashCode();
	}
}
