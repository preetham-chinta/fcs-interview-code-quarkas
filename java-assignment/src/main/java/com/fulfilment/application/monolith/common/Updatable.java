package com.fulfilment.application.monolith.common;

/**
 * Generic interface for entities that support field mapping from a source object.
 *
 * Keeps mapping logic co-located with the entity (it knows its own fields),
 * so the service/resource layer just calls entity.updateFrom(source) instead
 * of repeating field-by-field assignments everywhere.
 *
 * Two operations:
 *   updateFrom — full replace (PUT semantics: all fields overwritten)
 *   patchFrom  — partial replace (PATCH semantics: only non-null/non-default fields)
 *
 * @param <T> the entity type (typically the implementing class itself)
 */
public interface Updatable<T> {

  /** Full update — overwrite all fields from the source. */
  void updateFrom(T source);

  /** Partial update — only overwrite fields that are explicitly set in the source. */
  void patchFrom(T source);
}
