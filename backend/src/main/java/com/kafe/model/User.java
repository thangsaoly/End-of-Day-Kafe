package com.kafe.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email")
    private String email;

    @Column(name = "email_confirmed", nullable = false)
    private boolean emailConfirmed = false;

    @Column(name = "no_show_count", nullable = false)
    private int noShowCount = 0;

    @Column(name = "is_banned", nullable = false)
    private boolean isBanned = false;

    @JsonIgnore
    @Column(name = "email_verification_code")
    private String emailVerificationCode;

    @Column(name = "role", nullable = false)
    private String role = "CUSTOMER";

    // The cafe a MERCHANT account manages; null for customers and unassigned merchants
    @Column(name = "cafe_id")
    private Long cafeId;

    public User() {}

    public User(String name, String phoneNumber, String passwordHash) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.role = "CUSTOMER";
    }

    public User(String name, String phoneNumber, String passwordHash, String role) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : "CUSTOMER";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailConfirmed() {
        return emailConfirmed;
    }

    public void setEmailConfirmed(boolean emailConfirmed) {
        this.emailConfirmed = emailConfirmed;
    }

    public int getNoShowCount() {
        return noShowCount;
    }

    public void setNoShowCount(int noShowCount) {
        this.noShowCount = noShowCount;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        this.isBanned = banned;
    }

    public String getEmailVerificationCode() {
        return emailVerificationCode;
    }

    public void setEmailVerificationCode(String emailVerificationCode) {
        this.emailVerificationCode = emailVerificationCode;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getCafeId() {
        return cafeId;
    }

    public void setCafeId(Long cafeId) {
        this.cafeId = cafeId;
    }
}
