package me.sparky983.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link JsonWriter}.
 *
 * <p>These tests specify, exhaustively, the behaviour of JsonWriter.
 */
class JsonWriterTest {
  static List<Arguments> provideStrings() {
    return List.of(
        Arguments.of("\"", "\"\\\"\""),
        Arguments.of("\\", "\"\\\\\""),
        Arguments.of("\b", "\"\\b\""),
        Arguments.of("\t", "\"\\t\""),
        Arguments.of("\n", "\"\\n\""),
        Arguments.of("\f", "\"\\f\""),
        Arguments.of("\r", "\"\\r\""),
        Arguments.of("\0", "\"\\u0000\""),
        Arguments.of("\u0001", "\"\\u0001\""),
        Arguments.of("\u0002", "\"\\u0002\""),
        Arguments.of("\u0003", "\"\\u0003\""),
        Arguments.of("\u0004", "\"\\u0004\""),
        Arguments.of("\u0005", "\"\\u0005\""),
        Arguments.of("\u0006", "\"\\u0006\""),
        Arguments.of("\u0007", "\"\\u0007\""),
        Arguments.of("\u0008", "\"\\b\""),
        Arguments.of("\u0019", "\"\\u0019\""),
        Arguments.of("\u001a", "\"\\u001a\""),
        Arguments.of("\u000b", "\"\\u000b\""),
        Arguments.of("\u000c", "\"\\f\""),
        Arguments.of("\u001d", "\"\\u001d\""),
        Arguments.of("\u000e", "\"\\u000e\""),
        Arguments.of("\u000f", "\"\\u000f\""),
        Arguments.of(" ", "\" \""),
        Arguments.of(
            "\r\t\n\u0001 abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "\"\\r\\t\\n\\u0001 abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\""),
        Arguments.of("", "\"\""));
  }

  // transition from TOP_LEVEL
  @Test
  void testWriteField_TopLevel_Throws() {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);

