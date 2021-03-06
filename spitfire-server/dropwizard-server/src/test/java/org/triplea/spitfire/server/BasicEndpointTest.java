package org.triplea.spitfire.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.extension.ExtendWith;

/** Test to verify endpoints that are publicly accessible and do not require an API key. */
@AllArgsConstructor
@ExtendWith(SpitfireServerTestExtension.class)
public abstract class BasicEndpointTest<T> {
  private final URI localhost;
  private final Function<URI, T> clientBuilder;

  /** Use this to verify an endpoint that returns void. */
  protected void verifyEndpoint(final Consumer<T> methodRunner) {
    methodRunner.accept(clientBuilder.apply(localhost));
  }

  /** Use this to verify an endpoint that returns data. */
  protected <X> X verifyEndpointReturningObject(final Function<T, X> methodRunner) {
    final X result = methodRunner.apply(clientBuilder.apply(localhost));
    assertThat(result, notNullValue());
    return result;
  }
}
