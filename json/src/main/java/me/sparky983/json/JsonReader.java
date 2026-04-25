package me.sparky983.json;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.math.BigInteger;
import java.util.Arrays;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.VisibleForTesting;

final class JsonReader implements Closeable {
  @VisibleForTesting
  static final int BUFFER_SIZE = 1024;
  private static final String UNKNOWN_LITERAL = "Unknown literal";
  private static final String UNEXPECTED_EOF = "Unexpected end of input";

  /** Used internally to represent the state in which no character has been read yet. */
  private static final int NO_LOOKAHEAD = -2; // -1 indicates the end of the sequence

  // Use byte to represent states results in ~5% speed up
  private static final byte TOP_LEVEL = 1;
  private static final byte DONE = 2;
  private static final byte OBJECT_EXPECT_FIELD_OR_END = 3;
  private static final byte OBJECT_EXPECT_VALUE = 4;
  private static final byte OBJECT_EXPECT_COMMA_OR_END = 5;
  private static final byte ARRAY_EXPECT_VALUE = 6;
  private static final byte ARRAY_EXPECT_VALUE_OR_END = 7;
  private static final byte ARRAY_EXPECT_COMMA_OR_END = 8;

  private final Reader reader;
  private final char[] buf = new char[BUFFER_SIZE];
  private int bufPosition = 0;
  private int bufLimit = 0;
  /** the position of the next lookahead */
  private int lookahead = NO_LOOKAHEAD;

  // Manage a stack ourselves results in ~5% speed up
  private byte[] state = new byte[8];
  private int stateSize = 1;

  JsonReader(Reader reader) {
    this.reader = reader;
    state[0] = TOP_LEVEL;
  }

  private void beforeValue() throws IOException, JsonParseException {
    tryInitLookahead();

    // "array": startArray() [ (ARRAY_EXPECT_VALUE_OR_END) readValue()
    //   1 (ARRAY_EXPECT_COMMA_OR_END) readValue() ,
    //   2 (ARRAY_EXPECT_COMMA_OR_END) peek() , (ARRAY_EXPECT_VALUE) readValue()
    //   3 (ARRAY_EXPECT_COMMA_OR_END) endArray() 
    // ] 

    switch (peekState()) {
      case TOP_LEVEL, OBJECT_EXPECT_VALUE, ARRAY_EXPECT_VALUE, ARRAY_EXPECT_VALUE_OR_END -> {}
      case ARRAY_EXPECT_COMMA_OR_END -> {
        skipWhitespace();
        expect(',');
        replaceState(ARRAY_EXPECT_VALUE);
      }
      case OBJECT_EXPECT_COMMA_OR_END ->
          throw new IllegalStateException("Object value read before reading the field name");
      default -> throw new IllegalStateException();
    }
  }

  private void afterValue() {
    switch (popState()) {
      case ARRAY_EXPECT_VALUE_OR_END, ARRAY_EXPECT_VALUE -> pushState(ARRAY_EXPECT_COMMA_OR_END);
      case OBJECT_EXPECT_VALUE -> pushState(OBJECT_EXPECT_COMMA_OR_END);
      case TOP_LEVEL -> {
        assert stateSize == 0;
        pushState(DONE);
      }
      default -> throw new IllegalStateException();
    }
  }

  public String readString() throws IOException, JsonParseException {
    beforeValue();
    final String string = readRawString();
    afterValue();
    return string;
  }

  private String readRawString() throws IOException, JsonParseException {
    skipWhitespace();
    expect('"');

    if (lookahead == -1) {
      throw new JsonParseException("Unterminated string");
    }

    // lazily initialise builder so:
    // Allocation can be avoided entirely if there are no escape sequences and the whole string
    // can be read without reaching the buffer limit.
    // Then, if we either reach an escape sequence of the end of the buffer, we can set the capacity
    // then, rather than upfront so we have a better idea of the size of the buffer
    StringBuilder builder = null;
    int start = bufPosition - 1;

    string: while (true) {
      int end = start;
      while (end < bufLimit) {
        final char c = buf[end];
        if (c < 0x0020) {
          throw new JsonParseException("Illegal character");
        }
        switch (c) {
          case '"' -> {
            if (builder == null) {
              final String string = new String(buf, start, end - start);
              bufPosition = end + 1;
              consume();
              return string;
            }
            builder.append(buf, start, end - start);
            bufPosition = end + 1;
            consume();
            return builder.toString();
          }
          case '\\' -> {
            if (builder == null) {
              builder = new StringBuilder(Math.max((end - start + 1) * 2, 16));
            }
            builder.append(buf, start, end - start);
            bufPosition = end + 1;
            consume();
            builder.append(readEscapedCharacter());
            if (lookahead == -1) {
              throw new JsonParseException("Unterminated string");
            }
            start = bufPosition - 1;
            continue string;
          }
        }
        end++;
      }

      if (builder == null) {
        builder = new StringBuilder(Math.max((end - start) * 2, 16));
      }
      builder.append(buf, start, end - start);
      bufPosition = end;
      consume();
      if (lookahead == -1) {
        throw new JsonParseException("Unterminated string");
      }
      start = bufPosition - 1;
    }
  }

