package com.fkmed.domain.reimbursement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

/**
 * One Terapia/Psicologia session (SPEC-0015 BR7): date + amount, a child of {@link
 * ReimbursementRequest}.
 */
@Entity
@Table(name = "reimbursement_session_item")
@Getter
public class SessionItem {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id", nullable = false)
  private ReimbursementRequest request;

  @Column(name = "session_date", nullable = false)
  private LocalDate sessionDate;

  @Column(nullable = false)
  private BigDecimal amount;

  /** JPA only. */
  protected SessionItem() {}

  private SessionItem(ReimbursementRequest request, LocalDate sessionDate, BigDecimal amount) {
    this.id = UUID.randomUUID();
    this.request = request;
    this.sessionDate = sessionDate;
    this.amount = amount;
  }

  /** Creates a session item bound to {@code request}. */
  static SessionItem of(ReimbursementRequest request, LocalDate sessionDate, BigDecimal amount) {
    return new SessionItem(request, sessionDate, amount);
  }
}
