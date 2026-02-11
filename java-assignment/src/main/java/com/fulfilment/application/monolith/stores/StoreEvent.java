package com.fulfilment.application.monolith.stores;

/** CDI event payload for store lifecycle events. */
public class StoreEvent {

  public enum Type {
    CREATED,
    UPDATED
  }

  private final Store store;
  private final Type type;

  public StoreEvent(Store store, Type type) {
    this.store = store;
    this.type = type;
  }

  public Store getStore() {
    return store;
  }

  public Type getType() {
    return type;
  }
}
