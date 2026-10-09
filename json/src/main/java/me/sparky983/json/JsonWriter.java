package me.sparky983.json;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import org.intellij.lang.annotations.MagicConstant;
import org.jspecify.annotations.Nullable;

final class JsonWriter implements AutoCloseable {
  private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

  // Use byte to represent states results in ~5% speed up
  private static final byte TOP_LEVEL = 1;
  private static final byte OBJECT_EXPECT_FIELD_OR_END = 2;
  private static final byte OBJECT_EXPECT_VALUE = 3;
  private static final byte OBJECT_EXPECT_COMMA_OR_END = 4;
  private static final byte ARRAY_EXPECT_VALUE_OR_END = 5;
  private static final byte ARRAY_EXPECT_COMMA_OR_END = 6;

  private final Writer writer;

  // Manage a stack ourselves results in ~5% speed up
  private byte[] state = new byte[8];
  private int stateSize = 1;

  private final @Nullable String indentation;

  JsonWriter(final Writer writer, final @Nullable String indentation) {
    this.writer = writer;
    this.indentation = indentation;
    state[0] = TOP_LEVEL;
  }

  void startObject() throws IOException {
    beforeValue();
    writer.write('{');
    pushState(OBJECT_EXPECT_FIELD_OR_END);
  }

  void endObject() throws IOException {
    final byte scope = peekState();
    if (scope != OBJECT_EXPECT_FIELD_OR_END && scope != OBJECT_EXPECT_COMMA_OR_END) {
      throw new IllegalStateException("Cannot end object");
    }

    if (scope == OBJECT_EXPECT_COMMA_OR_END && indentation != null) {
      newlineAndIndent(stateSize - 2);
    }

    popState();
    writer.write('}');
    afterValue();
  }

  void writeField(final String key) throws IOException {
    switch (peekState()) {
      case OBJECT_EXPECT_FIELD_OR_END -> {}
      case OBJECT_EXPECT_COMMA_OR_END -> writer.write(',');
      default -> throw new IllegalStateException("Cannot write a field outside of object");
    }

    if (indentation != null) {
      newlineAndIndent(stateSize - 1);
    }

    writeEscapedString(key);
    writer.write(':');
    if (indentation != null) {
      writer.write(' ');
    }
    replaceState(OBJECT_EXPECT_VALUE);
  }

  void startArray() throws IOException {
    beforeValue();
    writer.write('[');
    pushState(ARRAY_EXPECT_VALUE_OR_END);
  }

  void endArray() throws IOException {
    final byte state = peekState();
    if (state != ARRAY_EXPECT_VALUE_OR_END && state != ARRAY_EXPECT_COMMA_OR_END) {
      throw new IllegalStateException("Cannot end array");
    }

    if (state == ARRAY_EXPECT_COMMA_OR_END && indentation != null) {
      newlineAndIndent(stateSize - 2);
    }

    popState();
    writer.write(']');
    afterValue();
  }

  void writeString(final String string) throws IOException {
    beforeValue();
    writeEscapedString(string);
    afterValue();
  }

  private void writeEscapedString(final String string) throws IOException {
    writer.write('"');
    final int length = string.length();
    int chunkStart = 0;
    for (int i = 0; i < length; i++) {
      final char c = string.charAt(i);
      switch (c) {
        case '"' -> {
          writer.write(string, chunkStart, i - chunkStart);
          writer.write("\\\"");
          chunkStart = i + 1;
        }
        case '\\' -> {
          writer.write(string, chunkStart, i - chunkStart);
          writer.write("\\\\");
          chunkStart = i + 1;
        }
        case '\b' -> {
          writer.write(string, chunkStart, i - chunkStart);
          writer.write("\\b");
          chunkStart = i + 1;
        }
        case '\f' -> {
          writer.write(string, chunkStart, i - chunkStart);
          writer.write("\\f");
          chunkStart = i + 1;
        }
        case '\n' -> {
          writer.write(string, chunkStart, i - chunkStart);
          writer.write("\\n");
          chunkStart = i + 1;
        }
        case '\r' -> {
          writer.write(string, chunkStart, i - chunkStart);
          writer.write("\\r");
          chunkStart = i + 1;
        }
        case '\t' -> {
          writer.write(string, chunkStart, i - chunkStart);
          writer.write("\\t");
          chunkStart = i + 1;
        }
        default -> {
          if (c < 0x0020) {
            writer.write(string, chunkStart, i - chunkStart);
            writeUnicodeEscape(c);
            chunkStart = i + 1;
          }
        }
      }
    }
    writer.write(string, chunkStart, length - chunkStart);
    writer.write('"');
  }

