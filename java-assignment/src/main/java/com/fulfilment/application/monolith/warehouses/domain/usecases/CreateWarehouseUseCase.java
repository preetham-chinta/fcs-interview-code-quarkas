package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.logging.Logged;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

/**
 * Use case: create a new warehouse with full business validation.
 *
 * Validations (from CODE_ASSIGNMENT.md):
 *   1. Business unit code must not already exist
 *   2. Location must be valid (exists in LocationResolver)
 *   3. Max number of warehouses at the location not exceeded
 *   4. Capacity must not exceed the location's maxCapacity and must handle the stock
 */
@ApplicationScoped
@Logged
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator validator;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, WarehouseValidator validator) {
    this.warehouseStore = warehouseStore;
    this.validator = validator;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {

    // 1. Business unit code must not already exist
    validator.requireUniqueBusinessUnitCode(warehouse.businessUnitCode);

    // 2. Location must be valid
    Location location = validator.requireValidLocation(warehouse.location);

    // 3. Max number of warehouses at this location not exceeded
    validator.requireAvailableSlot(warehouse.location, location);

    // 4. Capacity must not exceed location's maxCapacity and must handle the stock
    validator.validateCapacityAndStock(
        warehouse.capacity, warehouse.stock, location, warehouse.location);

    // All validations passed — set creation timestamp and persist
    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
  }
}
