package games.strategy.engine.data;

import java.util.Map;
import java.util.Optional;

public interface DynamicallyModifiable {
  Map<String, ModifiableProperty<?>> getPropertyMap();

  default ModifiableProperty<?> getOrError(final String property) {
    return Optional.ofNullable(getPropertyMap().get(property))
        .orElseThrow(() -> new IllegalStateException("Missing property definition for option: " + property));
  }
}
