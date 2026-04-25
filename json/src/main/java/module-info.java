import org.jspecify.annotations.NullMarked;

@SuppressWarnings("module") // suppress terminal digits warning
@NullMarked
module me.sparky983.json {
  requires static org.jspecify;
  requires static org.jetbrains.annotations;
  exports me.sparky983.json;
}
