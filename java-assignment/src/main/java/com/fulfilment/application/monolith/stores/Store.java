package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.common.Updatable;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Cacheable
public class Store implements Updatable<Store> {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "store_seq")
  @SequenceGenerator(name = "store_seq", sequenceName = "store_seq", allocationSize = 50)
  public Long id;

  @NotBlank(message = "Store name is required")
  @Size(max = 40, message = "Store name must be at most 40 characters")
  @Column(length = 40, unique = true)
  public String name;

  @PositiveOrZero(message = "Stock quantity cannot be negative")
  public int quantityProductsInStock;

  public Store() {}

  public Store(String name) {
    this.name = name;
  }

  @Override
  public void updateFrom(Store source) {
    this.name = source.name;
    this.quantityProductsInStock = source.quantityProductsInStock;
  }

  @Override
  public void patchFrom(Store source) {
    if (source.name != null) {
      this.name = source.name;
    }
    if (source.quantityProductsInStock != 0) {
      this.quantityProductsInStock = source.quantityProductsInStock;
    }
  }
}
