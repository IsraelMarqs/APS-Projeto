package com.example.demo;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "loan_requests")
public class LoanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private Instant createdAt = Instant.now();

    public enum Status {
        PENDING, ACCEPTED, DECLINED
    }
    private boolean seen = false;
    public LoanRequest() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }
}

