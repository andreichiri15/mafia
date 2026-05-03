package com.andreichiri.mafia_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"requesterId", "addresseeId"}))
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesterId", nullable = false)
    private MafiaUser requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addresseeId", nullable = false)
    private MafiaUser addressee;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime acceptedAt;

    public enum Status {
        PENDING,
        ACCEPTED
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MafiaUser getRequester() { return requester; }
    public void setRequester(MafiaUser requester) { this.requester = requester; }
    public MafiaUser getAddressee() { return addressee; }
    public void setAddressee(MafiaUser addressee) { this.addressee = addressee; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
}
