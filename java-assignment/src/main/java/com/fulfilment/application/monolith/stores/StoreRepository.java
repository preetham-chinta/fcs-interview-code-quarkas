package com.fulfilment.application.monolith.stores;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/** Panache repository for Store entities. */
@ApplicationScoped
public class StoreRepository implements PanacheRepository<Store> {}
