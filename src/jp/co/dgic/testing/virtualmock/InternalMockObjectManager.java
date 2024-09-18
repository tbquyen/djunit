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
package jp.co.dgic.testing.virtualmock;

import jp.co.dgic.testing.common.DJUnitRuntimeException;
import jp.co.dgic.testing.common.util.DJUnitUtil;

public class InternalMockObjectManager {
  private static ReturnValueList testData = new ReturnValueList();
  private static ArgumentValueList callsMade = new ArgumentValueList();

  public static void initialize() {
    DJUnitUtil.debug("[InternalMockObjectManager]::initialize");
    testData.clear();
    callsMade.clear();
  }

  public static ReturnValueList getTestData() {
    return testData;
  }

  public static ArgumentValueList getCallsMade() {
    return callsMade;
  }

  public static Object getReturnValue(String classAndMethodName) {
    Object value = testData.get(classAndMethodName);
    DJUnitUtil.debug("[InternalMockObjectManager][getReturnValue] : " + classAndMethodName + ", " + value);
    return value;
  }

  public static boolean isIgnoreMethodValue(String classAndMethodName) {
    Object value = testData.get(classAndMethodName, false);
    if (value == null)
      return false;
    return value instanceof IgnoreMethodValue;
  }

  public static void indicateCalled(String className, String methodName, Object[] arguments) {
    indicateCalled(makeKey(className, methodName), arguments);
  }

  public static void indicateCalled(String classAndMethodName, Object[] arguments) {
    DJUnitUtil.debug("[InternalMockObjectManager][indicateCalled] : " + classAndMethodName + ", " + arguments);
    callsMade.put(classAndMethodName, arguments);
  }

  public static Object indicateCalledAndGetReturnValue(String className, String methodName, Object[] arguments)
      throws Throwable {
    return indicateCalledAndGetReturnValue(makeKey(className, methodName), arguments);
  }

  public static Object indicateCalledAndGetReturnValue(String classAndMethodName, Object[] arguments) throws Throwable {
    // register arguments
    indicateCalled(classAndMethodName, arguments);

    // getReturnValue
    Object value = getReturnValue(classAndMethodName);

    if (value == null) {
      return null;
    }

    // check 'IReturnValueProvider' and createReturnValue
    if (value instanceof IReturnValueProvider) {
      Object providedValue = ((IReturnValueProvider) value).createReturnValue(arguments);
      value = (providedValue == null ? new NullReturnValue() : providedValue);
    }

    return value;
  }

  public static Object indicateCalledAndGetReturnValueForNewExpr(String className, String methodName,
      Object[] arguments, boolean isOwnSource) throws Throwable {
    return indicateCalledAndGetReturnValueForNewExpr(makeKey(className, methodName), arguments, isOwnSource);
  }

  public static Object indicateCalledAndGetReturnValueForNewExpr(String classAndMethodName, Object[] arguments,
      boolean isOwnSource) throws Throwable {

    // register arguments
    if (!isOwnSource) {
      indicateCalled(classAndMethodName, arguments);
    }

    // getReturnValue
    Object value = null;
    if (isOwnSource) {
      if (!isIgnoreMethodValue(classAndMethodName)) {
        value = getReturnValue(classAndMethodName);
      }
    } else {
      value = getReturnValue(classAndMethodName);
    }

    if (value == null)
      return null;

    // check 'IReturnValueProvider' and createReturnValue
    if (value instanceof IReturnValueProvider) {
      Object providedValue = ((IReturnValueProvider) value).createReturnValue(arguments);
      value = (providedValue == null ? new NullReturnValue() : providedValue);
    }

    return value;
  }

  public static void throwException(Object value, String className, String methodName) {
    throwException(value, className + "#" + methodName);
  }

  public static void throwException(Object value, String classAndMethodName) {

    if (value instanceof RuntimeException)
      throw (RuntimeException) value;
    if (value instanceof Error)
      throw (Error) value;

    if (value instanceof Throwable) {
      throw new DJUnitRuntimeException(createMismatchExceptionTypeMessage(value, classAndMethodName));
    }

  }

  public static void checkReturnTypeIsIgnoreReturnValue(Object value, String className, String methodName) {
    checkReturnTypeIsIgnoreReturnValue(value, className + "#" + methodName);
  }

  public static void checkReturnTypeIsIgnoreReturnValue(Object value, String classAndMethodName) {

    if (value instanceof IgnoreMethodValue)
      return;

    throw new DJUnitRuntimeException(createMismatchReturnTypeMessage(value, classAndMethodName));
  }

  public static void checkReturnTypeIsIgnoreOrNullReturnValue(Object value, String className, String methodName) {
    checkReturnTypeIsIgnoreOrNullReturnValue(value, className + "#" + methodName);
  }

  public static void checkReturnTypeIsIgnoreOrNullReturnValue(Object value, String classAndMethodName) {

    if (value instanceof IgnoreMethodValue)
      return;
    if (value instanceof NullReturnValue)
      return;

    throw new DJUnitRuntimeException(createMismatchReturnTypeMessage(value, classAndMethodName) + "\nTry: MockObjectManager::addReturnNull()");
  }

  public static void checkReturnTypeIsNullReturnValue(Object value, String className, String methodName) {
    checkReturnTypeIsNullReturnValue(value, className + "#" + methodName);
  }

  public static void checkReturnTypeIsNullReturnValue(Object value, String classAndMethodName) {

    if (value instanceof NullReturnValue)
      return;

    throw new DJUnitRuntimeException(
        createMismatchReturnTypeMessage(value, classAndMethodName) + "\nTry: MockObjectManager::addReturnNull()");
  }

  public static void checkReturnTypeForNewExpr(Object value, String className, String methodName) {
    checkReturnTypeForNewExpr(value, className + "#" + methodName);
  }

  public static void checkReturnTypeForNewExpr(Object value, String classAndMethodName) {
    if (!(value instanceof NullReturnValue)) {
      if (value instanceof IgnoreMethodValue) {
        // for system class
        throw new DJUnitRuntimeException(createCannotSetObjectTypeMessage(classAndMethodName));
      }
      // for own source class
      throw new DJUnitRuntimeException(createMismatchReturnTypeMessage(value, classAndMethodName));
    }
  }

  private static String createMismatchExceptionTypeMessage(Object value, String classAndMethodName) {
    StringBuffer sb = new StringBuffer();
    sb.append("Mismatch exception type.");
    sb.append("This method(");
    sb.append(classAndMethodName);
    sb.append(") can ");
    sb.append("NOT throws");
    sb.append(" [");
    sb.append(value.getClass().getName());
    sb.append("] type exception. ");
    return sb.toString();
  }

  private static String createMismatchReturnTypeMessage(Object value, String classAndMethodName) {
    StringBuffer sb = new StringBuffer();
    sb.append("Mismatch return type.You can NOT set [");
    sb.append(value.getClass().getName());
    sb.append("] type return value into ");
    sb.append("this method(");
    sb.append(classAndMethodName);
    sb.append(").");
    return sb.toString();
  }

  private static String createCannotSetObjectTypeMessage(String classAndMethodName) {
    StringBuffer sb = new StringBuffer();

    sb.append("Mismatch object type.");
    sb.append("At");
    sb.append(classAndMethodName);
    sb.append(".");

    return sb.toString();
  }

  private static String makeKey(String className, String methodName) {
    return className + "." + methodName;
  }
}
