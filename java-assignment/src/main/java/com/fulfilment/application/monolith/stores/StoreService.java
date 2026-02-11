package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.exceptions.NotFoundException;
import com.fulfilment.application.monolith.logging.Logged;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Service layer for Store business logic.
 *
 * Owns transaction boundaries and legacy system sync. The resource layer
 * delegates here and stays thin (HTTP concerns only).
 *
 * The gateway calls happen WITHIN the @Transactional boundary:
 *   1. persist store to DB
 *   2. sync to legacy system (MANDATORY — joins this transaction)
 *   → If step 2 fails, step 1 rolls back. Both stay consistent.
 *
 */
@ApplicationScoped
@Logged
public class StoreService {

  @Inject StoreRepository storeRepository;
  @Inject LegacyStoreManagerGateway legacyGateway;

  // ── CDI event-based alternative (kept for reference) ──────────────
  // @Inject Event<StoreEvent> storeEvent;
  //
  // Instead of calling legacyGateway directly, you can fire CDI events:
  //   storeEvent.fire(new StoreEvent(store, StoreEvent.Type.CREATED));
  //
  // The LegacyStoreManagerGateway then observes these events with
  // @Observes(during = TransactionPhase.AFTER_SUCCESS) — meaning the
  // sync only fires after the DB commit succeeds.
  //
  // Trade-off vs direct calls:
  //   Direct (current)  → tight coupling, but atomic consistency
  //   CDI events         → loose coupling, but no rollback if sync fails
  // ──────────────────────────────────────────────────────────────────

  public List<Store> listAll() {
    return storeRepository.listAll(Sort.by("name"));
  }

  public Store findById(Long id) {
    Store entity = storeRepository.findById(id);
    if (entity == null) {
      throw new NotFoundException("Store", id);
    }
    return entity;
  }

  @Transactional
  public Store create(Store store) {
    storeRepository.persist(store);
    legacyGateway.createStoreOnLegacySystem(store);
    return store;
  }

  @Transactional
  public Store update(Long id, Store updatedStore) {
    Store entity = findById(id);
    entity.updateFrom(updatedStore);
    legacyGateway.updateStoreOnLegacySystem(entity);
    return entity;
  }

  @Transactional
  public Store patch(Long id, Store updatedStore) {
    Store entity = findById(id);
    entity.patchFrom(updatedStore);
    legacyGateway.updateStoreOnLegacySystem(entity);
    return entity;
  }

  @Transactional
  public void delete(Long id) {
    Store entity = findById(id);
    storeRepository.delete(entity);
  }
}