  private void writeUnicodeEscape(final char c) throws IOException {
    writer.write("\\u00");
    writer.write(HEX_DIGITS[(c >>> 4) & 0xF]);
    writer.write(HEX_DIGITS[c & 0xF]);
  }

  void writeInteger(final BigInteger integer) throws IOException {
    beforeValue();
    writer.write(integer.toString());
    afterValue();
  }

  void writeDecimal(final BigDecimal decimal) throws IOException {
    beforeValue();
    final String value = decimal.toString();
    final int dot = value.indexOf('.');
    if (dot != -1) {
      writer.write(value);
    } else {
      final int exponent = Math.max(value.indexOf('E'), value.indexOf('e'));
      if (exponent == -1) {
        writer.write(value);
        writer.write(".0");
      } else {
        writer.write(value, 0, exponent);
        writer.write(".0");
        writer.write(value, exponent, value.length() - exponent);
      }
    }
    afterValue();
  }

  void writeNull() throws IOException {
    beforeValue();
    writer.write("null");
    afterValue();
  }

  void writeBool(final Json.Bool bool) throws IOException {
    beforeValue();
    writer.write(bool == Json.Bool.TRUE ? "true" : "false");
    afterValue();
  }

  private void beforeValue() throws IOException {
    switch (peekState()) {
      case TOP_LEVEL, OBJECT_EXPECT_VALUE -> {}
      case ARRAY_EXPECT_VALUE_OR_END -> {
        if (indentation != null) {
          newlineAndIndent(stateSize - 1);
        }
      }
      case ARRAY_EXPECT_COMMA_OR_END -> {
        writer.write(',');
        if (indentation != null) {
          newlineAndIndent(stateSize - 1);
        }
      }
      case OBJECT_EXPECT_FIELD_OR_END, OBJECT_EXPECT_COMMA_OR_END ->
          throw new IllegalStateException("Object field must be written before writing its value");
      default -> throw new IllegalStateException();
    }
  }

  private void afterValue() {
    switch (popState()) {
      case TOP_LEVEL -> {
        assert stateSize == 0;
      }
      case OBJECT_EXPECT_VALUE -> pushState(OBJECT_EXPECT_COMMA_OR_END);
      case ARRAY_EXPECT_VALUE_OR_END, ARRAY_EXPECT_COMMA_OR_END -> pushState(ARRAY_EXPECT_COMMA_OR_END);
      default -> throw new IllegalStateException();
    }
  }

  private void newlineAndIndent(final int amount) throws IOException {
    writer.write('\n');
    if (indentation != null) {
      for (int i = 0; i < amount; i++) {
        writer.write(indentation);
      }
    }
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }

  @SuppressWarnings("MagicConstant")
  @State
  private byte peekState() {
    if (stateSize == 0) {
      throw new IllegalStateException("JSON has already been fully written");
    }
    final byte popped = state[stateSize - 1];
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

  @Target({ElementType.METHOD, ElementType.PARAMETER})
  @MagicConstant(intValues = {
      TOP_LEVEL,
      OBJECT_EXPECT_FIELD_OR_END,
      OBJECT_EXPECT_VALUE,
      OBJECT_EXPECT_COMMA_OR_END,
      ARRAY_EXPECT_VALUE_OR_END,
      ARRAY_EXPECT_COMMA_OR_END
  })
  private @interface State {}
}
