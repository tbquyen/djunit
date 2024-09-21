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
import jp.co.dgic.testing.common.util.VirtualMockUtil;

public class AsmClassChecker extends ClassVisitor {
  private static final String JUNIT_TEST_INTERFACENAME = "junit.framework.Test";

  private static final String JUNIT_TESTRUNNER_ANNOTATION = "org/junit/";

  private String _className;
  private String _superClassName;
  private String[] _interfaces;

  private HashMap<String, Integer> _maxLocals = new HashMap<>();
  private HashMap<String, String[]> _exceptionsMap = new HashMap<>();
  private Set<String> _methodNames = new HashSet<>();

  private boolean isInterface = false;
  private boolean isAnnotation = false;
  private boolean isEnum = false;
  private boolean hasJUnitTestAnnotation = false;

  private AsmClassChecker _superClassChecker;

  private static final HashMap<String, AsmClassChecker> _classCheckers = new HashMap<>();

  private AsmClassChecker() {
    super(VirtualMockUtil.ASM_API_VERSION);
  }

  /**
   * Creates a new instance of AsmClassChecker for a class being loaded by a Java
   * agent.
   */
  public static AsmClassChecker createInstance(String className, AsmClassReader reader) {
    AsmClassChecker classChecker = new AsmClassChecker();
    reader.accept(classChecker);
    _classCheckers.put(className, classChecker);
    return classChecker;
  }

  /**
   * Gets an instance of AsmClassChecker to retrieve class information.
   */
  public static AsmClassChecker getInstance(String className, AsmClassReader reader) {
    AsmClassChecker classChecker = _classCheckers.get(className);
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
      _classCheckers.put(className, classChecker);
    }

    return classChecker;
  }

  public String getClassName() {
    return _className;
  }

  public String getSuperClassName() {
    return _superClassName;
  }

  public AsmClassChecker getSuperClassChecker() {
    return _superClassChecker;
  }

  public void setSuperClassChecker(AsmClassChecker superClassChecker) {
    _superClassChecker = superClassChecker;
  }

  public String[] getInterfaces() {
    return _interfaces;
  }

  public String[] getSuperClassNames() {
    Set<String> names = getAllSuperClassNames();
    return names.toArray(new String[names.size()]);
  }

  public Set<String> getAllSuperClassNames() {
    Set<String> names = null;
    if (_superClassChecker == null) {
      names = new HashSet<>();
      if (_superClassName != null) {
        names.add(_superClassName);
      }
    } else {
      names = _superClassChecker.getAllSuperClassNames();
    }

    names.add(_className);

    return names;
  }

  public boolean isTestCase() {
    if (hasJUnitTestAnnotation)
      return true;
    boolean isTestCase = hasTestInterface(_interfaces);
    if (isTestCase || _superClassChecker == null)
      return isTestCase;
    return _superClassChecker.isTestCase();

  }

  public boolean isInterface() {
    return isInterface;
  }

  public boolean isAnnotation() {
    return isAnnotation;
  }

  public boolean isEnum() {
    return isEnum;
  }

  public void setJUnitTestAnnotation(boolean hasAnnotation) {
    hasJUnitTestAnnotation = hasAnnotation;
  }

  public void putMaxLocals(String methodName, String desc, int maxLocal) {
    _maxLocals.put(methodName + desc, maxLocal);
  }

  public int getMaxLocals(String methodName, String desc) {
    try {
      Integer max = (Integer) _maxLocals.get(methodName + desc);
      if (max == null)
        return -1;
      return max.intValue();
    } catch (Throwable t) {
      // continue
    }
    return -1;
  }

  public void putExceptions(String methodNameAndDesc, String[] exceptions) {
    if (exceptions == null || exceptions.length == 0)
      return;
    _exceptionsMap.put(methodNameAndDesc, exceptions);
  }

  public String[] getExceptions(String methodName, String desc) {
    String key = methodName + desc;
    String[] exceptions = getExceptions(key);
    if (exceptions == null)
      return new String[0];
    return exceptions;
  }

  public String[] getExceptions(String methodNameAndDesc) {
    if (_exceptionsMap.containsKey(methodNameAndDesc)) {
      return (String[]) _exceptionsMap.get(methodNameAndDesc);
    }
    if (_superClassChecker == null)
      return null;

    return _superClassChecker.getExceptions(methodNameAndDesc);
  }

  public void putMethodName(String methodNameAndDesc) {
    _methodNames.add(methodNameAndDesc);
  }

  public String getOwnerName(String methodNameAndDesc, String oldOwner) {
    if (_methodNames.contains(methodNameAndDesc))
      return _className;
    if (_superClassChecker == null)
      return oldOwner;

    // If this class is not an own source, stop searching for a information of
    // superclass.
    if (!isOwnSource(_className))
      return _className;

    return _superClassChecker.getOwnerName(methodNameAndDesc, oldOwner);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    String key = name + desc;
    putExceptions(key, exceptions);
    putMethodName(key);
    return new AsmMethodChecker(this, name, desc);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    _className = name.replace('/', '.');
    _superClassName = superName;
    _interfaces = interfaces;

    isInterface = (access & Opcodes.ACC_INTERFACE) > 0;
    isAnnotation = (access & Opcodes.ACC_ANNOTATION) > 0;
    isEnum = (access & Opcodes.ACC_ENUM) > 0;

    if (isInterface && interfaces != null && interfaces.length > 0) {
      _superClassName = interfaces[0];
    }
    if (_superClassName != null) {
      _superClassName = _superClassName.replace('/', '.');
    }

    DJUnitUtil.debug("[AsmClassChecker] " + _className + ".class is " + (isInterface ? "<INTERFACE>" : "<CLASS>"));

    collectSuperClassInfo();
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    if (desc != null && desc.indexOf(JUNIT_TESTRUNNER_ANNOTATION) >= 0) {
      this.setJUnitTestAnnotation(true);
    }
    return super.visitAnnotation(desc, visible);
  }

  protected void collectSuperClassInfo() {
    if ("java.lang.Object".equals(_className))
      return;
    if (getSuperClassName() == null)
      return;

    this.setSuperClassChecker(AsmClassChecker.getInstance(getSuperClassName(), null));
  }

  protected boolean hasTestInterface(String[] interfaces) {
    if (interfaces == null)
      return false;
    for (int i = 0; i < interfaces.length; i++) {
      if (JUNIT_TEST_INTERFACENAME.equals(interfaces[i].replace('/', '.')))
        return true;
    }
    return false;
  }

  protected boolean isOwnSource(String className) {
    return DJUnitUtil.isOwnSource(className);
  }
}
