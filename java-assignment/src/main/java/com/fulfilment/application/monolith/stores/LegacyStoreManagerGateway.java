package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.logging.Logger;

/**
 * Syncs store data to the legacy system (simulated via file I/O).
 *
 * Treated as a transactional resource — uses TxType.MANDATORY to enforce
 * that these methods are ALWAYS called within an existing transaction.
 * If the caller has no active transaction, a TransactionRequiredException is thrown.
 *
 * This means: if StoreService.create() persists a store and then this
 * gateway call fails, the entire transaction rolls back — the DB persist
 * is undone too. Both resources stay consistent.
 *
 * Spring equivalent: @Transactional(propagation = Propagation.MANDATORY)
 */
@ApplicationScoped
public class LegacyStoreManagerGateway {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreManagerGateway.class);

  /**
   * Syncs a newly created store to the legacy system.
   * Participates in the caller's transaction (MANDATORY).
   */
  @Transactional(TxType.MANDATORY)
  public void createStoreOnLegacySystem(Store store) {
    LOGGER.infof("Syncing CREATED store '%s' to legacy system", store.name);
    writeToFile(store);
  }

  /**
   * Syncs an updated store to the legacy system.
   * Participates in the caller's transaction (MANDATORY).
   */
  @Transactional(TxType.MANDATORY)
  public void updateStoreOnLegacySystem(Store store) {
    LOGGER.infof("Syncing UPDATED store '%s' to legacy system", store.name);
    writeToFile(store);
  }

  // ──────────────────────────────────────────────────────────────────────
  // Alternative: CDI event-based approach (kept for reference)
  //
  // Instead of direct calls within the transaction, you can use CDI events
  // that fire AFTER the transaction commits. This decouples the gateway
  // from the service but means the legacy sync happens outside the tx:
  //
  //   public void onStoreEvent(
  //       @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreEvent event) {
  //     Store store = event.getStore();
  //     switch (event.getType()) {
  //       case CREATED -> writeToFile(store);
  //       case UPDATED -> writeToFile(store);
  //     }
  //   }
  //
  // Trade-off:
  //   Direct call (MANDATORY)  → both fail or both succeed (consistency)
  //   CDI event (AFTER_SUCCESS) → legacy never sees uncommitted data,
  //                                but if sync fails the DB change is already committed
  // ──────────────────────────────────────────────────────────────────────

  /** Simulates writing to a legacy system (file I/O as placeholder). */
  private void writeToFile(Store store) {
    try {
      Path tempFile = Files.createTempFile(store.name, ".txt");

      String content =
          "Store synced to legacy. [ name ="
              + store.name
              + " ] [ items on stock ="
              + store.quantityProductsInStock
              + "]";
      Files.write(tempFile, content.getBytes());
      LOGGER.infof("Legacy sync written to: %s", tempFile);

      Files.delete(tempFile);
    } catch (Exception e) {
      LOGGER.error("Failed to sync store to legacy system", e);
    }
  }
}
