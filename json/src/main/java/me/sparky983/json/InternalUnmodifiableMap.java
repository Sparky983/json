package me.sparky983.json;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A marker for an unmodifiable view of a map that is never modified after creation. Used to avoid
 * copying in the {@link Json.Object} constructor.
 */
final class InternalUnmodifiableMap implements Map<String, @Nullable Json> {
  private final Map<String, @Nullable Json> delegate;

  InternalUnmodifiableMap(final Map<String, @Nullable Json> delegate) {
    this.delegate = Collections.unmodifiableMap(delegate);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean containsKey(final Object key) {
    return delegate.containsKey(key);
  }

  @Override
  public boolean containsValue(final Object value) {
    return delegate.containsValue(value);
  }

  @Override
  public @Nullable Json get(final Object key) {
    return delegate.get(key);
  }

  @Override
  public @Nullable Json put(final String key, final @Nullable Json value) {
    return delegate.put(key, value);
  }

  @Override
  public @Nullable Json remove(final @Nullable Object key) {
    return delegate.remove(key);
  }

  @Override
  public void putAll(final Map<? extends String, ? extends @Nullable Json> m) {
    delegate.putAll(m);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public Set<String> keySet() {
    return delegate.keySet();
  }

  @Override
  public Collection<@Nullable Json> values() {
    return delegate.values();
  }

  @Override
  public Set<Entry<String, @Nullable Json>> entrySet() {
    return delegate.entrySet();
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
