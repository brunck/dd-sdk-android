package datadog.trace.api.iast.sink;

import datadog.trace.api.iast.IastModule;
import androidx.annotation.Nullable;

public interface XPathInjectionModule extends IastModule {
  void onExpression(@Nullable final String expression);
}
