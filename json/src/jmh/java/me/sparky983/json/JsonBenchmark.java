package me.sparky983.json;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class JsonBenchmark {
  static final Gson GSON = new Gson();
  static final String JSON;

  static {
    try (final InputStream json = JsonBenchmark.class.getResourceAsStream("/twitter.json")) {
        JSON = new String(Objects.requireNonNull(json, "twitter.json").readAllBytes());
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
  }

  @Benchmark
  public void jsonRead(Blackhole blackhole) throws JsonParseException {
    blackhole.consume(Json.read(JSON));
  }

   @Benchmark
  public void gsonFromJsonCheap(Blackhole blackhole) {
    blackhole.consume(GSON.fromJson(new CheapStringReader(JSON), Object.class));
  }
}
