package me.sparky983.json;

import org.jspecify.annotations.Nullable;

public final class JsonParseException extends JsonException {
  public JsonParseException() {
  }

  public JsonParseException(final @Nullable String message) {
    super(message);
  }

  public JsonParseException(final @Nullable String message, final @Nullable Throwable cause) {
    super(message, cause);
  }

  public JsonParseException(final @Nullable Throwable cause) {
    super(cause);
  }
}
