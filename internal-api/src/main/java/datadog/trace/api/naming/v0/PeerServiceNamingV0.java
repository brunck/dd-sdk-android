package datadog.trace.api.naming.v0;

import datadog.trace.api.naming.NamingSchema;
import java.util.Collections;
import java.util.Map;
import androidx.annotation.NonNull;

public class PeerServiceNamingV0 implements NamingSchema.ForPeerService {
  @Override
  public boolean supports() {
    return false;
  }

  @NonNull
  @Override
  public Map<String, Object> tags(@NonNull final Map<String, Object> unsafeTags) {
    return Collections.emptyMap();
  }
}
