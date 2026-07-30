package com.tradingapp.alert;

import com.tradingapp.user.User;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "target_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertDirection direction;

    @Column(nullable = false)
    private boolean triggered = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    protected Alert() {
    }

    public Alert(User user, String ticker, BigDecimal targetPrice, AlertDirection direction) {
        this.user = user;
        this.ticker = ticker;
        this.targetPrice = targetPrice;
        this.direction = direction;
    }

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Flags the alert as fired; notification delivery is out of scope. */
    public void markTriggered(Instant when) {
        this.triggered = true;
        this.triggeredAt = when;
    }

    /** True when {@code price} has crossed the target in this alert's direction. */
    public boolean isCrossedBy(BigDecimal price) {
        if (price == null) {
            return false;
        }
        return direction == AlertDirection.ABOVE
                ? price.compareTo(targetPrice) >= 0
                : price.compareTo(targetPrice) <= 0;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTicker() {
        return ticker;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public AlertDirection getDirection() {
        return direction;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }
}
