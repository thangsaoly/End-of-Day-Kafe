package com.kafe.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "cafe_id"}))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    private Cafe cafe;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Favorite() {}

    public Favorite(User user, Cafe cafe) {
        this.user = user;
        this.cafe = cafe;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Cafe getCafe() {
        return cafe;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
