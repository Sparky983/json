package me.sparky983.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link JsonReaderTest}.
 * 
 * <p>These tests specify, exhaustively, the behaviour of JsonReaderTest
 */
@SuppressWarnings("resource")
class JsonReaderTest {
  static JsonReader newReader(final String input) {
    return new JsonReader(new StringReader(input));
  }
  
  static List<Arguments> provideStrings() {
    return List.of(
        Arguments.of("\\b", "\b"),
        Arguments.of("\\t", "\t"),
        Arguments.of("\\n", "\n"),
        Arguments.of("\\f", "\f"),
        Arguments.of("\\r", "\r"),
        Arguments.of("\\u0000", "\u0000"),
        Arguments.of("\\u0001", "\u0001"),
        Arguments.of("\\u0002", "\u0002"),
        Arguments.of("\\u0003", "\u0003"),
        Arguments.of("\\u0004", "\u0004"),
        Arguments.of("\\u0005", "\u0005"),
        Arguments.of("\\u0006", "\u0006"),
        Arguments.of("\\u0007", "\u0007"),
        Arguments.of("\\u0008", "\u0008"),
        Arguments.of("\\u0009", "\t"),
        Arguments.of("\\u000a", "\n"),
        Arguments.of("\\u000b", "\u000b"),
        Arguments.of("\\u000c", "\u000c"),
        Arguments.of("\\u000d", "\r"),
        Arguments.of("\\u000e", "\u000e"),
        Arguments.of("\\u000f", "\u000f"),
        Arguments.of("\\u000A", "\n"),
        Arguments.of("\\u000B", "\u000b"),
        Arguments.of("\\u000C", "\u000c"),
        Arguments.of("\\u000D", "\r"),
        Arguments.of("\\u000E", "\u000e"),
        Arguments.of("\\u000F", "\u000f"),
        Arguments.of(" ", " "),
        Arguments.of("\\r\\t\\n\\u0001 abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", "\r\t\n\u0001 abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        Arguments.of("", "")
    );
  }
  
  static List<Arguments> provideNonTerminatingStrings() {
    return List.of(
        Arguments.of("\""),
        Arguments.of("\"abc"),
        Arguments.of("\"\\\""),
        Arguments.of("\"\\\\"),
        Arguments.of("\"\\")
    );
  }
  
  static List<Arguments> provideIllegalCharacters() {
    return IntStream.rangeClosed(0, 0x0019)
        .mapToObj(n -> Arguments.of((char) n))
        .toList();
  }

  static List<Arguments> provideIllegalEscapeSequences() {
    return List.of(
        Arguments.of("\"\\u"),
        Arguments.of("\"\\u0"),
        Arguments.of("\"\\u00"),
        Arguments.of("\"\\u000"),
        Arguments.of("\"\\uxxxx\":"),
        Arguments.of("\"\\u0xxx\":"),
        Arguments.of("\"\\u00xx\":"),
        Arguments.of("\"\\u000x\":"),
        Arguments.of("\"\\\":"),
        Arguments.of("\"\\"),
        Arguments.of("\"\\a\":")
    );
  }

  // transition from TOP_LEVEL
  @Test
  void testReadField_TopLevel_Throws() {
    final JsonReader reader = newReader("");

    assertThrows(IllegalStateException.class, reader::readField);
  }

  // transition from DONE
  @Test
  void testReadField_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0\"field_name\":");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::readField);
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testReadField_ObjectValue_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":\"not_a_field_name\"");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertThrows(IllegalStateException.class, reader::readField);
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testReadField_InArrayAfterValueAndPeek_Throws() throws Exception {
    final JsonReader reader = newReader("[0,\"field_name\":");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    // peek will advance the lookahead, resulting in a unique state that can only be reached this
    // way
    assumeTrue(reader.peek() == JsonReader.Token.STRING);

    assertThrows(IllegalStateException.class, reader::readField);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testReadField_FirstInArray_Throws() throws Exception {
    final JsonReader reader = newReader("[\"not_a_field_name\":");
    reader.startArray();

    assertThrows(IllegalStateException.class, reader::readField);
  }
  
  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testReadField_SubsequentInArray_Throws() throws Exception {
    final JsonReader reader = newReader("[0,\"field_name\":");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::readField);
  }
  
  // transition from OBJECT_EXPECT_FIELD_OR_END when FIELD
  @ParameterizedTest
  @ValueSource(strings = {"{\"field_name\":", "{ \r\t\n\"field_name\":"})
  void testReadField_FirstInObjectWhenField(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();
    
    final String name = reader.readField();
    
    assertEquals("field_name", name);
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END when no field
  @ParameterizedTest
  @ValueSource(strings = {"{", "{ \r\t\n"})
  void testReadField_FirstInObjectWhenNoField(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();

    assertThrows(JsonParseException.class, reader::readField);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END when comma and no field
  @ParameterizedTest
  @ValueSource(strings = {"{\"field_name\":0,", "{\"field_name\":0, \r\t\n"})
  void testReadField_SubsequentWhenCommaNoField(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();
    assumeTrue(reader.readField().equals("field_name"));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(JsonParseException.class, reader::readField);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END when no comma
  @ParameterizedTest
  @ValueSource(strings = {"{\"field_name\":0", "{\"field_name\":0 \r\t\n"})
  void testReadField_SubsequentWhenNoComma(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();
    assumeTrue(reader.readField().equals("field_name"));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(JsonParseException.class, reader::readField);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END when comma and field
  @Test
  void testReadField_SubsequentWhenComma() throws Exception {
    final JsonReader reader = newReader("{\"field_name_1\":0, \r\t\n\"field_name_2\":0,\"field_name_3\":");
    reader.startObject();
    assumeTrue(reader.readField().equals("field_name_1"));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    final String name2 = reader.readField();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    final String name3 = reader.readField();

    assertEquals("field_name_2", name2);
    assertEquals("field_name_3", name3);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END when end
  @ParameterizedTest
  @ValueSource(strings = {"{\"field_name\":0}", "{\"field_name\":0 \r\t\n}"})
  void testReadField_SubsequentWhenEnd(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();
    assumeTrue(reader.readField().equals("field_name"));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(JsonParseException.class, reader::readField);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{ \r\t\n\"field_name\":", "{\"field_name\" \r\t\n:"})
  void testReadField_Whitespace(final String json) throws Exception {
      final JsonReader reader = newReader(json);
      reader.startObject();
      
      final String name = reader.readField();

      assertEquals("field_name", name);
  }

  @Test
  void testReadField_AcrossBufferBoundary() throws Exception {
    final String name = "x".repeat(JsonReader.BUFFER_SIZE * 3);
    final JsonReader reader = newReader("{\"" + name + "\":");
    reader.startObject();
    
    assertEquals(name, reader.readField());
  }
  
  @ParameterizedTest
  @MethodSource("provideStrings")
  void testReadField_EscapeSequence(final String escapedSequence, final String escapeSequence)
      throws Exception {
    final JsonReader reader = newReader("{\"" + escapedSequence + "\":");
    reader.startObject();
    
    final String name = reader.readField();
    
    assertEquals(escapeSequence, name);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{field_name\":", "{ \r\n\tfield_name\":"})
  void testReadField_NoLeadingQuote_Throws(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();

    assertThrows(JsonParseException.class, reader::readField);
  }

  @ParameterizedTest
  @MethodSource("provideNonTerminatingStrings")
  void testReadField_NonTerminating_Throws(final String string) throws Exception {
    final JsonReader reader = newReader("{" + string);
    reader.startObject();
    
    assertThrows(JsonParseException.class, reader::readField);
  }

  @ParameterizedTest
  @MethodSource("provideIllegalCharacters")
  void testReadField_IllegalCharacter_Throws(final char c) throws Exception {
    final JsonReader reader = newReader("{\"" + c + "\":");
    reader.startObject();
    
    assertThrows(JsonParseException.class, reader::readField);
  }

  @ParameterizedTest
  @MethodSource("provideIllegalEscapeSequences")
  void testReadField_NonTerminatingEscapeSequence(final String escapeSequence) throws Exception {
    final JsonReader reader = newReader("{" + escapeSequence);
    reader.startObject();

    assertThrows(JsonParseException.class, reader::readField);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"field_name\"", "{\"field_name\" \r\t\n"})
  void testReadField_NoColon(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();

    assertThrows(JsonParseException.class, reader::readField);
  }

  // transition from TOP_LEVEL
  @ParameterizedTest
  @ValueSource(strings = {"\"value\"", " \r\t\n\"value\""})
  void testReadString_TopLevel(final String json) throws Exception {
    final JsonReader reader = newReader(json);

    assertEquals("value", reader.readString());
  }

  // transition from DONE
  @Test
  void testReadString_Done_Throws() throws Exception {
    final JsonReader reader = newReader("\"value\"");
    assumeTrue("value".equals(reader.readString()));

    assertThrows(IllegalStateException.class, reader::readString);
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testReadString_ObjectValue() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":\"value\"}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertEquals("value", reader.readString());
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testReadString_ArraySubsequentAfterPeek() throws Exception {
    final JsonReader reader = newReader("[0,\"value\"]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.STRING);

    assertEquals("value", reader.readString());
  }

  // transition from TOP_LEVEL
  @ParameterizedTest
  @CsvSource({
      "0, 0",
      "-1, -1",
      "12, 12",
      "'-1 ', -1",
      "'12 ', 12",
      "'1.0 ', 1.0",
      "'1e2 ', 1E2",
      "'-1.5e+2 ', -1.5E+2"
  })
  void testReadNumber_TopLevel(final String json, final String expected) throws Exception {
    final JsonReader reader = newReader(json);

    assertEquals(expected, reader.readNumber());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testReadNumber_ObjectValue() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":-1.5e+2}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertEquals("-1.5E+2", reader.readNumber());
  }

  // transition from TOP_LEVEL
  @ParameterizedTest
  @CsvSource({
      "0, 0",
      "-1, -1",
      "12, 12",
      "'-1 ', -1",
      "'12 ', 12"
  })
  void testReadInteger_TopLevel(final String json, final String expected) throws Exception {
    final JsonReader reader = newReader(json);

    assertEquals(new java.math.BigInteger(expected), reader.readInteger());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testReadInteger_ArraySubsequent() throws Exception {
    final JsonReader reader = newReader("[0,12]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertEquals(new java.math.BigInteger("12"), reader.readInteger());
  }

  // transition from TOP_LEVEL
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testReadBoolean_TopLevel(final boolean expected) throws Exception {
    final JsonReader reader = newReader(Boolean.toString(expected));

    assertEquals(expected, reader.readBoolean());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testReadBoolean_ObjectValue() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":false}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertFalse(reader.readBoolean());
  }

  // transition from TOP_LEVEL
  @Test
  void testReadNull_TopLevel() throws Exception {
    final JsonReader reader = newReader("null");

    reader.readNull();
    assertTrue(reader.isEof());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testReadNull_ObjectValue() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":null,\"field_name_2\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    reader.readNull();

    assertEquals("field_name_2", reader.readField());
    assertEquals(0, reader.readInteger().intValueExact());
  }

  // transition from TOP_LEVEL
  @Test
  void testStartObject_TopLevel() throws Exception {
    final JsonReader reader = newReader("{}");

    reader.startObject();
    reader.endObject();

    assertTrue(reader.isEof());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testStartObject_ObjectValue() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":{\"field_name_2\":0}}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    reader.startObject();

    assertEquals("field_name_2", reader.readField());
    assertEquals(0, reader.readInteger().intValueExact());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testStartObject_ArrayFirstValue() throws Exception {
    final JsonReader reader = newReader("[{}]");
    reader.startArray();

    reader.startObject();
    reader.endObject();
    reader.endArray();

    assertTrue(reader.isEof());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testStartObject_ArraySubsequent() throws Exception {
    final JsonReader reader = newReader("[0,{}]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    reader.startObject();
    reader.endObject();
    reader.endArray();

    assertTrue(reader.isEof());
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testStartObject_ArrayValueAfterPeek() throws Exception {
    final JsonReader reader = newReader("[0,{}]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.OBJECT);

    reader.startObject();
    reader.endObject();
    reader.endArray();

    assertTrue(reader.isEof());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testStartObject_ObjectField_Throws() throws Exception {
    final JsonReader reader = newReader("{");
    reader.startObject();

    assertThrows(IllegalStateException.class, reader::startObject);
  }

  // transition from DONE
  @Test
  void testStartObject_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::startObject);
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END when end
  @Test
  void testEndObject_Empty() throws Exception {
    final JsonReader reader = newReader("{}");
    reader.startObject();

    reader.endObject();

    assertTrue(reader.isEof());
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END when end
  @Test
  void testEndObject_NonEmpty() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    reader.endObject();

    assertTrue(reader.isEof());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END when EOF
  @ParameterizedTest
  @ValueSource(strings = {"{", "{ \r\t\n"})
  void testEndObject_MissingClosingBrace(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();

    assertThrows(JsonParseException.class, reader::endObject);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END when EOF
  @ParameterizedTest
  @ValueSource(strings = {"{\"field_name\":0", "{\"field_name\":0 \r\t\n"})
  void testEndObject_SubsequentMissingClosingBrace(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(JsonParseException.class, reader::endObject);
  }

  // transition from TOP_LEVEL
  @Test
  void testEndObject_TopLevel_Throws() {
    final JsonReader reader = newReader("");

    assertThrows(IllegalStateException.class, reader::endObject);
  }

  // transition from TOP_LEVEL
  @Test
  void testStartArray_TopLevel() throws Exception {
    final JsonReader reader = newReader("[]");

    reader.startArray();
    reader.endArray();

    assertTrue(reader.isEof());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testStartArray_ObjectValue() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":[0]}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    reader.startArray();

    assertEquals(0, reader.readInteger().intValueExact());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testStartArray_ArrayFirstValue() throws Exception {
    final JsonReader reader = newReader("[[]]");
    reader.startArray();

    reader.startArray();
    reader.endArray();
    reader.endArray();

    assertTrue(reader.isEof());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testStartArray_ArraySubsequent() throws Exception {
    final JsonReader reader = newReader("[0,[]]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    reader.startArray();
    reader.endArray();
    reader.endArray();

    assertTrue(reader.isEof());
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testStartArray_ArrayValueAfterPeek() throws Exception {
    final JsonReader reader = newReader("[0,[]]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.ARRAY);

    reader.startArray();
    reader.endArray();
    reader.endArray();

    assertTrue(reader.isEof());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testStartArray_ObjectField_Throws() throws Exception {
    final JsonReader reader = newReader("{");
    reader.startObject();

    assertThrows(IllegalStateException.class, reader::startArray);
  }

  // transition from DONE
  @Test
  void testStartArray_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::startArray);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END when end
  @Test
  void testEndArray_Empty() throws Exception {
    final JsonReader reader = newReader("[]");
    reader.startArray();

    reader.endArray();

    assertTrue(reader.isEof());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END when end
  @Test
  void testEndArray_NonEmpty() throws Exception {
    final JsonReader reader = newReader("[0]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    reader.endArray();

    assertTrue(reader.isEof());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END when EOF
  @ParameterizedTest
  @ValueSource(strings = {"[", "[ \r\t\n"})
  void testEndArray_MissingClosingBracket(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startArray();

    assertThrows(JsonParseException.class, reader::endArray);
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END when EOF
  @ParameterizedTest
  @ValueSource(strings = {"[0", "[0 \r\t\n"})
  void testEndArray_SubsequentMissingClosingBracket(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(JsonParseException.class, reader::endArray);
  }

  // transition from TOP_LEVEL
  @Test
  void testEndArray_TopLevel_Throws() {
    final JsonReader reader = newReader("");

    assertThrows(IllegalStateException.class, reader::endArray);
  }

  // transition from TOP_LEVEL
  @ParameterizedTest
  @CsvSource({
      "'{}', OBJECT",
      "'[]', ARRAY",
      "'\"value\"', STRING",
      "0, NUMBER",
      "true, BOOLEAN",
      "null, NULL"
  })
  void testPeek_TopLevel(final String json, final String expected) throws Exception {
    final JsonReader reader = newReader(json);

    assertEquals(JsonReader.Token.valueOf(expected), reader.peek());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testPeek_ObjectField_Throws() throws Exception {
    final JsonReader reader = newReader("{}");
    reader.startObject();

    assertThrows(IllegalStateException.class, reader::peek);
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END when comma and value
  @Test
  void testPeek_ArraySubsequentConsumesComma() throws Exception {
    final JsonReader reader = newReader("[0,\"value\"]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertEquals(JsonReader.Token.STRING, reader.peek());
    assertEquals("value", reader.readString());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END when field
  @Test
  void testHasNext_ObjectFirstWhenField() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();

    assertTrue(reader.hasNext());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END when end
  @Test
  void testHasNext_ObjectFirstWhenEnd() throws Exception {
    final JsonReader reader = newReader("{}");
    reader.startObject();

    assertFalse(reader.hasNext());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END when EOF
  @ParameterizedTest
  @ValueSource(strings = {"{", "{ \r\t\n"})
  void testHasNext_ObjectFirstWhenEof(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();

    assertFalse(reader.hasNext());
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END when comma
  @Test
  void testHasNext_ObjectSubsequentWhenComma() throws Exception {
    final JsonReader reader = newReader("{\"field_name_1\":0,\"field_name_2\":0}");
    reader.startObject();
    assumeTrue("field_name_1".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertTrue(reader.hasNext());
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END when end
  @Test
  void testHasNext_ObjectSubsequentWhenEnd() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertFalse(reader.hasNext());
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END when EOF
  @ParameterizedTest
  @ValueSource(strings = {"{\"field_name\":0", "{\"field_name\":0 \r\t\n"})
  void testHasNext_ObjectSubsequentWhenEof(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertFalse(reader.hasNext());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END when value
  @Test
  void testHasNext_ArrayFirstWhenValue() throws Exception {
    final JsonReader reader = newReader("[0]");
    reader.startArray();

    assertTrue(reader.hasNext());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END when end
  @Test
  void testHasNext_ArrayFirstWhenEnd() throws Exception {
    final JsonReader reader = newReader("[]");
    reader.startArray();

    assertFalse(reader.hasNext());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END when EOF
  @ParameterizedTest
  @ValueSource(strings = {"[", "[ \r\t\n"})
  void testHasNext_ArrayFirstWhenEof(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startArray();

    assertFalse(reader.hasNext());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END when comma
  @Test
  void testHasNext_ArraySubsequentWhenComma() throws Exception {
    final JsonReader reader = newReader("[0,1]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertTrue(reader.hasNext());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END when end
  @Test
  void testHasNext_ArraySubsequentWhenEnd() throws Exception {
    final JsonReader reader = newReader("[0]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertFalse(reader.hasNext());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END when EOF
  @ParameterizedTest
  @ValueSource(strings = {"[0", "[0 \r\t\n"})
  void testHasNext_ArraySubsequentWhenEof(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertFalse(reader.hasNext());
  }

  // transition from TOP_LEVEL
  @Test
  void testHasNext_TopLevel_Throws() {
    final JsonReader reader = newReader("");

    assertThrows(IllegalStateException.class, reader::hasNext);
  }

  // transition from DONE when EOF
  @Test
  void testIsEof_DoneTrue() throws Exception {
    final JsonReader reader = newReader("0 \r\t\n");
    reader.readInteger();

    assertTrue(reader.isEof());
  }

  // transition from DONE when trailing input remains
  @Test
  void testIsEof_DoneFalseWhenTrailingInputRemains() throws Exception {
    final JsonReader reader = newReader("0[]");
    reader.readInteger();

    assertFalse(reader.isEof());
  }

  // transition from TOP_LEVEL
  @Test
  void testIsEof_NotDone_Throws() {
    final JsonReader reader = newReader("");

    assertThrows(IllegalStateException.class, reader::isEof);
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testReadString_ObjectField_Throws() throws Exception {
    final JsonReader reader = newReader("{");
    reader.startObject();

    assertThrows(IllegalStateException.class, reader::readString);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testReadString_ObjectCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::readString);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testReadString_ArrayFirstValue() throws Exception {
    final JsonReader reader = newReader("[\"value\"]");
    reader.startArray();

    assertEquals("value", reader.readString());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testReadString_ArraySubsequentWithoutPeek() throws Exception {
    final JsonReader reader = newReader("[0,\"value\"]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertEquals("value", reader.readString());
  }

  @ParameterizedTest
  @MethodSource("provideNonTerminatingStrings")
  void testReadString_NonTerminating_Throws(final String string) throws Exception {
    final JsonReader reader = newReader(string);

    assertThrows(JsonParseException.class, reader::readString);
  }

  @Test
  void testReadString_AcrossBufferBoundary() throws Exception {
    final String value = "x".repeat(JsonReader.BUFFER_SIZE * 3);
    final JsonReader reader = newReader("\"" + value + "\"");

    assertEquals(value, reader.readString());
  }

  @ParameterizedTest
  @MethodSource("provideStrings")
  void testReadString_EscapeSequence(final String escapedSequence, final String escapeSequence)
      throws Exception {
    final JsonReader reader = newReader("\"" + escapedSequence + "\"");

    assertEquals(escapeSequence, reader.readString());
  }

  @ParameterizedTest
  @MethodSource("provideIllegalCharacters")
  void testReadString_IllegalCharacter_Throws(final char c) {
    final JsonReader reader = newReader("\"" + c + "\"");

    assertThrows(JsonParseException.class, reader::readString);
  }

  @ParameterizedTest
  @MethodSource("provideIllegalEscapeSequences")
  void testReadString_NonTerminatingEscapeSequence(final String escapeSequence) {
    final JsonReader reader = newReader(escapeSequence);

    assertThrows(JsonParseException.class, reader::readString);
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testReadNumber_ObjectField_Throws() throws Exception {
    final JsonReader reader = newReader("{");
    reader.startObject();

    assertThrows(IllegalStateException.class, reader::readNumber);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testReadNumber_ObjectCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::readNumber);
  }

  // transition from DONE
  @Test
  void testReadNumber_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0 ");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::readNumber);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testReadNumber_ArrayFirstValue() throws Exception {
    final JsonReader reader = newReader("[1.0]");
    reader.startArray();

    assertEquals("1.0", reader.readNumber());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testReadNumber_ArraySubsequentWithoutPeek() throws Exception {
    final JsonReader reader = newReader("[0,12 ]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertEquals("12", reader.readNumber());
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testReadNumber_ArrayValueAfterPeek() throws Exception {
    final JsonReader reader = newReader("[0,12 ]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.NUMBER);

    assertEquals("12", reader.readNumber());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " \r\t\n", "-", "- \r\t\n", "1.", "1. \r\t\n", "1e", "1e \r\t\n", "1e+", "1e+ \r\t\n", "x", " x"})
  void testReadNumber_Malformed_Throws(final String json) {
    final JsonReader reader = newReader(json);

    assertThrows(JsonParseException.class, reader::readNumber);
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testReadInteger_ObjectField_Throws() throws Exception {
    final JsonReader reader = newReader("{");
    reader.startObject();

    assertThrows(IllegalStateException.class, reader::readInteger);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testReadInteger_ObjectCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::readInteger);
  }

  // transition from DONE
  @Test
  void testReadInteger_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0 ");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::readInteger);
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testReadInteger_ObjectValue() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":-1 }");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertEquals(new java.math.BigInteger("-1"), reader.readInteger());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testReadInteger_ArrayFirstValue() throws Exception {
    final JsonReader reader = newReader("[12 ]");
    reader.startArray();

    assertEquals(new java.math.BigInteger("12"), reader.readInteger());
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testReadInteger_ArrayValueAfterPeek() throws Exception {
    final JsonReader reader = newReader("[0,12 ]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.NUMBER);

    assertEquals(new java.math.BigInteger("12"), reader.readInteger());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " \r\t\n", "-", "- \r\t\n", "x", " x"})
  void testReadInteger_Malformed_Throws(final String json) {
    final JsonReader reader = newReader(json);

    assertThrows(JsonParseException.class, reader::readInteger);
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testReadBoolean_ObjectField_Throws() throws Exception {
    final JsonReader reader = newReader("{");
    reader.startObject();

    assertThrows(IllegalStateException.class, reader::readBoolean);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testReadBoolean_ObjectCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::readBoolean);
  }

  // transition from DONE
  @Test
  void testReadBoolean_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0 ");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::readBoolean);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testReadBoolean_ArrayFirstValue() throws Exception {
    final JsonReader reader = newReader("[true]");
    reader.startArray();

    assertTrue(reader.readBoolean());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testReadBoolean_ArraySubsequentWithoutPeek() throws Exception {
    final JsonReader reader = newReader("[0,false]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertFalse(reader.readBoolean());
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testReadBoolean_ArrayValueAfterPeek() throws Exception {
    final JsonReader reader = newReader("[0,true]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.BOOLEAN);

    assertTrue(reader.readBoolean());
  }

  @ParameterizedTest
  @ValueSource(strings = {"tru", "tru \r\t\n", "fals", "fals \r\t\n", "x", " x"})
  void testReadBoolean_Malformed_Throws(final String json) {
    final JsonReader reader = newReader(json);

    assertThrows(JsonParseException.class, reader::readBoolean);
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testReadNull_ObjectField_Throws() throws Exception {
    final JsonReader reader = newReader("{");
    reader.startObject();

    assertThrows(IllegalStateException.class, reader::readNull);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testReadNull_ObjectCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::readNull);
  }

  // transition from DONE
  @Test
  void testReadNull_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0 ");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::readNull);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testReadNull_ArrayFirstValue() throws Exception {
    final JsonReader reader = newReader("[null]");
    reader.startArray();

    reader.readNull();
    reader.endArray();
    assertTrue(reader.isEof());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testReadNull_ArraySubsequentWithoutPeek() throws Exception {
    final JsonReader reader = newReader("[0,null]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    reader.readNull();
    reader.endArray();
    assertTrue(reader.isEof());
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testReadNull_ArrayValueAfterPeek() throws Exception {
    final JsonReader reader = newReader("[0,null]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.NULL);

    reader.readNull();
    reader.endArray();
    assertTrue(reader.isEof());
  }

  @ParameterizedTest
  @ValueSource(strings = {"nul", "nul \r\t\n", "x", " x"})
  void testReadNull_Malformed_Throws(final String json) {
    final JsonReader reader = newReader(json);

    assertThrows(JsonParseException.class, reader::readNull);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testStartObject_ObjectCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::startObject);
  }

  // transition from TOP_LEVEL when EOF
  @ParameterizedTest
  @ValueSource(strings = {"", " \r\t\n"})
  void testStartObject_Eof_Throws(final String json) {
    final JsonReader reader = newReader(json);

    assertThrows(JsonParseException.class, reader::startObject);
  }

  // transition from DONE
  @Test
  void testEndObject_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0 ");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::endObject);
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testEndObject_ObjectValue_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertThrows(IllegalStateException.class, reader::endObject);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testEndObject_ArrayFirstValueOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("[");
    reader.startArray();

    assertThrows(IllegalStateException.class, reader::endObject);
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testEndObject_ArrayCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("[0]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::endObject);
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testEndObject_ArrayValueAfterPeek_Throws() throws Exception {
    final JsonReader reader = newReader("[0,\"value\"]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.STRING);

    assertThrows(IllegalStateException.class, reader::endObject);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testStartArray_ObjectCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::startArray);
  }

  // transition from TOP_LEVEL when EOF
  @ParameterizedTest
  @ValueSource(strings = {"", " \r\t\n"})
  void testStartArray_Eof_Throws(final String json) {
    final JsonReader reader = newReader(json);

    assertThrows(JsonParseException.class, reader::startArray);
  }

  // transition from DONE
  @Test
  void testEndArray_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0 ");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::endArray);
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testEndArray_ObjectField_Throws() throws Exception {
    final JsonReader reader = newReader("{");
    reader.startObject();

    assertThrows(IllegalStateException.class, reader::endArray);
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testEndArray_ObjectValue_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertThrows(IllegalStateException.class, reader::endArray);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testEndArray_ObjectCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::endArray);
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testEndArray_ArrayValueAfterPeek_Throws() throws Exception {
    final JsonReader reader = newReader("[0,\"value\"]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.STRING);

    assertThrows(IllegalStateException.class, reader::endArray);
  }

  // transition from OBJECT_EXPECT_VALUE
  @ParameterizedTest
  @ValueSource(strings = {"{\"field_name\":", "{\"field_name\": \r\t\n"})
  void testPeek_ObjectValueWhenEof_Throws(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertThrows(JsonParseException.class, reader::peek);
  }

  // transition from DONE
  @Test
  void testPeek_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0 ");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::peek);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testPeek_ObjectCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::peek);
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testPeek_ObjectValue() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":true}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertEquals(JsonReader.Token.BOOLEAN, reader.peek());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testPeek_ArrayFirstValue() throws Exception {
    final JsonReader reader = newReader("[true]");
    reader.startArray();

    assertEquals(JsonReader.Token.BOOLEAN, reader.peek());
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testPeek_ArrayValueAfterPeek() throws Exception {
    final JsonReader reader = newReader("[0,\"value\"]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.STRING);

    assertEquals(JsonReader.Token.STRING, reader.peek());
  }

  // transition from TOP_LEVEL when EOF
  @ParameterizedTest
  @ValueSource(strings = {"", " \r\t\n"})
  void testPeek_TopLevelWhenEof_Throws(final String json) {
    final JsonReader reader = newReader(json);

    assertThrows(JsonParseException.class, reader::peek);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END when end
  @Test
  void testPeek_ArrayEnd_Throws() throws Exception {
    final JsonReader reader = newReader("[]");
    reader.startArray();

    assertThrows(IllegalStateException.class, reader::peek);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END when EOF
  @ParameterizedTest
  @ValueSource(strings = {"[", "[ \r\t\n"})
  void testPeek_ArrayFirstWhenEof_Throws(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startArray();

    assertThrows(JsonParseException.class, reader::peek);
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END when end
  @Test
  void testPeek_ArraySubsequentEnd_Throws() throws Exception {
    final JsonReader reader = newReader("[0]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::peek);
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END when EOF
  @ParameterizedTest
  @ValueSource(strings = {"[0", "[0 \r\t\n"})
  void testPeek_ArraySubsequentWhenEof_Throws(final String json) throws Exception {
    final JsonReader reader = newReader(json);
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(JsonParseException.class, reader::peek);
  }

  // transition from DONE
  @Test
  void testHasNext_Done_Throws() throws Exception {
    final JsonReader reader = newReader("0 ");
    reader.readInteger();

    assertThrows(IllegalStateException.class, reader::hasNext);
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testHasNext_ObjectValue_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertThrows(IllegalStateException.class, reader::hasNext);
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testHasNext_ArrayValueAfterPeek_Throws() throws Exception {
    final JsonReader reader = newReader("[0,\"value\"]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.STRING);

    assertThrows(IllegalStateException.class, reader::hasNext);
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testIsEof_ObjectField_Throws() throws Exception {
    final JsonReader reader = newReader("{");
    reader.startObject();

    assertThrows(IllegalStateException.class, reader::isEof);
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testIsEof_ObjectValue_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));

    assertThrows(IllegalStateException.class, reader::isEof);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testIsEof_ObjectCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("{\"field_name\":0}");
    reader.startObject();
    assumeTrue("field_name".equals(reader.readField()));
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::isEof);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testIsEof_ArrayFirstValueOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("[");
    reader.startArray();

    assertThrows(IllegalStateException.class, reader::isEof);
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testIsEof_ArrayCommaOrEnd_Throws() throws Exception {
    final JsonReader reader = newReader("[0]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);

    assertThrows(IllegalStateException.class, reader::isEof);
  }

  // transition from ARRAY_EXPECT_VALUE
  @Test
  void testIsEof_ArrayValueAfterPeek_Throws() throws Exception {
    final JsonReader reader = newReader("[0,\"value\"]");
    reader.startArray();
    assumeTrue(reader.readInteger().intValueExact() == 0);
    assumeTrue(reader.peek() == JsonReader.Token.STRING);

    assertThrows(IllegalStateException.class, reader::isEof);
  }

  @Test
  void testClose_ClosesReader() throws Exception {
    class TestReader extends Reader {
      boolean isClosed = false;

      @Override
      public int read(char[] buf, int off, int len) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void close() {
        isClosed = true;
      }
    }
    
    final TestReader testReader = new TestReader();
    final JsonReader jsonReader = new JsonReader(testReader);

    jsonReader.close();

    assertTrue(testReader.isClosed);
  }
}
