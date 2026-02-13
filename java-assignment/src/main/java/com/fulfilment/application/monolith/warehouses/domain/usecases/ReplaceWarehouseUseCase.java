package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.logging.Logged;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

/**
 * Use case: replace an active warehouse with a new one under the same business unit code.
 *
 * From BRIEFING.md: "when a Warehouse is replaced we basically archive the current
 * Warehouse using the Business Unit Code provided and create the Warehouse using
 * this same Business Unit Code."
 *
 * Additional validations (from CODE_ASSIGNMENT.md):
 *   1. New warehouse capacity must accommodate the old warehouse's stock
 *   2. New warehouse stock must match the old warehouse's stock
 *   Plus all standard creation validations (location, capacity vs maxCapacity)
 */
@ApplicationScoped
@Logged
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator validator;
  private final ArchiveWarehouseOperation archiveOperation;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      WarehouseValidator validator,
      ArchiveWarehouseOperation archiveOperation) {
    this.warehouseStore = warehouseStore;
    this.validator = validator;
    this.archiveOperation = archiveOperation;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {

    // Existing warehouse must exist and be active
    Warehouse existingWarehouse =
        validator.requireActiveWarehouse(newWarehouse.businessUnitCode);

    // Validate location of the new warehouse
    Location location = validator.requireValidLocation(newWarehouse.location);

    // Replace-specific validations (checked first — more specific error messages):
    // 1. New capacity must accommodate the existing warehouse's stock (Capacity Accommodation)
    validator.validateCapacityAccommodation(newWarehouse.capacity, existingWarehouse.stock);

    // 2. New warehouse stock must match the existing warehouse's stock (Stock Matching)
    validator.validateStockMatching(newWarehouse.stock, existingWarehouse.stock);

    // General: capacity must not exceed location's maxCapacity
    validator.validateCapacityAndStock(
        newWarehouse.capacity, newWarehouse.stock, location, newWarehouse.location);

    // Archive the existing warehouse, then create the new one with the same BU code
    archiveOperation.archive(existingWarehouse);

    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
  }
}
