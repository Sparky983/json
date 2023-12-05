package me.sparky983.json;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class JsonReader implements AutoCloseable {
  // Implementation notes:
  //  The main goals of this reader implementation are:
  //   - High throughput
  //   - Readable errors with a minimal stacktrace

  private static final String UNKNOWN_LITERAL = "Unknown literal";
  private static final String UNEXPECTED_EOF = "Unexpected end of input";

  /**
   * Used internally to represent the state in which no character has been read yet.
   */
  private static final int NO_LOOKAHEAD = -2; // -1 indicates the end of the sequence
  private int lookahead = NO_LOOKAHEAD;

  private final Reader reader;

  JsonReader(final Reader reader) {
    this.reader = reader;
  }

  Json readJson() throws IOException, JsonParseException {
    final Json json = readElement();
    if (peek() != -1) {
      throw new JsonParseException("Expected end of input");
    }
    return json;
  }

  private Json readElement() throws IOException, JsonParseException {
    skipWhitespace();
    final Json value = readValue();
    skipWhitespace();
    return value;
  }

  private Json readValue() throws IOException, JsonParseException {
    return switch (peek()) {
      case '{' -> readObject();
      case '[' -> readArray();
      case '"' -> readString();
      case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-' -> readNumber();
      case 't' -> readTrue();
      case 'f' -> readFalse();
      case 'n' -> readNull();
      case -1 -> throw new JsonParseException(UNEXPECTED_EOF);
      default -> throw new JsonParseException("Unexpected character \"" + (char) peek() + "\"");
    };
  }

  private Json.Object readObject() throws IOException, JsonParseException {
    consume(); // {
    skipWhitespace();

    if (peek() == '}') {
      consume();
      return Json.Object.EMPTY;
    }

    final LinkedHashMap<String, Json> members = new LinkedHashMap<>();

    while (true) {
      if (peek() != '"') {
        throw new JsonParseException("Expected a '\"'");
      }
      final String key = readString().value();

      skipWhitespace();

      if (peek() != ':') {
        throw new JsonParseException("Expected a \":\"");
      }

      consume();

      final Json element = readElement();

      if (members.put(key, element) != null) {
        // The spec does not require that members be unique, however, we have added this requirement
        //  to avoid the API being too confusing. I don't think there is any real use case for
        //  duplicate members anyway.
        throw new JsonParseException("Duplicate member \"" + key + "\"");
      }

      skipWhitespace();

      if (peek() != ',') {
        break;
      }

      consume();
      skipWhitespace();
    }

    if (peek() != '}') {
      throw new JsonParseException("Expected a \"}\"");
    }
    consume();

    // This unmodifiable map implementations isn't copied by the Json.Object constructor
    final Map<String, Json> unmodifiableMap = new InternalUnmodifiableMap(members);

    return new Json.Object(unmodifiableMap);
  }

  private Json.Array readArray() throws IOException, JsonParseException {
    consume(); // [
    skipWhitespace();

    if (peek() == ']') {
      consume();
      return Json.array();
    }

    final ArrayList<Json> members = new ArrayList<>();

    while (true) {
      members.add(readElement());

      skipWhitespace();

      if (peek() != ',') {
        break;
      }

      consume();
      skipWhitespace();
    }
    if (peek() != ']') {
      throw new JsonParseException("Expected a \"]\"");
    }

    consume();

    return new Json.Array(new InternalUnmodifiableList(members));
  }

  private Json.String readString() throws IOException, JsonParseException {
    consume(); // "

    final StringBuilder builder = new StringBuilder();

    while (peek() != '"') {
      final int c = peek();

      if (c < 0x0020) {
        if (c == -1) {
          throw new JsonParseException("Unterminated string");
        }
        throw new JsonParseException("Illegal character" + (char) c);
      }

      consume();

      if (c == '\\') { // Escape sequence
        final int type = peek();
        consume();
        final char escaped = switch (type) {
          case '"' -> '"';
          case '\\' -> '\\';
          case '/' -> '/';
          case 'b' -> '\b';
          case 'f' -> '\f';
          case 'n' -> '\n';
          case 'r' -> '\r';
          case 't' -> '\t';
          case 'u' -> {
            final int c1 = peek();
            consume();
            final int c2 = peek();
            consume();
            final int c3 = peek();
            consume();
            final int c4 = peek();
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
          default -> throw new JsonParseException("Illegal character escape character \"" + type + "\"");
        };
        builder.append(escaped);
      } else {
        builder.append((char) c);
      }
    }

    consume(); // "
    return Json.string(builder.toString());
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

  private Json.Number readNumber() throws IOException, JsonParseException {
    final StringBuilder builder = new StringBuilder();

    readInteger(builder);
    final boolean isDecimal = readFraction(builder);
    readExponent(builder);

    final String number = builder.toString();

    if (isDecimal) {
      return Json.decimal(new BigDecimal(number));
    } else {
      return Json.integer(new BigInteger(number));
    }
  }

  private void readInteger(final StringBuilder builder) throws IOException, JsonParseException {
    if (peek() == '-') {
      consume();
      builder.append('-');
    }

    final int first = peek();

    switch (first) {
      case -1 -> throw new JsonParseException(UNEXPECTED_EOF);
      case '0' -> {
        consume();
        builder.append('0');
      }
      case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
        consume();
        builder.append((char) first);
        while (true) {
          final int digit = peek();
          switch (digit) {
            case -1 -> throw new JsonParseException(UNEXPECTED_EOF);
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
      default -> throw new JsonParseException("Unexpected character \"" + (char) first + "\"");
    }
  }

  private boolean readFraction(final StringBuilder builder) throws IOException, JsonParseException {
    if (peek() == '.') {
      consume();
      builder.append('.');
    } else {
      return false;
    }

    readDigitsAtLeast1(builder);
    return true;
  }

  private void readExponent(final StringBuilder builder) throws IOException, JsonParseException {
    final int e = peek();

    switch (e) {
      case 'e', 'E' -> {
        consume();
        builder.append('E');
      }
      default -> {
        return;
      }
    }

    final int sign = peek();
    switch (peek()) {
      case -1 -> throw new JsonParseException(UNEXPECTED_EOF);
      case '-', '+' -> {
        consume();
        builder.append((char) sign);
      }
    }

    readDigitsAtLeast1(builder);
  }

  private void readDigitsAtLeast1(final StringBuilder builder) throws IOException, JsonParseException {
    while (true) {
      final int digit = peek();
      switch (digit) {
        case -1 -> throw new JsonParseException(UNEXPECTED_EOF);
        case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
          consume();
          builder.append((char) digit);
          switch (peek()) {
            case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {}
            default -> {
              return;
            }
          }
        }
      }
    }
  }

  private Json.True readTrue() throws IOException, JsonParseException {
    consume(); // t
    final int r = peek();
    consume();
    final int u  = peek();
    consume();
    final int e = peek();
    consume();

    if (r != 'r' || u != 'u' || e != 'e') {
      throw new JsonParseException(UNKNOWN_LITERAL);
    }

    return Json.TRUE;
  }

  private Json.False readFalse() throws IOException, JsonParseException {
    consume(); // f
    final int a = peek();
    consume();
    final int l  = peek();
    consume();
    final int s = peek();
    consume();
    final int e = peek();
    consume();

    if (a != 'a' || l != 'l' || s != 's' || e != 'e') {
      throw new JsonParseException(UNKNOWN_LITERAL);
    }

    return Json.FALSE;
  }

  private Json.Null readNull() throws IOException, JsonParseException {
    consume(); // n
    final int u = peek();
    consume();
    final int l1 = peek();
    consume();
    final int l2 = peek();
    consume();

    if (u != 'u' || l1 != 'l' || l2 != 'l') {
      throw new JsonParseException(UNKNOWN_LITERAL);
    }

    return Json.NULL;
  }

  private void skipWhitespace() throws IOException {
    while (true) {
      switch (peek()) {
        case ' ', '\n', '\r', '\t' -> consume();
        default -> {
          return;
        }
      }
    }
  }

  private int peek() throws IOException {
    if (lookahead == NO_LOOKAHEAD) {
      consume();
    }

    return lookahead;
  }

  private void consume() throws IOException {
    lookahead = reader.read();
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
