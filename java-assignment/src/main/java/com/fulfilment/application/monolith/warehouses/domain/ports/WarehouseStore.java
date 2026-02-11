package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

public interface WarehouseStore {

  List<Warehouse> getAll();

  void create(Warehouse warehouse);

  void update(Warehouse warehouse);

  /**
   * Can be removed if not needed.
   * 
   * Hard-delete a warehouse. Currently unused — warehouses are soft-deleted 
   * via archive (update with archivedAt). Kept for future use if a permanent
   * removal requirement arises; 
   * 
   */
  void remove(Warehouse warehouse);

  Warehouse findByBusinessUnitCode(String buCode);
}
