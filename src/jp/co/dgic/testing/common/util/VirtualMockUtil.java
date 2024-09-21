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
package jp.co.dgic.testing.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;

public class VirtualMockUtil {
  public static final int ASM_API_VERSION = Opcodes.ASM9;

  public static final String CONSTRUCTOR_METHOD_NAME = "<init>";

  public static final String VIRTUALMOCK_USE_VIRTUALMOCK_KEY = "jp.co.dgic.virtualmock.usevirtualmock";

  public static final String VIRTUALMOCK_INCLUDE_CLASS_KEY = "jp.co.dgic.virtualmock.include.class";

  public static final String VIRTUALMOCK_IGNORE_LIBRARY_KEY = "jp.co.dgic.virtualmock.ignore.library";

  public static final String VIRTUALMOCK_NOTIGNORE_PATTERNS_KEY = "jp.co.dgic.virtualmock.notignore.patterns";

  public static final String VIRTUALMOCK_COVERAGE_METHODS_KEY = "jp.co.dgic.virtualmock.coverage.methods";

  public static final String USEVIRTUALMOCK = System.getProperty(VIRTUALMOCK_USE_VIRTUALMOCK_KEY, "true");
  public static final String isIgnoreLibrary = System.getProperty(VIRTUALMOCK_IGNORE_LIBRARY_KEY);

  private static String[] includes;
  private static String[] notIgnorePatterns;
  public static String[] coverageMethods = { "$jacocoInit" };

  static {
    String coverageMethod = System.getProperty(VIRTUALMOCK_COVERAGE_METHODS_KEY);
    if (coverageMethod != null) {
      DJUnitUtil.debug("Coverage methods: " + coverageMethod);
      coverageMethods = DJUnitUtil.splitValue(coverageMethod);
    }

    String includesValue = System.getProperty(VIRTUALMOCK_INCLUDE_CLASS_KEY);
    if (includesValue != null) {
      DJUnitUtil.debug("Includes: " + includesValue);
      includes = DJUnitUtil.splitValue(includesValue);
    }

    String patterns = System.getProperty(VIRTUALMOCK_NOTIGNORE_PATTERNS_KEY);
    if (patterns != null) {
      notIgnorePatterns = DJUnitUtil.splitValue(patterns);
    }
  }

  public static boolean isUseVirtualMock() {
    return "true".equalsIgnoreCase(USEVIRTUALMOCK);
  }

  public static boolean isIgnoreLibrary() {
    return "true".equalsIgnoreCase(isIgnoreLibrary);
  }

  public static boolean isCoverageMethod(String methodName) {
    for (String method : coverageMethods) {
      if (methodName.equalsIgnoreCase(method)) {
        return true;
      }
    }

    return false;
  }

  public static boolean isNotIgnore(String className) {
    String[] patterns = notIgnorePatterns;
    if (patterns == null) {
      return false;
    }

    className = className.replace("/", ".");
    for (int index = 0; index < patterns.length; index++) {
      Pattern pattern = Pattern.compile(patterns[index], Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(className);
      if (matcher.find()) {
        return true;
      }
    }

    return false;
  }

  public static boolean isInclude(String className) {
    if (includes == null) {
      return true;
    }

    className = className.replace("/", ".");
    for (int i = 0; i < includes.length; i++) {
      if (className.equals(includes[i])) {
        return true;
      }
    }

    return false;
  }

  public static String[] getIncludeValue() {
    return includes;
  }
}
