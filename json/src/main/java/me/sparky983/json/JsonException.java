package me.sparky983.json;

import org.jspecify.annotations.Nullable;

public sealed abstract class JsonException extends Exception permits JsonParseException {
  protected JsonException() {
  }

  protected JsonException(final @Nullable String message) {
    super(message);
  }

  protected JsonException(final @Nullable String message, final @Nullable Throwable cause) {
    super(message, cause);
  }

  protected JsonException(final @Nullable Throwable cause) {
    super(cause);
  }
}
