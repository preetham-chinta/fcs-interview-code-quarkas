package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.logging.Logged;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

/**
 * Use case: archive an active warehouse by setting its archivedAt timestamp.
 *
 * Archived warehouses remain in the database for history tracking
 * but are excluded from active warehouse counts and operations.
 */
@ApplicationScoped
@Logged
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator validator;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore, WarehouseValidator validator) {
    this.warehouseStore = warehouseStore;
    this.validator = validator;
  }

  @Override
  @Transactional
  public void archive(Warehouse warehouse) {
    // Validate the warehouse exists and is active
    Warehouse existing = validator.requireActiveWarehouse(warehouse.businessUnitCode);

    // Set the archived timestamp and persist
    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);
  }
}
