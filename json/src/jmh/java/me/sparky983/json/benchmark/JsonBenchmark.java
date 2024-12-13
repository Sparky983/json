package me.sparky983.json.benchmark;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import me.sparky983.json.Json;
import me.sparky983.json.JsonParseException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class JsonBenchmark {
  static final Gson GSON = new Gson();
  static final TypeToken<Map> TYPE = TypeToken.get(Map.class);
  static final String JSON;

  static {
    final InputStream json = JsonBenchmark.class.getResourceAsStream("/twitter.json");
    try {
        JSON = new String(Objects.requireNonNull(json, "twitter.json").readAllBytes());
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
  }

  @Benchmark
  public void json(Blackhole blackhole) throws JsonParseException {
    blackhole.consume(Json.read(JSON));
  }

  @Benchmark
  public void gson(Blackhole blackhole) {
    blackhole.consume(GSON.fromJson(JSON, TYPE));
  }
}
