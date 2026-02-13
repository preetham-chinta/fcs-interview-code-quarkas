package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.ConflictException;
import com.fulfilment.application.monolith.exceptions.NotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Centralised validation rules for warehouse operations.
 *
 * Each method encapsulates a single business rule so use cases
 * can compose only the checks they need without duplication.
 */
@ApplicationScoped
public class WarehouseValidator {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public WarehouseValidator(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  /**
   * Looks up a warehouse by BU code and ensures it exists and is not archived.
   *
   * @return the active warehouse
   * @throws NotFoundException if no warehouse with that BU code exists
   * @throws ConflictException if the warehouse is already archived
   */
  public Warehouse requireActiveWarehouse(String businessUnitCode) {
    Warehouse existing = warehouseStore.findByBusinessUnitCode(businessUnitCode);
    if (existing == null) {
      throw new NotFoundException(
          "Warehouse with business unit code '" + businessUnitCode + "' does not exist.");
    }
    if (existing.archivedAt != null) {
      throw new ConflictException(
          "Warehouse with business unit code '" + businessUnitCode + "' is already archived.");
    }
    return existing;
  }

  /**
   * Ensures no active warehouse already uses this BU code.
   *
   * @throws ConflictException if the code is already taken
   */
  public void requireUniqueBusinessUnitCode(String businessUnitCode) {
    Warehouse existing = warehouseStore.findByBusinessUnitCode(businessUnitCode);
    if (existing != null) {
      throw new ConflictException(
          "Warehouse with business unit code '" + businessUnitCode + "' already exists.");
    }
  }

  /**
   * Resolves a location identifier and validates it exists.
   *
   * @return the resolved location
   * @throws ValidationException if the location does not exist
   */
  public Location requireValidLocation(String locationIdentifier) {
    Location location = locationResolver.resolveByIdentifier(locationIdentifier);
    if (location == null) {
      throw new ValidationException(
          "Location '" + locationIdentifier + "' is not a valid location.");
    }
    return location;
  }

  /**
   * Ensures the location has not reached its maximum number of active warehouses.
   *
   * @throws ConflictException if the limit is reached
   */
  public void requireAvailableSlot(String locationIdentifier, Location location) {
    long activeCount =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(locationIdentifier))
            .filter(w -> w.archivedAt == null)
            .count();

    if (activeCount >= location.maxNumberOfWarehouses) {
      throw new ConflictException(
          "Maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ") already reached for location '"
              + locationIdentifier
              + "'.");
    }
  }

  /**
   * Validates that the warehouse capacity does not exceed the location's max
   * and that the capacity can hold the declared stock.
   *
   * @throws ValidationException if either check fails
   */
  public void validateCapacityAndStock(
      int capacity, Integer stock, Location location, String locationIdentifier) {

    if (capacity > location.maxCapacity) {
      throw new ValidationException(
          "Warehouse capacity ("
              + capacity
              + ") exceeds maximum capacity ("
              + location.maxCapacity
              + ") for location '"
              + locationIdentifier
              + "'.");
    }

    if (stock != null && capacity < stock) {
      throw new ValidationException(
          "Warehouse capacity (" + capacity + ") cannot handle the stock (" + stock + ").");
    }
  }
  
  /**
   * Validates warehouse replacement constraints (Capacity Accommodation).
   *
   * Ensures the new warehouse's capacity can accommodate the stock from the warehouse
   * being replaced.
   *
   * @param newCapacity the capacity of the new warehouse
   * @param existingStock the stock of the warehouse being replaced
   * @throws ValidationException if the new capacity cannot accommodate the existing stock
   */
  public void validateCapacityAccommodation(int newCapacity, int existingStock) {
    if (newCapacity < existingStock) {
      throw new ValidationException(
          "New warehouse capacity ("
              + newCapacity
              + ") cannot accommodate the existing warehouse's stock ("
              + existingStock
              + ").");
    }
  }

  /**
   * Validates warehouse replacement constraints (Stock Matching).
   *
   * Confirms that the stock of the new warehouse matches the stock of the previous
   * warehouse.
   *
   * @param newStock the stock of the new warehouse
   * @param existingStock the stock of the warehouse being replaced
   * @throws ValidationException if the stocks do not match
   */
  public void validateStockMatching(int newStock, int existingStock) {
    if (newStock != existingStock) {
      throw new ValidationException(
          "New warehouse stock ("
              + newStock
              + ") must match the existing warehouse's stock ("
              + existingStock
              + ").");
    }
  }


}
