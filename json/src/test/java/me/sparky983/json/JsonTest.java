package me.sparky983.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JsonTest {
  static final String JSON_STRING_COMPACT =
      "{"
          + "\"object\":{"
          + "\"integer\":1,"
          + "\"decimal\":1.0,"
          + "\"string\":\"a string\","
          + "\"true\":true,"
          + "\"false\":false,"
          + "\"null\":null"
          + "},"
          + "\"array\":["
          + "1,"
          + "1.0,"
          + "\"a string\","
          + "true,"
          + "false,"
          + "null"
          + "],"
          + "\"integer\":1,"
          + "\"decimal\":1.0,"
          + "\"string\":\"a string\","
          + "\"true\":true,"
          + "\"false\":false,"
          + "\"null\":null"
          + "}";

  static final Json JSON_OBJECT =
      Json.object()
          .put(
              "object",
              Json.object()
                  .put("integer", Json.integer(1))
                  .put("decimal", Json.decimal(1.0))
                  .put("string", Json.string("a string"))
                  .put("true", Json.Bool.TRUE)
                  .put("false", Json.Bool.FALSE)
                  .put("null", null)
                  .build())
          .put(
              "array",
              Json.array(
                  Json.integer(1),
                  Json.decimal(1.0),
                  Json.string("a string"),
                  Json.Bool.TRUE,
                  Json.Bool.FALSE,
                  null))
          .put("integer", Json.integer(1))
          .put("decimal", Json.decimal(1.0))
          .put("string", Json.string("a string"))
          .put("true", Json.Bool.TRUE)
          .put("false", Json.Bool.FALSE)
          .put("null", null)
          .build();

  @Test
  void testJsonRead_Reader() throws JsonParseException, IOException {
    final Json json = Json.read(new StringReader(JSON_STRING_COMPACT));

    assertEquals(JSON_OBJECT, json);
  }

  @Test
  void testJsonRead_String() throws JsonParseException {
    final Json json = Json.read(JSON_STRING_COMPACT);

    assertEquals(JSON_OBJECT, json);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{\"a\":1,\"a\":2}",
      "{\"a\":null,\"a\":null}",
      "{\"a\":1,\"b\":2,\"a\":3}",
      "{\"object\":{\"a\":1,\"a\":2}}",
      "[{\"a\":1,\"a\":2}]"
  })
  void testJsonRead_DuplicateMember_Throws(final String json) {
    assertThrows(JsonParseException.class, () -> Json.read(json));
  }

  @ParameterizedTest
  @ValueSource(strings = {"]", "}", "[0,]", "[0,}", "{\"a\":]}", "{\"a\":}", "[{\"a\":]}]"})
  void testJsonRead_UnexpectedEnd_Throws(final String json) {
    assertThrows(JsonParseException.class, () -> Json.read(json));
  }

  @Test
  void testJsonWrite_Writer() throws IOException {
    final StringWriter writer = new StringWriter();

    Json.write(JSON_OBJECT, writer);

    assertEquals(JSON_STRING_COMPACT, writer.toString());
  }

  @Test
  void testJsonWrite_String() {
    final String json = Json.write(JSON_OBJECT);

    assertEquals(JSON_STRING_COMPACT, json);
  }
}
