package jp.co.dgic.testing.agent;

import java.lang.instrument.Instrumentation;

import jp.co.dgic.testing.common.util.DJUnitUtil;

public class DJunitAgent {
  private static boolean initialized = false;

  public static void premain(String agentArgs, Instrumentation inst) {
    if (!initialized) {
      DJUnitUtil.debug("Agent loaded at JVM startup.");
      inst.addTransformer(new DJunitTransformer(inst), false);
      initialized = true;
    }
  }

  public static void agentmain(String agentArgs, Instrumentation inst) {
    if (!initialized) {
      DJUnitUtil.debug("Agent loaded dynamically.");
      inst.addTransformer(new DJunitTransformer(inst), false);
      initialized = true;
    }
  }
}
