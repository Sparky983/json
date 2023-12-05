package me.sparky983.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class JsonWriter implements AutoCloseable {
  // Implementation notes:
  //  Apart from Json.String, the primitive value toString() implementations have small
  //  implementations, so they have been copied into this class. The complex value and Json.String
  //  toString() implementations both depend on this class because the implementations are more
  //  complex so copying would be too annoying to maintain.

  private final Writer writer;
  private final String indentation;

  JsonWriter(final Writer writer, final String indentation) {
    this.writer = writer;
    this.indentation = indentation;
  }

  void writeJson(final Json json) throws IOException {
    writeJson(json, 0);
  }

  private void writeJson(final Json json, int level) throws IOException {
    switch (json) {
      case Json.Object object -> writeObject(object, level);
      case Json.Array array -> writeArray(array, level);
      case Json.String(String string) -> writeString(string);
      case Json.Integer integer -> writeInteger(integer);
      case Json.Decimal decimal -> writeDecimal(decimal);
      case Json.Null jsonNull -> writeNull();
      case Json.True jsonTrue -> writeTrue();
      case Json.False jsonFalse -> writeFalse();
    }
  }

  private void writeObject(final Json.Object object, final int level) throws IOException {
    final Map<String, Json> members = object.members();

    writer.write('{');

    if (indentation != null) {
      writer.write('\n');
    }

    final int membersLevel = level + 1;

    final Iterator<Map.Entry<String, Json>> iterator = members.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<String, Json> member = iterator.next();
      indent(membersLevel);
      writeString(member.getKey());
      writer.write(':');
      if (indentation != null) {
        writer.write(' ');
      }
      writeJson(member.getValue(), membersLevel);
      if (iterator.hasNext()) {
        writer.write(',');
      }
      if (indentation != null) {
        writer.write('\n');
      }
    }

    indent(level);
    writer.write('}');
  }

  private void writeArray(final Json.Array array, final int level) throws IOException {
    final List<Json> elements = array.elements();
    final int size = elements.size();

    writer.write('[');

    if (indentation != null) {
      writer.write('\n');
    }

    final int elementsLevel = level + 1;

    // Avoid the iterator allocation; most List.copyOf implementations will return a random access
    //  list implementation
    for (int i = 0; i < size;) {
      final Json element = elements.get(i);
      indent(elementsLevel);
      writeJson(element, elementsLevel);
      if (++i != size) {
        writer.write(',');
      }
      if (indentation != null) {
        writer.write('\n');
      }
    }

    indent(level);
    writer.write("]");
  }

  private void writeString(final String string) throws IOException {
    writer.write('\"');
    final int length = string.length();
    for (int i = 0; i < length; i++) {
      char c = string.charAt(i);
      switch (c) {
        case '"' -> writer.write("\\");
        case '\\' ->writer.write("\\\\");
        case '\b' -> writer.write("\\b");
        case '\f' -> writer.write("\\f");
        case '\n' -> writer.write("\\n");
        case '\r' -> writer.write("\\r");
        case '\t' -> writer.write("\\t");
        default -> writer.write(c);
      }
    }
    writer.write('\"');
  }

  private void writeInteger(final Json.Integer integer) throws IOException {
    writer.write(integer.value().toString());
  }

  private void writeDecimal(final Json.Decimal decimal) throws IOException {
    final String value = decimal.value().toString();
    writer.write(value);
    if (value.indexOf('.') == -1) {
      writer.write(".0");
    }
  }

  private void writeNull() throws IOException {
    writer.write("null");
  }

  private void writeTrue() throws IOException {
    writer.write("true");
  }

  private void writeFalse() throws IOException {
    writer.write("false");
  }

  private void indent(int amount) throws IOException {
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
}
