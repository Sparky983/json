package me.sparky983.json;

public sealed abstract class JsonException extends Exception permits JsonParseException {
  protected JsonException() {
  }

  protected JsonException(final String message) {
    super(message);
  }

  protected JsonException(final String message, final Throwable cause) {
    super(message, cause);
  }

  protected JsonException(final Throwable cause) {
    super(cause);
  }
}
