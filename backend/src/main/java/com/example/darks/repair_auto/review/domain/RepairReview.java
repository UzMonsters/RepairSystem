package com.example.darks.repair_auto.review.domain;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "repair_reviews")
public class RepairReview {

    public static final int MAX_COMMENT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repair_request_id", nullable = false, unique = true)
    private RepairRequest repairRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Column(nullable = false)
    private int rating;

    @Column(length = MAX_COMMENT_LENGTH)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "submitted_language", nullable = false, length = 8)
    private LanguageCode submittedLanguage;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @JsonIgnore
    @Version
    @Column(nullable = false)
    private long version;

    protected RepairReview() {
    }

    public RepairReview(
            RepairRequest repairRequest,
            Customer customer,
            Technician technician,
            int rating,
            String comment,
            ReviewSource source,
            LanguageCode submittedLanguage,
            OffsetDateTime now) {
        this.repairRequest = repairRequest;
        this.customer = customer;
        this.technician = technician;
        this.rating = rating;
        this.comment = comment;
        this.source = source;
        this.submittedLanguage = submittedLanguage;
        this.submittedAt = now;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public RepairRequest getRepairRequest() {
        return repairRequest;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Technician getTechnician() {
        return technician;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public ReviewSource getSource() {
        return source;
    }

    public LanguageCode getSubmittedLanguage() {
        return submittedLanguage;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
