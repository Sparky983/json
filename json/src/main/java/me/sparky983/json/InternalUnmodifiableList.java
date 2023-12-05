package me.sparky983.json;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

final class InternalUnmodifiableList implements List<Json> {
  private final List<Json> delegate;

  InternalUnmodifiableList(final List<Json> delegate) {
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
  public Iterator<Json> iterator() {
    final Iterator<Json> delegate = this.delegate.iterator();

    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return delegate.hasNext();
      }

      @Override
      public Json next() {
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
  public boolean add(final Json json) {
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
  public boolean addAll(final Collection<? extends Json> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(final int index, final Collection<? extends Json> c) {
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
  public Json get(final int index) {
    return delegate.get(index);
  }

  @Override
  public Json set(final int index, final Json element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final int index, final Json element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Json remove(final int index) {
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
  public ListIterator<Json> listIterator() {
    return listIterator(0);
  }

  @Override
  public ListIterator<Json> listIterator(final int index) {
    final ListIterator<Json> iterator = this.delegate.listIterator(index);

    return new ListIterator<>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Json next() {
        return iterator.next();
      }

      @Override
      public boolean hasPrevious() {
        return iterator.hasPrevious();
      }

      @Override
      public Json previous() {
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
      public void set(final Json json) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void add(final Json json) {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public List<Json> subList(final int fromIndex, final int toIndex) {
    return new InternalUnmodifiableList(delegate.subList(fromIndex, toIndex));
  }
}