  private char readEscapedCharacter() throws IOException, JsonParseException {
    final int type = lookahead;
    consume();
    return switch (type) {
      case '"', '\\', '/' -> (char) type;
      case 'b' -> '\b';
      case 'f' -> '\f';
      case 'n' -> '\n';
      case 'r' -> '\r';
      case 't' -> '\t';
      case 'u' -> {
        final int c1 = lookahead;
        consume();
        final int c2 = lookahead;
        consume();
        final int c3 = lookahead;
        consume();
        final int c4 = lookahead;
        consume();

        if (c1 == -1 || c2 == -1 || c3 == -1 || c4 == -1) {
          throw new JsonParseException(UNEXPECTED_EOF);
        }

        final int h1 = hexDigit(c1);
        final int h2 = hexDigit(c2);
        final int h3 = hexDigit(c3);
        final int h4 = hexDigit(c4);
        final int codePoint = (h1 << 12)
                              | (h2 << 8)
                              | (h3 << 4)
                              | h4;
        yield (char) codePoint;
      }
      case -1 -> throw new JsonParseException(UNEXPECTED_EOF);
      default ->
          throw new JsonParseException("Illegal character escape character \"" + type + "\"");
    };
  }

  private byte hexDigit(int digit) throws JsonParseException {
    return switch (digit) {
      case '0' -> 0x0;
      case '1' -> 0x1;
      case '2' -> 0x2;
      case '3' -> 0x3;
      case '4' -> 0x4;
      case '5' -> 0x5;
      case '6' -> 0x6;
      case '7' -> 0x7;
      case '8' -> 0x8;
      case '9' -> 0x9;
      case 'A', 'a' -> 0xA;
      case 'B', 'b' -> 0xB;
      case 'C', 'c' -> 0xC;
      case 'D', 'd' -> 0xD;
      case 'E', 'e' -> 0xE;
      case 'F', 'f' -> 0xF;
      default -> throw new JsonParseException("Illegal hex digit \"" + (char) digit + "\"");
    };
  }

  public String readNumber() throws IOException, JsonParseException {
    beforeValue();
    skipWhitespace();
    final StringBuilder builder = new StringBuilder();

    readInteger(builder);
    final boolean isDecimal = readFraction(builder);
    readExponent(builder);

    afterValue();
    return builder.toString();
  }

  public BigInteger readInteger() throws IOException, JsonParseException {
    beforeValue();
    skipWhitespace();
    final StringBuilder builder = new StringBuilder();
    readInteger(builder);
    afterValue();
    return new BigInteger(builder.toString());
  }

