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

import java.io.File;

import org.objectweb.asm.Opcodes;

public class DJUnitUtil {
  public static final String LOG_LEVEL_KEY = "djunit.log.level";
  public static final String PROJECTS_SOURCE_DIR_KEY = "djunit.source.dir";
  public static final String JUNIT_EXCLUDES_PATHS_KEY = "djunit.excluded.paths";
  public static final String VIRTUALMOCK_INCLUDE_CLASS_KEY = "djunit.include.class";
  public static final String VIRTUALMOCK_NOTIGNORE_PATTERNS_KEY = "djunit.notignore.patterns";
  public static final String VIRTUALMOCK_COVERAGE_METHODS_KEY = "djunit.coverage.methods";

  public static final int ASM_API_VERSION = Opcodes.ASM9;
  public static final String CONSTRUCTOR_METHOD_NAME = "<init>";
  public static final String[] DJUNIT_EXCLUSIONS = { "org.objectweb.asm.", "jp.co.dgic.testing.", "org.eclipse." };
  public static final String[] JUNIT_EXCLUSIONS = { "junit.", "org.junit.", "org.jacoco." };

  public static final String LOG_LEVEL = System.getProperty(LOG_LEVEL_KEY);
  public static final String SEPARATOR = System.getProperty("path.separator");

  public static String[] sourceDirectries = { "src", "src/main/java" };
  private static String[] classExclusions = {};
  public static String[] classInclude = null;
  private static String[] notIgnorePatterns = null;
  public static String[] coverageMethods = { "$jacocoInit" };

  static {
    String directriesValue = System.getProperty(PROJECTS_SOURCE_DIR_KEY);
    if (directriesValue != null) {
      debug("Projects source dir: " + directriesValue);
      sourceDirectries = directriesValue.split(SEPARATOR);
    }

    String paths = System.getProperty(JUNIT_EXCLUDES_PATHS_KEY);
    if (paths != null) {
      debug("Class exclusions: " + paths);
      classExclusions = paths.split(SEPARATOR);
    }

    String includeValue = System.getProperty(VIRTUALMOCK_INCLUDE_CLASS_KEY);
    if (includeValue != null) {
      debug("Class include: " + includeValue);
      classInclude = includeValue.split(SEPARATOR);
    }

    String patterns = System.getProperty(VIRTUALMOCK_NOTIGNORE_PATTERNS_KEY);
    if (patterns != null) {
      debug("Not ignore patterns: " + patterns);
      notIgnorePatterns = patterns.split(SEPARATOR);
    }

    String coverageMethod = System.getProperty(VIRTUALMOCK_COVERAGE_METHODS_KEY);
    if (coverageMethod != null) {
      debug("Coverage methods: " + coverageMethod);
      coverageMethods = coverageMethod.split(SEPARATOR);
    }
  }

  public static boolean isUseVirtualMock() {
    return true;
  }

  public static boolean isExcluded(String className) {
    className = DJUnitUtil.getQualifiedName(className);

    for (String packageName : DJUNIT_EXCLUSIONS) {
      if (className.startsWith(packageName)) {
        return true;
      }
    }

    for (String packageName : JUNIT_EXCLUSIONS) {
      if (className.startsWith(packageName)) {
        return true;
      }
    }

    for (String excluded : classExclusions) {
      if (className.startsWith(removeAsterisk(excluded))) {
        return true;
      }
    }

    return false;
  }

  public static boolean isProjectsSource(String className) {
    className = DJUnitUtil.getClassPath(className);

    String pathString = null;
    File f = null;
    for (String dir : sourceDirectries) {
      pathString = dir + "/" + className + ".java";
      f = new File(pathString);
      if (f.exists()) {
        return true;
      }
    }

    return false;
  }

  public static boolean isCoverageMethod(String methodName) {
    for (String method : coverageMethods) {
      if (methodName.equalsIgnoreCase(method)) {
        return true;
      }
    }

    return false;
  }

  public static boolean isIgnore(String className) {
    if (notIgnorePatterns == null || className == null) {
      return false;
    }

    for (String pattern : notIgnorePatterns) {
      if (className.matches(pattern)) {
        return true;
      }
    }

    return false;
  }

  public static boolean isInclude(String className) {
    if (classInclude == null) {
      return true;
    }

    className = getQualifiedName(className);

    for (String method : classInclude) {
      if (className.equals(method)) {
        return true;
      }
    }

    return false;
  }

  public static Object getIncludeValue() {
    return classInclude;
  }

  /**
   * Method to get the class path from the class name
   */
  private static String getClassPath(String className) {
    if (className == null) {
      return null;
    }

    String name = className.replace('.', '/');
    if (name.indexOf('$') < 0) {
      return name;
    }

    String simpleName = DJUnitUtil.getSimpleName(name);
    if (simpleName.indexOf('$') < 0) {
      return name;
    }

    int lastIndex = name.indexOf('$');
    return name.substring(0, lastIndex);
  }

  /**
   * Method to get the simple name of the class from the class name
   */
  private static String getSimpleName(String className) {
    int lastIndex = className.lastIndexOf('.');
    return className.substring(lastIndex + 1);
  }

  private static String removeAsterisk(String pathString) {
    if (pathString == null) return null;
    if (!pathString.endsWith("*")) return pathString;
    return pathString.substring(0, pathString.length() - 1);
  }

  /**
   * Converts a class file path to a fully qualified class name.
   */
  public static String getQualifiedName(String classPathName) {
    if (classPathName == null) {
      return null;
    }
    return classPathName.replace("/", ".");
  }

  public static void debug(Object message) {
    if ("debug".equals(LOG_LEVEL) || "trace".equals(LOG_LEVEL)) {
      System.out.println("[DJUnit][DEBUG]" + message);
    }
  }

  public static void trace(Object message) {
    if ("trace".equals(LOG_LEVEL)) {
      System.out.println("[DJUnit][TRACE]" + message);
    }
  }
}
