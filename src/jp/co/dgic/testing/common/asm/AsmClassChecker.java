/**
 * Copyright (C)2004 dGIC Corporation.
 *
 * This file is part of djUnit plugin.
 *
 * djUnit plugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * djUnit plugin is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with djUnit plugin; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 *
 */
package jp.co.dgic.testing.common.asm;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import jp.co.dgic.testing.common.util.DJUnitUtil;

public class AsmClassChecker extends ClassVisitor {
  public static final String JUNIT_TEST_INTERFACENAME = "junit.framework.Test";

  // org.junit.runner, org.junit.jupiter.api.extension;
  public static final String JUNIT_TESTRUNNER_ANNOTATION = "org/junit/";

  /** A map to store instances of AsmClassChecker for different classes. */
  private static final HashMap<String, AsmClassChecker> CLASS_CHECKERS = new HashMap<String, AsmClassChecker>();

  private boolean isInterface = false;
  private boolean isAnnotation = false;
  private boolean isEnum = false;
  private boolean hasJUnitTestAnnotation = false;
  private String[] interfaces;
  private String className;
  private String superClassName;
  private AsmClassChecker superClassChecker;
  private HashMap<String, String[]> exceptionsMap = new HashMap<String, String[]>();
  private Set<String> methodNames = new HashSet<String>();
  private HashMap<String, Integer> maxLocals = new HashMap<String, Integer>();

  private AsmClassChecker() {
    super(DJUnitUtil.ASM_API_VERSION);
  }

  /**
   * Creates a new instance of AsmClassChecker for a class being loaded by a Java agent.
   */
  public static AsmClassChecker createInstance(String className, AsmClassReader reader) {
    AsmClassChecker classChecker = new AsmClassChecker();
    reader.accept(classChecker);
    CLASS_CHECKERS.put(className, classChecker);
    return classChecker;
  }

  /**
   * Gets an instance of AsmClassChecker to retrieve class information.
   */
  public static AsmClassChecker getInstance(String className, AsmClassReader reader) {
    AsmClassChecker classChecker = CLASS_CHECKERS.get(className);
    if (classChecker == null) {
      if (reader == null) {
        try {
          reader = AsmClassReader.getInstance(className, null);
        } catch (IOException e) {
          return null;
        }
      }

      classChecker = new AsmClassChecker();
      reader.accept(classChecker);
      CLASS_CHECKERS.put(className, classChecker);
    }

    return classChecker;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.className = DJUnitUtil.getQualifiedName(name);
    this.superClassName = superName;
    this.interfaces = interfaces;

    isInterface = (access & Opcodes.ACC_INTERFACE) > 0;
    isAnnotation = (access & Opcodes.ACC_ANNOTATION) > 0;
    isEnum = (access & Opcodes.ACC_ENUM) > 0;

    if (isInterface && interfaces != null && interfaces.length > 0) {
      superClassName = interfaces[0];
    }

    if (superClassName != null) {
      superClassName = DJUnitUtil.getQualifiedName(superClassName);
    }

    DJUnitUtil.debug("[AsmClassChecker][visit]: " + className + " is " + (isInterface ? "<INTERFACE>" : "<CLASS>"));

    collectSuperClassInfo();
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    String key = name + descriptor;
    putExceptions(key, exceptions);
    putMethodName(key);
    return new AsmMethodChecker(this, name, descriptor);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    DJUnitUtil.trace("[AsmClassChecker][visitAnnotation] : " + className + ", " + descriptor);

    if (descriptor != null && descriptor.indexOf(JUNIT_TESTRUNNER_ANNOTATION) >= 0) {
      this.setJUnitTestAnnotation(true);
    }

    return super.visitAnnotation(descriptor, visible);
  }

  public void setJUnitTestAnnotation(boolean hasAnnotation) {
    hasJUnitTestAnnotation = hasAnnotation;
  }

  public boolean isEnum() {
    return isEnum;
  }

  public boolean isInterface() {
    return isInterface;
  }

  public boolean isAnnotation() {
    return isAnnotation;
  }

  public boolean isTestCase() {
    if (hasJUnitTestAnnotation) {
      return true;
    }

    boolean isTestCase = hasTestInterface(interfaces);
    if (isTestCase || superClassChecker == null) {
      return isTestCase;
    }

    return superClassChecker.isTestCase();
  }

  private boolean hasTestInterface(String[] interfaces) {
    if (interfaces == null)
      return false;
    for (int i = 0; i < interfaces.length; i++) {
      if (JUNIT_TEST_INTERFACENAME.equals(DJUnitUtil.getQualifiedName(interfaces[i])))
        return true;
    }
    return false;
  }

  private void collectSuperClassInfo() {
    if ("java.lang.Object".equals(className)) {
      return;
    }

    if (superClassName == null) {
      return;
    }

    try {
      AsmClassReader reader = AsmClassReader.getInstance(superClassName, null);
      superClassChecker = AsmClassChecker.getInstance(superClassName, reader);
      DJUnitUtil.trace("[AsmClassChecker][collectSuperClassInfo] : " + reader.getClassName());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String[] getSuperClassNames() {
    Set<String> names = getAllSuperClassNames();
    return names.toArray(new String[names.size()]);
  }

  private Set<String> getAllSuperClassNames() {
    Set<String> names = null;
    if (superClassChecker == null) {
      names = new HashSet<String>();
      if (superClassName != null) {
        names.add(superClassName);
      }
    } else {
      names = superClassChecker.getAllSuperClassNames();
    }

    names.add(className);

    return names;
  }
  
  public String getOwnerName(String methodNameAndDesc, String oldOwner) {
    if (methodNames.contains(methodNameAndDesc)) return className;
    if (superClassChecker == null) return oldOwner;
    
    // version 0.8.4
    // If this class is not an own source, stop searching for a information of superclass.
    if (!isOwnSource(className)) return className;

    return superClassChecker.getOwnerName(methodNameAndDesc, oldOwner);
  }
  
  public String[] getExceptions(String methodName, String desc) {
    String key = methodName + desc;
    String[] exceptions = getExceptions(key);
    if (exceptions == null)
      return new String[0];
    return exceptions;
  }

  private String[] getExceptions(String methodNameAndDesc) {
    if (exceptionsMap.containsKey(methodNameAndDesc)) {
      return exceptionsMap.get(methodNameAndDesc);
    }

    if (superClassChecker == null)
      return null;

    return superClassChecker.getExceptions(methodNameAndDesc);
  }

  public int getMaxLocals(String methodName, String descriptor) {
    try {
      Integer max = maxLocals.get(methodName + descriptor);
      if (max == null) {
        return -1;
      }

      return max.intValue();
    } catch (Throwable t) {
      // continue
    }

    return -1;
  }

  public void putMaxLocals(String methodName, String desc, int maxLocal) {
    maxLocals.put(methodName + desc, Integer.valueOf(maxLocal));
  }

  private void putExceptions(String methodNameAndDesc, String[] exceptions) {
    if (exceptions == null || exceptions.length == 0) {
      return;
    }

    exceptionsMap.put(methodNameAndDesc, exceptions);
  }

  private void putMethodName(String methodNameAndDesc) {
    methodNames.add(methodNameAndDesc);
  }

  private boolean isOwnSource(String className) {
    String name = DJUnitUtil.getQualifiedName(className);

    if (DJUnitUtil.getIncludeValue() != null && DJUnitUtil.isInclude(name)) {
      return true;
    }

    if (DJUnitUtil.isProjectsSource(name)) {
      return true;
    }

    return false;
  }
}
