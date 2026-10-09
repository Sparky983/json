package me.sparky983.json;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.jspecify.annotations.Nullable;

final class InternalUnmodifiableList implements List<@Nullable Json> {
  private final List<@Nullable Json> delegate;

  InternalUnmodifiableList(final List<@Nullable Json> delegate) {
    this.delegate = delegate;
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
  public boolean contains(final Object o) {
    return delegate.contains(o);
  }

  @Override
  public Iterator<@Nullable Json> iterator() {
    final Iterator<@Nullable Json> delegate = this.delegate.iterator();

    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return delegate.hasNext();
      }

      @Override
      public @Nullable Json next() {
        return delegate.next();
      }
    };
  }

  @Override
  public Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public <T> T[] toArray(final T[] a) {
    return delegate.toArray(a);
  }

  @Override
  public boolean add(final @Nullable Json json) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(final Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(final Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override
  public boolean addAll(final Collection<? extends @Nullable Json> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(final int index, final Collection<? extends @Nullable Json> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(final Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(final Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable Json get(final int index) {
    return delegate.get(index);
  }

  @Override
  public @Nullable Json set(final int index, final @Nullable Json element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final int index, final @Nullable Json element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable Json remove(final int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(final Object o) {
    return delegate.indexOf(o);
  }

  @Override
  public int lastIndexOf(final Object o) {
    return delegate.lastIndexOf(o);
  }

  @Override
  public ListIterator<@Nullable Json> listIterator() {
    return listIterator(0);
  }

  @Override
  public ListIterator<@Nullable Json> listIterator(final int index) {
    final ListIterator<@Nullable Json> iterator = this.delegate.listIterator(index);

    return new ListIterator<>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public @Nullable Json next() {
        return iterator.next();
      }

      @Override
      public boolean hasPrevious() {
        return iterator.hasPrevious();
      }

      @Override
      public @Nullable Json previous() {
        return iterator.previous();
      }

      @Override
      public int nextIndex() {
        return iterator.nextIndex();
      }

      @Override
      public int previousIndex() {
        return iterator.previousIndex();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void set(final @Nullable Json json) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void add(final @Nullable Json json) {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public List<@Nullable Json> subList(final int fromIndex, final int toIndex) {
    return new InternalUnmodifiableList(delegate.subList(fromIndex, toIndex));
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
