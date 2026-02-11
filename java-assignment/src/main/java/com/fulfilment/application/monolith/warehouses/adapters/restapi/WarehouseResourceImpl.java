package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.exceptions.NotFoundException;
import com.fulfilment.application.monolith.logging.Logged;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * REST adapter implementing the generated WarehouseResource interface.
 *
 * Thin HTTP layer — handles only DTO mapping and delegation.
 * No @Transactional here; transaction boundaries are owned by the use cases.
 * No direct repository access; all reads/writes go through domain ports.
 */
@RequestScoped
@Logged
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseStore warehouseStore;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseStore.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var domainWarehouse = toDomainModel(data);
    createWarehouseOperation.create(domainWarehouse);
    return toWarehouseResponse(domainWarehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    var warehouse = warehouseStore.findByBusinessUnitCode(id);
    if (warehouse == null) {
      throw new NotFoundException(
          "Warehouse with business unit code '" + id + "' does not exist.");
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = id;
    archiveWarehouseOperation.archive(warehouse);
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    var domainWarehouse = toDomainModel(data);
    domainWarehouse.businessUnitCode = businessUnitCode;
    replaceWarehouseOperation.replace(domainWarehouse);
    return toWarehouseResponse(domainWarehouse);
  }

  /** Maps the generated API bean → domain model. */
  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainModel(
      Warehouse data) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }

  /** Maps the domain model → generated API bean. */
  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }
}
