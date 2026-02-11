package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse")
@Cacheable
public class DbWarehouse {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "warehouse_seq")
  @SequenceGenerator(name = "warehouse_seq", sequenceName = "warehouse_seq", allocationSize = 50)
  public Long id;

  public String businessUnitCode;
  public String location;
  public Integer capacity;
  public Integer stock;
  public LocalDateTime createdAt;
  public LocalDateTime archivedAt;

  public DbWarehouse() {}

  public Warehouse toWarehouse() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = this.businessUnitCode;
    warehouse.location = this.location;
    warehouse.capacity = this.capacity;
    warehouse.stock = this.stock;
    warehouse.createdAt = this.createdAt;
    warehouse.archivedAt = this.archivedAt;
    return warehouse;
  }

  // Add mapper for conversion
  public static DbWarehouse fromWarehouse(Warehouse warehouse) {
    var db = new DbWarehouse();
    db.businessUnitCode = warehouse.businessUnitCode;
    db.location = warehouse.location;
    db.capacity = warehouse.capacity;
    db.stock = warehouse.stock;
    db.createdAt = warehouse.createdAt;
    db.archivedAt = warehouse.archivedAt;
    return db;
  }
}
