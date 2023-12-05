package me.sparky983.json;

public final class JsonParseException extends JsonException {
  public JsonParseException() {
  }

  public JsonParseException(final String message) {
    super(message);
  }

  public JsonParseException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public JsonParseException(final Throwable cause) {
    super(cause);
  }
}
