package me.sparky983.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class JsonTest {
  static final String JSON_STRING_COMPACT =
      "{"
          + "\"object\": {"
          + "\"integer\": 1,"
          + "\"decimal\": 1.0,"
          + "\"string\": \"a string\","
          + "\"true\": true,"
          + "\"false\": false,"
          + "\"null\": null"
          + "},"
          + "\"array\": ["
          + "1,"
          + "1.0,"
          + "\"string\","
          + "true,"
          + "false,"
          + "null"
          + "],"
          + "\"integer\": 1,"
          + "\"decimal\": 1.0,"
          + "\"string\": \"a string\","
          + "\"true\": true,"
          + "\"false\": false,"
          + "\"null\": null"
          + "}";

  static final Json JSON_OBJECT =
      Json.object()
          .put(
              "object",
              Json.object()
                  .put("integer", Json.integer(1))
                  .put("decimal", Json.decimal(1.0))
                  .put("string", Json.string("a string"))
                  .put("true", Json.TRUE)
                  .put("false", Json.FALSE)
                  .put("null", Json.NULL)
                  .build())
          .put(
              "array",
              Json.array(
                  Json.integer(1),
                  Json.decimal(1.0),
                  Json.string("a string"),
                  Json.TRUE,
                  Json.FALSE,
                  Json.NULL))
          .put("integer", Json.integer(1))
          .put("decimal", Json.decimal(1.0))
          .put("string", Json.string("string"))
          .put("true", Json.TRUE)
          .put("false", Json.FALSE)
          .put("null", Json.NULL)
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
