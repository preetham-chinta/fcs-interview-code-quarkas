package com.fulfilment.application.monolith.products;

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
import java.math.BigDecimal;

@Entity
@Cacheable
public class Product implements Updatable<Product> {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
  @SequenceGenerator(name = "product_seq", sequenceName = "product_seq", allocationSize = 50)
  public Long id;

  @NotBlank(message = "Product name is required")
  @Size(max = 40, message = "Product name must be at most 40 characters")
  @Column(length = 40, unique = true)
  public String name;

  @Column(nullable = true)
  public String description;

  @PositiveOrZero(message = "Price cannot be negative")
  @Column(precision = 10, scale = 2, nullable = true)
  public BigDecimal price;

  @PositiveOrZero(message = "Stock cannot be negative")
  public int stock;

  public Product() {}

  public Product(String name) {
    this.name = name;
  }

  @Override
  public void updateFrom(Product source) {
    this.name = source.name;
    this.description = source.description;
    this.price = source.price;
    this.stock = source.stock;
  }

  @Override
  public void patchFrom(Product source) {
    if (source.name != null) {
      this.name = source.name;
    }
    if (source.description != null) {
      this.description = source.description;
    }
    if (source.price != null) {
      this.price = source.price;
    }
    if (source.stock != 0) {
      this.stock = source.stock;
    }
  }
}
