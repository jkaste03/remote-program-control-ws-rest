/**
 * 
 */
package com.github.jkaste03.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.hateoas.RepresentationModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;

/**
 * @author tdoy
 */
@Entity
@Table(name = "users")
public class User extends RepresentationModel<User> {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long userid;

	@Column(nullable = false)
	private String firstname;

	@Column(nullable = false)
	private String lastname;

	@Column(nullable = false, length = 50, unique = true)
	private String email;

	// @Column(nullable = false, length = 64)
	// @JsonIgnore
	// private String password;

	@ManyToMany
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<Device> devices = new HashSet<>();

	public User() {
		/* default */}

	public User(String email, String firstname, String lastname) {
		this.email = email;
		// this.password = password;
		this.firstname = firstname;
		this.lastname = lastname;
	}

	/**
	 * @return the userid
	 */
	public Long getUserid() {
		return userid;
	}

	/**
	 * @param userid the userid to set
	 */
	public void setUserid(Long userid) {
		this.userid = userid;
	}

	/**
	 * @return the firstname
	 */
	public String getFirstname() {
		return firstname;
	}

	/**
	 * @param firstname the firstname to set
	 */
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	/**
	 * @return the lastname
	 */
	public String getLastname() {
		return lastname;
	}

	/**
	 * @param lastname the lastname to set
	 */
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the roles
	 */
	public Set<Role> getRoles() {
		return roles;
	}

	/**
	 * @param roles the roles to set
	 */
	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	public void addRole(Role role) {
		this.roles.add(role);
	}

	public void removeRole(Role role) {
		this.roles.remove(role);
	}

	public Set<Device> getDevices() {
		return devices;
	}

	public void setDevices(Set<Device> devices) {
		this.devices = devices;
	}

	public void addDevice(Device device) {
		this.devices.add(device);
		device.setUser(this);
	}

	public void removeDevice(Device device) {
		// Removing from collection is enough; orphanRemoval=true will delete the device
		this.devices.remove(device);
	}

	// /**
	// * @param password the password to set
	// */
	// public void setPassword(String password) {
	// this.password = password;
	// }
	//
	// /**
	// * @return the password
	// */
	// public String getPassword() {
	// return password;
	// }

}