  private void readInteger(final StringBuilder builder) throws IOException, JsonParseException {
    if (lookahead == '-') {
      consume();
      builder.append('-');
    }

    final int first = lookahead;

    switch (first) {
      case '0' -> {
        consume();
        builder.append('0');
      }
      case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
        consume();
        builder.append((char) first);
        while (true) {
          final int digit = lookahead;
          switch (digit) {
            case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
              consume();
              builder.append((char) digit);
            }
            default -> {
              return;
            }
          }
        }
      }
      case -1 -> throw new JsonParseException(UNEXPECTED_EOF);
      default -> throw new JsonParseException("Unexpected character \"" + (char) first + "\"");
    }
  }

  private boolean readFraction(final StringBuilder builder) throws IOException, JsonParseException {
    if (lookahead == '.') {
      consume();
      builder.append('.');
    } else {
      return false;
    }

    readDigitsAtLeast1(builder);
    return true;
  }

  private void readExponent(final StringBuilder builder) throws IOException, JsonParseException {
    final int e = lookahead;

    switch (e) {
      case 'e', 'E' -> {
        consume();
        builder.append('E');
      }
      default -> {
        return;
      }
    }

    final int sign = lookahead;
    switch (lookahead) {
      case '-', '+' -> {
        consume();
        builder.append((char) sign);
      }
    }

    readDigitsAtLeast1(builder);
  }

  private void readDigitsAtLeast1(final StringBuilder builder)
      throws IOException, JsonParseException {
    while (true) {
      final int digit = lookahead;
      switch (digit) {
        case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
          consume();
          builder.append((char) digit);
          switch (lookahead) {
            case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {}
            default -> {
              return;
            }
          }
        }
        case -1 -> throw new JsonParseException(UNEXPECTED_EOF);
        default -> throw new JsonParseException("Unexpected character \"" + (char) digit + "\"");
      }
    }
  }

  public boolean readBoolean() throws IOException, JsonParseException {
    beforeValue();
    skipWhitespace();
    final boolean value = switch (lookahead) {
      case 't' -> {
        readTrue();
        yield true;
      }
      case 'f' -> {
        readFalse();
        yield false;
      }
      default -> throw new JsonParseException("Expected a boolean (true/false)");
    };
    afterValue();
    return value;
  }

  private void readTrue() throws IOException, JsonParseException {
    assert lookahead == 't';
    consume();
    final int r = lookahead;
    consume();
    final int u = lookahead;
    consume();
    final int e = lookahead;
    consume();

    if (r != 'r' || u != 'u' || e != 'e') {
      throw new JsonParseException(UNKNOWN_LITERAL);
    }
  }

  private void readFalse() throws IOException, JsonParseException {
    assert lookahead == 'f';
    consume();
    final int a = lookahead;
    consume();
    final int l = lookahead;
    consume();
    final int s = lookahead;
    consume();
    final int e = lookahead;
    consume();

    if (a != 'a' || l != 'l' || s != 's' || e != 'e') {
      throw new JsonParseException(UNKNOWN_LITERAL);
    }
  }

  public void readNull() throws IOException, JsonParseException {
    beforeValue();
    skipWhitespace();
    final int n = lookahead;
    consume();
    final int u = lookahead;
    consume();
    final int l1 = lookahead;
    consume();
    final int l2 = lookahead;
    consume();

    if (n != 'n' || u != 'u' || l1 != 'l' || l2 != 'l') {
      throw new JsonParseException(UNKNOWN_LITERAL);
    }
    afterValue();
  }

  public void startObject() throws JsonParseException, IOException {
    beforeValue();
    skipWhitespace();
    expect('{');
    pushState(OBJECT_EXPECT_FIELD_OR_END);
  }

  public void endObject() throws JsonParseException, IOException {
    final byte state = peekState();
    if (state != OBJECT_EXPECT_COMMA_OR_END && state != OBJECT_EXPECT_FIELD_OR_END) {
      throw new IllegalStateException("Cannot end object");
    }
    popState();
    skipWhitespace();
    expect('}');
    afterValue();
  }

  public void startArray() throws JsonParseException, IOException {
    beforeValue();
    skipWhitespace();
    expect('[');
    pushState(ARRAY_EXPECT_VALUE_OR_END);
  }

  public void endArray() throws JsonParseException, IOException {
    final byte state = peekState();
    if (state != ARRAY_EXPECT_COMMA_OR_END && state != ARRAY_EXPECT_VALUE_OR_END) {
      throw new IllegalStateException("Cannot end array");
    }
    popState();
    skipWhitespace();
    expect(']');
    afterValue();
  }

  /**
   * Peeks to see what the next value is.
   *
   * <p>May need to consume the input
   */
  Token peek() throws JsonParseException, IOException {
    tryInitLookahead();
    final byte state = peekState();
    switch (state) {
      case TOP_LEVEL, ARRAY_EXPECT_VALUE, OBJECT_EXPECT_VALUE, ARRAY_EXPECT_VALUE_OR_END -> {}
      case ARRAY_EXPECT_COMMA_OR_END ->  {
        skipWhitespace();
        if (lookahead == ',') {
          consume();
          replaceState(ARRAY_EXPECT_VALUE);
        }
      }
      default ->
          throw new IllegalStateException("Cannot peek; next token is a field name or object end");
    }
    skipWhitespace();
    return switch (lookahead) {
      case '{' -> Token.OBJECT;
      case '[' -> Token.ARRAY;
      case '"' -> Token.STRING;
      case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> Token.NUMBER;
      case 't', 'f' -> Token.BOOLEAN;
      case 'n' -> Token.NULL;
      case ']' -> throw new IllegalStateException("No value to peek, check for hasNext() before peeking");
      default ->
          throw new JsonParseException("Unexpected character " + (char) lookahead);
    };
  }

  public boolean hasNext() throws IOException {
    switch (peekState()) {
      case OBJECT_EXPECT_FIELD_OR_END, OBJECT_EXPECT_COMMA_OR_END,
           ARRAY_EXPECT_VALUE_OR_END, ARRAY_EXPECT_COMMA_OR_END -> {}
      default -> throw new IllegalStateException("Cannot check for next value unless in array or object");
    }

    skipWhitespace();
    return switch (lookahead) {
      case ']', '}', -1 -> false;
      default -> true;
    };
  }

  public String readField() throws JsonParseException, IOException {
    // "object": startObject() { (OBJECT_EXPECT_FIELD_OR_END) readField()
    //   "foo": (OBJECT_EXPECT_VALUE) readValue() "bar" (OBJECT_EXPECT_COMMA_OR_END) readField() ,
    //   "bar": (OBJECT_EXPECT_VALUE) peek() readValue() "baz" (OBJECT_EXPECT_COMMA_OR_END) readField() ,
    //   "baz": (OBJECT_EXPECT_VALUE) readValue() "foo" (OBJECT_EXPECT_COMMA_OR_END) 
    // }
    switch (peekState()) {
      case OBJECT_EXPECT_FIELD_OR_END -> {}
      case OBJECT_EXPECT_COMMA_OR_END -> {
        skipWhitespace();
        expect(',');
      }
      default -> throw new IllegalStateException("Cannot read field outside of object");
    }
    final String field = readRawString();
    skipWhitespace();
    // We consume the ':' here, however the ',' after array elements are read when the next value is
    // read so the behaviour is inconsistent
    expect(':');
    replaceState(OBJECT_EXPECT_VALUE);
    return field;
  }

  boolean isEof() throws IOException {
    if (peekState() != DONE) {
      throw new IllegalStateException("Is EOF can only be checked in DONE state");
    }
    skipWhitespace();
    return lookahead == -1;
  }

  private void skipWhitespace() throws IOException {
    while (true) {
      switch (lookahead) {
        case ' ', '\n', '\r', '\t' -> consume();
        default -> {
          return;
        }
      }
    }
  }

  private void expect(char c) throws JsonParseException, IOException {
    if (lookahead != c) {
      throw new JsonParseException("Unexpected character \"" + (char) lookahead + "\", expected \"" + c + "\"");
    }
    consume();
  }

  private void consume() throws IOException {
    if (bufPosition >= bufLimit) {
      final int count = reader.read(buf);
      assert count != 0 : "BUFFER_SIZE cannot be 0";
      if (count == -1) {
        lookahead = -1;
        return;
      } else {
        bufPosition = 0;
        bufLimit = count;
      }
    }

    lookahead = buf[bufPosition++];
  }

  private void tryInitLookahead() throws IOException {
    if (lookahead == NO_LOOKAHEAD) {
      consume();
    }
  }

  @SuppressWarnings("MagicConstant")
  @State
  private byte peekState() {
    final byte popped = state[stateSize - 1];
    assert popped != 0 : "TOP_LEVEL must not be 0";
    return popped;
  }

  private void pushState(@State final byte nextState) {
    if (stateSize == state.length) {
      state = Arrays.copyOf(state, stateSize * 2);
    }
    state[stateSize++] = nextState;
  }

  @SuppressWarnings("MagicConstant")
  @State
  private byte popState() {
    final byte popped = state[--stateSize];
    assert popped != 0 : "TOP_LEVEL must not be 0";
    return popped;
  }

  private void replaceState(@State final byte nextState) {
    state[stateSize - 1] = nextState;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  @Target({ElementType.METHOD, ElementType.PARAMETER})
  @MagicConstant(intValues = {
      TOP_LEVEL,
      DONE,
      OBJECT_EXPECT_FIELD_OR_END,
      OBJECT_EXPECT_VALUE,
      OBJECT_EXPECT_COMMA_OR_END,
      ARRAY_EXPECT_VALUE,
      ARRAY_EXPECT_VALUE_OR_END,
      ARRAY_EXPECT_COMMA_OR_END
  })
  private @interface State {}

  enum Token {
    OBJECT,
    ARRAY,
    STRING,
    BOOLEAN,
    NUMBER,
    NULL
  }
}
