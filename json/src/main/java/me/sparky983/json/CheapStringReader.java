package me.sparky983.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
* A more performant, partial implementation of {@link StringReader} that doesn't do synchronisation.
*/
final class CheapStringReader extends Reader {
  private final int length;
  private final String input;
  private int position = 0;

  CheapStringReader(final String input) {
    this.length = input.length();
    this.input = input;
  }

  @Override
  public int read() {
    if (position >= length)
      return -1;
    return input.charAt(position++);
  }

  @Override
  public int read(final char[] charBuffer, int off, int len) throws IOException {
    // This operation does not need to be implemented as it is unused
    throw new UnsupportedOperationException("Operation not implemented");
  }

  @Override
  public void close() {}
}