    assertThrows(IllegalStateException.class, () -> writer.writeField("field_name"));
  }

  // transition from DONE
  @Test
  void testWriteField_Done_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeInteger(BigInteger.ZERO);

    assertThrows(IllegalStateException.class, () -> writer.writeField("field_name"));
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testWriteField_ObjectValue_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");

    assertThrows(IllegalStateException.class, () -> writer.writeField("field_name_2"));
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testWriteField_ArrayFirstValue_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();

    assertThrows(IllegalStateException.class, () -> writer.writeField("field_name"));
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testWriteField_ArraySubsequent_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeNull();

    assertThrows(IllegalStateException.class, () -> writer.writeField("field_name"));
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testWriteField_FirstInObject() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeString("value");
    writer.endObject();

    assertEquals("{\"field_name\":\"value\"}", output.toString());
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testWriteField_SubsequentInObject() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name_1");
    writer.writeInteger(BigInteger.ZERO);
    writer.writeField("field_name_2");
    writer.writeString("value");
    writer.endObject();

    assertEquals("{\"field_name_1\":0,\"field_name_2\":\"value\"}", output.toString());
  }

  @ParameterizedTest
  @MethodSource("provideStrings")
  void testWriteField_EscapeSequence(final String field, final String expected) throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField(field);
    writer.writeNull();
    writer.endObject();

    assertEquals("{" + expected + ":null}", output.toString());
  }

  @Test
  void testWriteField_Indented() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, "  ");
    writer.startObject();
    writer.writeField("field_name_1");
    writer.writeInteger(BigInteger.ZERO);
    writer.writeField("field_name_2");
    writer.writeString("value");
    writer.endObject();

    assertEquals(
        """
        {
          "field_name_1": 0,
          "field_name_2": "value"
        }""",
        output.toString());
  }

  // transition from TOP_LEVEL
  @Test
  void testWriteString_TopLevel() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeString("value");

    assertEquals("\"value\"", output.toString());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testWriteString_ObjectValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeString("value");
    writer.endObject();

    assertEquals("{\"field_name\":\"value\"}", output.toString());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testWriteString_ArrayFirstValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeString("value");
    writer.endArray();

    assertEquals("[\"value\"]", output.toString());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testWriteString_ArraySubsequent() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeInteger(BigInteger.ZERO);
    writer.writeString("value");
    writer.endArray();

    assertEquals("[0,\"value\"]", output.toString());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testWriteString_ObjectField_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();

    assertThrows(IllegalStateException.class, () -> writer.writeString("value"));
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testWriteString_ObjectCommaOrEnd_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeNull();

    assertThrows(IllegalStateException.class, () -> writer.writeString("value"));
  }

  // transition from DONE
  @Test
  void testWriteString_Done_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeNull();

    assertThrows(IllegalStateException.class, () -> writer.writeString("value"));
  }

  @ParameterizedTest
  @MethodSource("provideStrings")
  void testWriteString_EscapeSequence(final String string, final String expected) throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeString(string);

    assertEquals(expected, output.toString());
  }

  // transition from TOP_LEVEL
  @ParameterizedTest
  @CsvSource({
      "0, 0",
      "-1, -1",
      "12, 12"
  })
  void testWriteInteger_TopLevel(final String value, final String expected) throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeInteger(new BigInteger(value));

    assertEquals(expected, output.toString());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testWriteInteger_ObjectValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeInteger(new BigInteger("-1"));
    writer.endObject();

    assertEquals("{\"field_name\":-1}", output.toString());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testWriteInteger_ArrayFirstValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeInteger(new BigInteger("12"));
    writer.endArray();

    assertEquals("[12]", output.toString());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testWriteInteger_ArraySubsequent() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeInteger(BigInteger.ZERO);
    writer.writeInteger(new BigInteger("12"));
    writer.endArray();

    assertEquals("[0,12]", output.toString());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testWriteInteger_ObjectField_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();

    assertThrows(IllegalStateException.class, () -> writer.writeInteger(BigInteger.ZERO));
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testWriteInteger_ObjectCommaOrEnd_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeNull();

    assertThrows(IllegalStateException.class, () -> writer.writeInteger(BigInteger.ZERO));
  }

  // transition from DONE
  @Test
  void testWriteInteger_Done_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeNull();

    assertThrows(IllegalStateException.class, () -> writer.writeInteger(BigInteger.ZERO));
  }

  // transition from TOP_LEVEL
  @ParameterizedTest
  @CsvSource({
      "12.5, 12.5",
      "12, 12.0",
      "'1E+2', '1.0E+2'"
  })
  void testWriteDecimal_TopLevel(final String value, final String expected) throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeDecimal(new BigDecimal(value));

    assertEquals(expected, output.toString());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testWriteDecimal_ObjectValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeDecimal(new BigDecimal("1E+2"));
    writer.endObject();

    assertEquals("{\"field_name\":1.0E+2}", output.toString());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testWriteDecimal_ArrayFirstValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeDecimal(new BigDecimal("12"));
    writer.endArray();

    assertEquals("[12.0]", output.toString());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testWriteDecimal_ArraySubsequent() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeInteger(BigInteger.ZERO);
    writer.writeDecimal(new BigDecimal("12.5"));
    writer.endArray();

    assertEquals("[0,12.5]", output.toString());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testWriteDecimal_ObjectField_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();

    assertThrows(IllegalStateException.class, () -> writer.writeDecimal(BigDecimal.ONE));
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testWriteDecimal_ObjectCommaOrEnd_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeNull();

    assertThrows(IllegalStateException.class, () -> writer.writeDecimal(BigDecimal.ONE));
  }

  // transition from DONE
  @Test
  void testWriteDecimal_Done_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeNull();

    assertThrows(IllegalStateException.class, () -> writer.writeDecimal(BigDecimal.ONE));
  }

  // transition from TOP_LEVEL
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testWriteBool_TopLevel(final boolean value) throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeBool(value ? Json.Bool.TRUE : Json.Bool.FALSE);

    assertEquals(Boolean.toString(value), output.toString());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testWriteBool_ObjectValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeBool(Json.Bool.FALSE);
    writer.endObject();

    assertEquals("{\"field_name\":false}", output.toString());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testWriteBool_ArrayFirstValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeBool(Json.Bool.TRUE);
    writer.endArray();

    assertEquals("[true]", output.toString());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testWriteBool_ArraySubsequent() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeInteger(BigInteger.ZERO);
    writer.writeBool(Json.Bool.FALSE);
    writer.endArray();

    assertEquals("[0,false]", output.toString());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testWriteBool_ObjectField_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();

    assertThrows(IllegalStateException.class, () -> writer.writeBool(Json.Bool.TRUE));
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testWriteBool_ObjectCommaOrEnd_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeNull();

    assertThrows(IllegalStateException.class, () -> writer.writeBool(Json.Bool.TRUE));
  }

  // transition from DONE
  @Test
  void testWriteBool_Done_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeNull();

    assertThrows(IllegalStateException.class, () -> writer.writeBool(Json.Bool.TRUE));
  }

  // transition from TOP_LEVEL
  @Test
  void testWriteNull_TopLevel() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeNull();

    assertEquals("null", output.toString());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testWriteNull_ObjectValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeNull();
    writer.endObject();

    assertEquals("{\"field_name\":null}", output.toString());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testWriteNull_ArrayFirstValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeNull();
    writer.endArray();

    assertEquals("[null]", output.toString());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testWriteNull_ArraySubsequent() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeInteger(BigInteger.ZERO);
    writer.writeNull();
    writer.endArray();

    assertEquals("[0,null]", output.toString());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testWriteNull_ObjectField_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();

    assertThrows(IllegalStateException.class, writer::writeNull);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testWriteNull_ObjectCommaOrEnd_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeNull();

    assertThrows(IllegalStateException.class, writer::writeNull);
  }

  // transition from DONE
  @Test
  void testWriteNull_Done_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeNull();

    assertThrows(IllegalStateException.class, writer::writeNull);
  }

  // transition from TOP_LEVEL
  @Test
  void testStartObject_TopLevel() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.endObject();

    assertEquals("{}", output.toString());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testStartObject_ObjectValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.startObject();
    writer.endObject();
    writer.endObject();

    assertEquals("{\"field_name\":{}}", output.toString());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testStartObject_ArrayFirstValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.startObject();
    writer.endObject();
    writer.endArray();

    assertEquals("[{}]", output.toString());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testStartObject_ArraySubsequent() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeInteger(BigInteger.ZERO);
    writer.startObject();
    writer.endObject();
    writer.endArray();

    assertEquals("[0,{}]", output.toString());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testStartObject_ObjectField_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();

    assertThrows(IllegalStateException.class, writer::startObject);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testStartObject_ObjectCommaOrEnd_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeNull();

    assertThrows(IllegalStateException.class, writer::startObject);
  }

  // transition from DONE
  @Test
  void testStartObject_Done_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeNull();

    assertThrows(IllegalStateException.class, writer::startObject);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 7, 8, 16})
  void testStartObject_NestingBeyondInitialStackSize(final int depth) throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    for (int i = 0; i < depth; i++) {
      writer.startArray();
    }
    writer.startObject();
    writer.endObject();
    for (int i = 0; i < depth; i++) {
      writer.endArray();
    }

    assertEquals("[".repeat(depth) + "{}" + "]".repeat(depth), output.toString());
  }

  @Test
  void testStartObject_Indented() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, "  ");
    writer.startArray();
    writer.startObject();
    writer.writeField("field_name");
    writer.startObject();
    writer.writeField("field_name_2");
    writer.writeNull();
    writer.endObject();
    writer.endObject();
    writer.endArray();

    assertEquals(
        """
        [
          {
            "field_name": {
              "field_name_2": null
            }
          }
        ]""",
        output.toString());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testEndObject_Empty() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.endObject();

    assertEquals("{}", output.toString());
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testEndObject_NonEmpty() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeNull();
    writer.endObject();

    assertEquals("{\"field_name\":null}", output.toString());
  }

  // transition from TOP_LEVEL
  @Test
  void testEndObject_TopLevel_Throws() {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);

    assertThrows(IllegalStateException.class, writer::endObject);
  }

  // transition from DONE
  @Test
  void testEndObject_Done_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeNull();

    assertThrows(IllegalStateException.class, writer::endObject);
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testEndObject_ObjectValue_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");

    assertThrows(IllegalStateException.class, writer::endObject);
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testEndObject_ArrayFirstValue_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();

    assertThrows(IllegalStateException.class, writer::endObject);
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testEndObject_ArraySubsequent_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeNull();

    assertThrows(IllegalStateException.class, writer::endObject);
  }

  // transition from TOP_LEVEL
  @Test
  void testStartArray_TopLevel() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.endArray();

    assertEquals("[]", output.toString());
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testStartArray_ObjectValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.startArray();
    writer.endArray();
    writer.endObject();

    assertEquals("{\"field_name\":[]}", output.toString());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testStartArray_ArrayFirstValue() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.startArray();
    writer.endArray();
    writer.endArray();

    assertEquals("[[]]", output.toString());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testStartArray_ArraySubsequent() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeInteger(BigInteger.ZERO);
    writer.startArray();
    writer.endArray();
    writer.endArray();

    assertEquals("[0,[]]", output.toString());
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testStartArray_ObjectField_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();

    assertThrows(IllegalStateException.class, writer::startArray);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testStartArray_ObjectCommaOrEnd_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeNull();

    assertThrows(IllegalStateException.class, writer::startArray);
  }

  // transition from DONE
  @Test
  void testStartArray_Done_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeNull();

    assertThrows(IllegalStateException.class, writer::startArray);
  }

  @Test
  void testStartArray_Indented() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, "  ");
    writer.startArray();
    writer.startArray();
    writer.writeNull();
    writer.endArray();
    writer.endArray();

    assertEquals(
        """
        [
          [
            null
          ]
        ]""",
        output.toString());
  }

  // transition from ARRAY_EXPECT_VALUE_OR_END
  @Test
  void testEndArray_Empty() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.endArray();

    assertEquals("[]", output.toString());
  }

  // transition from ARRAY_EXPECT_COMMA_OR_END
  @Test
  void testEndArray_NonEmpty() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startArray();
    writer.writeNull();
    writer.endArray();

    assertEquals("[null]", output.toString());
  }

  // transition from TOP_LEVEL
  @Test
  void testEndArray_TopLevel_Throws() {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);

    assertThrows(IllegalStateException.class, writer::endArray);
  }

  // transition from DONE
  @Test
  void testEndArray_Done_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.writeNull();

    assertThrows(IllegalStateException.class, writer::endArray);
  }

  // transition from OBJECT_EXPECT_FIELD_OR_END
  @Test
  void testEndArray_ObjectField_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();

    assertThrows(IllegalStateException.class, writer::endArray);
  }

  // transition from OBJECT_EXPECT_VALUE
  @Test
  void testEndArray_ObjectValue_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");

    assertThrows(IllegalStateException.class, writer::endArray);
  }

  // transition from OBJECT_EXPECT_COMMA_OR_END
  @Test
  void testEndArray_ObjectCommaOrEnd_Throws() throws Exception {
    final StringWriter output = new StringWriter();
    final JsonWriter writer = new JsonWriter(output, null);
    writer.startObject();
    writer.writeField("field_name");
    writer.writeNull();

    assertThrows(IllegalStateException.class, writer::endArray);
  }

  @Test
  void testClose_ClosesWriter() throws Exception {
    class TestWriter extends Writer {
      boolean isClosed = false;

      @Override
      public void write(final char[] cbuf, final int off, final int len) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void flush() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void close() {
        isClosed = true;
      }
    }

    final TestWriter testWriter = new TestWriter();
    final JsonWriter jsonWriter = new JsonWriter(testWriter, null);
    jsonWriter.close();

    assertTrue(testWriter.isClosed);
  }
}
