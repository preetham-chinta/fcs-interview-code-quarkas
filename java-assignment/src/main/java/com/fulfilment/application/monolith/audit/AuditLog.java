package com.fulfilment.application.monolith.audit;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;

/** Audit trail entry — records every attempted mutation across all audited resources. */
@Entity
public class AuditLog extends PanacheEntity {

  public String resourceName;
  public String action;
  public String performedBy;
  public String outcome;
  public LocalDateTime timestamp;

  public AuditLog() {}

  public AuditLog(String resourceName, String action, String outcome) {
    this.resourceName = resourceName;
    this.action = action;
    this.outcome = outcome;
    this.timestamp = LocalDateTime.now();
    this.performedBy = "system";
  }
}
