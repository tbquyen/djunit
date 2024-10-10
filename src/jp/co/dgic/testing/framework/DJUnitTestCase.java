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
package jp.co.dgic.testing.framework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import jp.co.dgic.testing.virtualmock.MockObjectManager;

public abstract class DJUnitTestCase extends Assertions {
  @BeforeEach
  protected void setUp() throws Exception {
    MockObjectManager.initialize();
  }

  public static void addReturnNull(String className, String methodName) {
    MockObjectManager.addReturnNull(className, methodName);
  }

  public static void addReturnNull(Class<?> cls, String methodName) {
    MockObjectManager.addReturnNull(cls, methodName);
  }

  public static void addReturnValue(String className, String methodName) {
    MockObjectManager.addReturnValue(className, methodName);
  }

  public static void addReturnValue(Class<?> cls, String methodName) {
    MockObjectManager.addReturnValue(cls, methodName);
  }

  public static void addReturnValue(String className, String methodName, Object returnValue) {
    MockObjectManager.addReturnValue(className, methodName, returnValue);
  }

  public static void addReturnValue(Class<?> cls, String methodName, Object returnValue) {
    MockObjectManager.addReturnValue(cls, methodName, returnValue);
  }

  public static void assertArgumentPassed(String className, String methodName, int argumentindex,
      Object argumentValue) {
    MockObjectManager.assertArgumentPassed(className, methodName, argumentindex, argumentValue);
  }

  public static void assertArgumentPassed(Class<?> cls, String methodName, int argumentindex, Object argumentValue) {
    MockObjectManager.assertArgumentPassed(cls, methodName, argumentindex, argumentValue);
  }

  public static void assertCalled(String className, String methodName) {
    MockObjectManager.assertCalled(className, methodName);
  }

  public static void assertCalled(Class<?> cls, String methodName) {
    MockObjectManager.assertCalled(cls, methodName);
  }

  public static void assertNotCalled(String className, String methodName) {
    MockObjectManager.assertNotCalled(className, methodName);
  }

  public static void assertNotCalled(Class<?> cls, String methodName) {
    MockObjectManager.assertNotCalled(cls, methodName);
  }

  public static Object getArgument(String className, String methodName, int argumentIndex) {
    return MockObjectManager.getArgument(className, methodName, argumentIndex);
  }

  public static Object getArgument(Class<?> cls, String methodName, int argumentIndex) {
    return MockObjectManager.getArgument(cls, methodName, argumentIndex);
  }

  public static Object getArgument(String className, String methodName, int methodIndex, int argumentIndex) {
    return MockObjectManager.getArgument(className, methodName, methodIndex, argumentIndex);
  }

  public static Object getArgument(Class<?> cls, String methodName, int methodIndex, int argumentIndex) {
    return MockObjectManager.getArgument(cls, methodName, methodIndex, argumentIndex);
  }

  public static int getCallCount(String className, String methodName) {
    return MockObjectManager.getCallCount(className, methodName);
  }

  public static int getCallCount(Class<?> cls, String methodName) {
    return MockObjectManager.getCallCount(cls, methodName);
  }

  public static Object getReturnValue(String className, String methodName) {
    return MockObjectManager.getReturnValue(className, methodName);
  }

  public static Object getReturnValue(Class<?> cls, String methodName) {
    return MockObjectManager.getReturnValue(cls, methodName);
  }

  public static boolean isCalled(String className, String methodName) {
    return MockObjectManager.isCalled(className, methodName);
  }

  public static boolean isCalled(Class<?> cls, String methodName) {
    return MockObjectManager.isCalled(cls, methodName);
  }

  public static void setReturnValueAt(String className, String methodName, int index, Object returnValue) {
    MockObjectManager.setReturnValueAt(className, methodName, index, returnValue);
  }

  public static void setReturnValueAt(Class<?> cls, String methodName, int index, Object returnValue) {
    MockObjectManager.setReturnValueAt(cls, methodName, index, returnValue);
  }

  public static void setReturnValueAt(String className, String methodName, int index) {
    MockObjectManager.setReturnValueAt(className, methodName, index);
  }

  public static void setReturnValueAt(Class<?> cls, String methodName, int index) {
    MockObjectManager.setReturnValueAt(cls, methodName, index);
  }

  public static void setReturnNullAt(String className, String methodName, int index) {
    MockObjectManager.setReturnNullAt(className, methodName, index);
  }

  public static void setReturnNullAt(Class<?> cls, String methodName, int index) {
    MockObjectManager.setReturnNullAt(cls, methodName, index);
  }

  public static void setReturnValueAtAllTimes(String className, String methodName, Object returnValue) {
    MockObjectManager.setReturnValueAtAllTimes(className, methodName, returnValue);
  }

  public static void setReturnValueAtAllTimes(Class<?> cls, String methodName, Object returnValue) {
    MockObjectManager.setReturnValueAtAllTimes(cls, methodName, returnValue);
  }

  public static void setReturnValueAtAllTimes(String className, String methodName) {
    MockObjectManager.setReturnValueAtAllTimes(className, methodName);
  }

  public static void setReturnValueAtAllTimes(Class<?> cls, String methodName) {
    MockObjectManager.setReturnValueAtAllTimes(cls, methodName);
  }

  public static void setReturnNullAtAllTimes(String className, String methodName) {
    MockObjectManager.setReturnNullAtAllTimes(className, methodName);
  }

  public static void setReturnNullAtAllTimes(Class<?> cls, String methodName) {
    MockObjectManager.setReturnNullAtAllTimes(cls, methodName);
  }

}
