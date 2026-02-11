package com.fulfilment.application.monolith.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.jboss.logging.Logger;

/**
 * Persists audit log entries in an independent transaction (REQUIRES_NEW).
 *
 * Called automatically by {@link AuditInterceptor} — not manually from resources.
 * The independent transaction ensures audit records survive even if the
 * caller's business transaction rolls back.
 */
@ApplicationScoped
public class AuditService {

  private static final Logger LOGGER = Logger.getLogger(AuditService.class);

  @Transactional(TxType.REQUIRES_NEW)
  public void logAction(String resourceIdentifier, String action, String outcome) {
    AuditLog entry = new AuditLog(resourceIdentifier, action, outcome);
    entry.persist();
    LOGGER.infof("Audit: %s on '%s' — %s", action, resourceIdentifier, outcome);
  }
}
