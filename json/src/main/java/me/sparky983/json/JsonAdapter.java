package me.sparky983.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

final class JsonAdapter {
  static @Nullable Json read(final JsonReader reader) throws JsonParseException, IOException {
    final Json json = readJson(reader);
    if (!reader.isEof()) {
      throw new JsonParseException("Expected EOF");
    }
    return json;
  }

  private static @Nullable Json readJson(final JsonReader reader) throws JsonParseException, IOException {
    return switch (reader.peek()) {
      case OBJECT ->  {
        reader.startObject();
        final Map<String, @Nullable Json> members = new LinkedHashMap<>();
        while (reader.hasNext()) {
          final String field = reader.readField();
          // The spec does not require that members be unique, however, we have added this
          //  requirement to avoid the API being too confusing. I don't think there is any real use
          //  case for duplicate members anyway.
          // containsKey is used rather than checking the result of put since values may be null
          if (members.containsKey(field)) {
            throw new JsonParseException("Duplicate member \"" + field + "\"");
          }
          members.put(field, readJson(reader));
        }
        reader.endObject();
        yield Json.object(new InternalUnmodifiableMap(members));
      }
      case ARRAY -> {
        reader.startArray();
        final List<@Nullable Json> elements = new ArrayList<>();
        while (reader.hasNext()) {
          elements.add(readJson(reader));
        }
        reader.endArray();
        yield Json.array(new InternalUnmodifiableList(elements));
      }
      case STRING -> Json.string(reader.readString());
      case BOOLEAN -> Json.bool(reader.readBoolean());
      case NUMBER -> {
        final String number = reader.readNumber();
        if (number.indexOf('.') != -1 || number.indexOf('E') != -1 || number.indexOf('e') != -1) {
          yield Json.decimal(new BigDecimal(number));
        }
        yield Json.integer(new BigInteger(number));
      }
      case NULL -> {
        reader.readNull();
        yield null;
      }
    };
  }

  static void write(final JsonWriter writer, final @Nullable Json json) throws IOException {
    switch (json) {
      case Json.Object(final Map<String, @Nullable Json> members) -> {
        writer.startObject();
        for (final Map.Entry<String, Json> member : members.entrySet()) {
          final String key = member.getKey();
          final Json value = member.getValue();
          writer.writeField(key);
          write(writer, value);
        }
        writer.endObject();
      }
      case Json.Array(final List<@Nullable Json> elements) -> {
        writer.startArray();
        for (int i = 0; i < elements.size(); i++) {
          write(writer, elements.get(i));
        }
        writer.endArray();
      }
      case Json.Integer(final BigInteger value) -> writer.writeInteger(value);
      case Json.Decimal(final BigDecimal value) -> writer.writeDecimal(value);
      case Json.String(final String value) -> writer.writeString(value);
      case Json.Bool bool -> writer.writeBool(bool);
      case null -> writer.writeNull();
    }
  }
}
