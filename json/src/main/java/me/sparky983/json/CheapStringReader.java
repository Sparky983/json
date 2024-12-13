package me.sparky983.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
* A more performant {@link StringReader} implementation.
 *
 * <p>The performance improvements come from guarantees made by the {@link Json#read(String)}:
 * <ul>
 *   <li>No need for {@link Reader#close()} implementation - the reader is never the only reference to the input</li>
 *   <li>No open state checks - the reader cannot be closed</li>
 *   <li>No need to {@code synchronize} - the reader is only accessed from a single thread at a time</li>
 * </ul>
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
