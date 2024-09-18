package jp.co.dgic.testing.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import jp.co.dgic.testing.common.DJUnitClassModifier;
import jp.co.dgic.testing.common.util.DJUnitUtil;

public class DJunitTransformer implements ClassFileTransformer {
  protected DJUnitClassModifier classModifier;
  protected Instrumentation inst;

  public DJunitTransformer(Instrumentation inst) {
    this.inst = inst;
    classModifier = new DJUnitClassModifier();
  }

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
    if (loader == null) {
      DJUnitUtil.trace("Bootstrap ClassLoader: " + className);
      return null;
    } else if (loader.getClass().getName().equals("sun.misc.Launcher$ExtClassLoader")) {
      DJUnitUtil.trace("Extension ClassLoader: " + className);
      return null;
    }

    return classModifier.getModifiedClass(className, classfileBuffer);
  }
}
