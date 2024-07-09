package me.sparky983.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public sealed interface Json {
  True TRUE = new True();
  False FALSE = new False();
  Null NULL = new Null();

  static Json read(final Reader input) throws IOException, JsonParseException {
    try (final JsonReader reader = new JsonReader(input)) {
      return reader.readJson();
    }
  }

  static Json read(final java.lang.String input) throws JsonParseException {
    try {
      return read(new StringReader(input));
    } catch (final IOException e) {
      throw new AssertionError("StringReader must not perform IO operations", e);
    }
  }

  static void write(final Json json, final Writer output) throws IOException {
    try (final JsonWriter writer = new JsonWriter(output, null)) {
      writer.writeJson(json);
    }
  }

  static java.lang.String write(final Json json) {
    final StringWriter stringWriter = new StringWriter();
    try (final JsonWriter jsonWriter = new JsonWriter(stringWriter, null)) {
      jsonWriter.writeJson(json);
    } catch (final IOException e) {
      throw new AssertionError("StringWriter must not perform IO operations", e);
    }
    return stringWriter.toString();
  }

  @SuppressWarnings("unchecked")
  static Object object(final Map<? extends java.lang.String, ? extends Json> members) {
    if (members.isEmpty()) { // implicit null check
      return Object.EMPTY;
    }

    return new Object((Map<java.lang.String, Json>) members); // safe
  }

  // IDEA-356054
  static Json.Object.Builder object() {
    return new Object.Builder();
  }

  @SuppressWarnings("unchecked")
  static Array array(final List<? extends Json> elements) {
    if (elements.isEmpty()) { // implicit null check
      return Array.EMPTY;
    }

    return new Array((List<Json>) elements); // safe
  }

  static Array array(final Json... elements) {
    if (elements.length == 0) {
      return Array.EMPTY;
    }

    return new Array(elements);
  }

  static Array array() {
    return Array.EMPTY;
  }

  static Integer integer(final BigInteger value) {
    try {
      final int intValue = value.intValueExact();
      if (intValue >= Integer.MIN_CACHED && intValue <= Integer.MAX_CACHED) {
        return Integer.CACHED[intValue - Integer.OFFSET];
      }
    } catch (final ArithmeticException e) {
      // if the value can't fit, don't use the cache
    }

    return new Integer(value);
  }

  static Integer integer(final long value) {
    if (value >= Integer.MIN_CACHED && value <= Integer.MAX_CACHED) {
      return Integer.CACHED[(int) value - Integer.OFFSET];
    }
    return new Integer(value);
  }

  static Decimal decimal(final BigDecimal value) {
    final double doubleValue = value.doubleValue();

    final Decimal cached = cachedDecimal(doubleValue);

    return cached != null
        ? cached
        : new Decimal(value);
  }

  static Decimal decimal(final double value) {
    final Decimal cached = cachedDecimal(value);

    return cached != null
        ? cached
        : new Decimal(value);
  }

  private static Decimal cachedDecimal(final double value) {
    if (value == 0D) {
      return Decimal.ZERO;
    }

    if (value == 1D) {
      return Decimal.ONE;
    }

    if (value == 2D) {
      return Decimal.TWO;
    }

    if (value == 10D) {
      return Decimal.TEN;
    }

    return null;
  }

  static String string(java.lang.String value) {
    if (value.isEmpty()) {
      return String.EMPTY;
    }

    return new String(value);
  }

  static Bool bool(final boolean isTrue) {
    if (isTrue) {
      return TRUE;
    }

    return FALSE;
  }

  record Object(Map<java.lang.String, Json> members) implements Json {
    static final Object EMPTY = new Object(Map.of());

    public Object(final Map<java.lang.String, Json> members) {
      if (members instanceof InternalUnmodifiableMap) {
        this.members = members;
      } else {
        this.members = new InternalUnmodifiableMap(new LinkedHashMap<>(members));
      }
    }

    @Override
    public int hashCode() {
      return members.hashCode();
    }

    @Override
    public java.lang.String toString() {
      return write(this);
    }

    // Not thread-safe
    public static final class Builder {
      private LinkedHashMap<java.lang.String, Json> values = null;

      public Builder put(final java.lang.String key, final Json value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        if (values == null) {
          values = new LinkedHashMap<>();
        }

        if (values.putIfAbsent(key, value) != null) {
          throw new IllegalStateException("Member \"" + key + "\" has already been added");
        }

        return this;
      }

      public Object build() {
        final LinkedHashMap<java.lang.String, Json> values = this.values;

        if (values == null) {
          return EMPTY;
        }

        return new Object(values);
      }
    }
  }

  record Array(List<Json> elements) implements Json {
    static final Array EMPTY = new Array(List.of());

    public Array(final List<Json> elements) {
      if (elements instanceof InternalUnmodifiableList) {
        this.elements = elements;
      } else {
        this.elements = List.copyOf(elements);
      }
    }

    public Array(final Json... values) {
      this(List.of(values));
    }

    @Override
    public java.lang.String toString() {
      return write(this);
    }
  }

  sealed interface Number extends Json {}

  record Integer(BigInteger value) implements Number {
    private static final int MIN_CACHED = -256;
    private static final int MAX_CACHED = 255;
    private static final int OFFSET = MIN_CACHED;
    /**
     * Cached values from -256 - 255
     */
    private static final Integer[] CACHED = new Integer[512];

    static {
      for (int i = 0; i < CACHED.length; i++) {
        final int n = i + OFFSET;
        CACHED[i] = new Integer(switch (n) {
          case 0 -> BigInteger.ZERO;
          case 1 -> BigInteger.ONE;
          case 2 -> BigInteger.TWO;
          case 10 -> BigInteger.TEN;
          default -> BigInteger.valueOf(n);
        });
      }
    }

    public Integer(final long value) {
      this(BigInteger.valueOf(value));
    }

    @Override
    public java.lang.String toString() {
      return write(this);
    }
  }

  record Decimal(BigDecimal value) implements Number {
    private static final Decimal ZERO = new Decimal(BigDecimal.ZERO);
    private static final Decimal ONE = new Decimal(BigDecimal.ONE);
    private static final Decimal TWO = new Decimal(BigDecimal.TWO);
    private static final Decimal TEN = new Decimal(BigDecimal.TEN);

    public Decimal {
      Objects.requireNonNull(value);
    }

    public Decimal(final double value) {
      this(BigDecimal.valueOf(value));
    }

    @Override
    public java.lang.String toString() {
      return write(this);
    }
  }

  record String(java.lang.String value) implements Json {
    static final String EMPTY = new String("");

    public String {
      Objects.requireNonNull(value);
    }

    @Override
    public java.lang.String toString() {
      return write(this);
      }
  }

  sealed interface Bool extends Json {
    boolean isTrue();

    boolean isFalse();
  }

  record True() implements Bool {
    @Override
    public boolean isTrue() {
      return true;
    }

    @Override
    public boolean isFalse() {
      return false;
    }

    @Override
    public java.lang.String toString() {
      return write(this);
    }
  }

  record False() implements Bool {
    @Override
    public boolean isTrue() {
      return false;
    }

    @Override
    public boolean isFalse() {
      return true;
    }

    @Override
    public java.lang.String toString() {
      return write(this);
    }
  }

  record Null() implements Json {
    @Override
    public java.lang.String toString() {
      return write(this);
    }
  }
}
