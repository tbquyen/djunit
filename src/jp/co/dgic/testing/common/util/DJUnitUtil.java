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
import java.util.StringTokenizer;

public class DJUnitUtil {
  public static final String PROJECTS_SOURCE_DIR_KEY = "jp.co.dgic.project.source.dir";

  public static final String JUNIT_EXCLUDES_PATHS_KEY = "jp.co.dgic.junit.excluded.paths";

  public static final String LOG_LEVEL_KEY = "djunit.log.level";

  public static final String SEPARATOR = System.getProperty("path.separator");
  public static final String LOG_LEVEL = System.getProperty(LOG_LEVEL_KEY);

  private static String[] sourceDirectries;
  private static String[] classExclusions;

  /** default excluded paths */
  private static String[] DEFAULT_EXCLUSIONS = { "org.objectweb.asm.", "jp.co.dgic.testing.", "org.eclipse.", "junit.",
      "org.junit.", "org.jacoco." };

  static {
    String directriesValue = System.getProperty(PROJECTS_SOURCE_DIR_KEY);
    if (directriesValue != null) {
      debug("Projects source dir: " + directriesValue);
      sourceDirectries = DJUnitUtil.splitValue(directriesValue);
    } else {
      String classpath = System.getProperty("java.class.path");
      debug("Projects source dir: " + classpath);
      sourceDirectries = DJUnitUtil.splitValue(System.getProperty("java.class.path"));
    }

    String paths = System.getProperty(JUNIT_EXCLUDES_PATHS_KEY);
    if (paths != null) {
      debug("Class exclusions: " + paths);
      classExclusions = DJUnitUtil.splitValue(paths);
    }
  }

  public static boolean isProjectsSource(String className) {
    String[] dirs = sourceDirectries;

    if (dirs == null)
      return false;

    String pathString = null;
    File f = null;
    for (int idx = 0; idx < dirs.length; idx++) {
      pathString = dirs[idx] + "/" + toClassName(className) + ".class";
      f = new File(pathString);
      if (f.exists()) {
        return true;
      }
    }
    return false;
  }

  private static String toClassName(String className) {
    String name = className.replace('.', '/');
    if (name.indexOf('$') < 0)
      return name;
    String simpleName = getSimpleName(name);
    if (simpleName.indexOf('$') < 0)
      return name;
    int lastIndex = name.indexOf('$');
    return name.substring(0, lastIndex);
  }

  private static String getSimpleName(String className) {
    int lastIndex = className.lastIndexOf('.');
    return className.substring(lastIndex + 1);
  }

  public static String[] splitValue(String value) {
    if (value == null) {
      return null;
    }

    StringTokenizer st = new StringTokenizer(value, SEPARATOR);
    String[] values = new String[st.countTokens()];
    for (int index = 0; index < values.length; index++) {
      values[index] = st.nextToken();
    }
    return values;
  }

  public static boolean isDefaultExcludedPath(String className) {
    className = className.replace("/", ".");
    String[] defaultExcluded = DEFAULT_EXCLUSIONS;
    for (int idx = 0; idx < defaultExcluded.length; idx++) {
      if (className.startsWith(defaultExcluded[idx]))
        return true;
    }
    return false;
  }

  public static boolean isOwnSource(String className) {
    String name = className.replace('/', '.');

    if (VirtualMockUtil.getIncludeValue() != null && VirtualMockUtil.isInclude(name)) {
      return true;
    }
    if (DJUnitUtil.isProjectsSource(name)) {
      return true;
    }

    return false;
  }
  
  public static boolean isExcluded(String className) {
    if (isDefaultExcludedPath(className))
      return true;
    return isExcludedForClassloader(className);
  }

  public static boolean isExcludedForClassloader(String className) {
    String[] excluded = classExclusions;
    if (excluded == null) {
      return false;
    }

    className = className.replace("/", ".");
    for (int idx = 0; idx < excluded.length; idx++) {
      if (className.startsWith(removeAsterisk(excluded[idx])))
        return true;
    }
    return false;
  }

  private static String removeAsterisk(String pathString) {
    if (pathString == null)
      return null;
    if (!pathString.endsWith("*"))
      return pathString;
    return pathString.substring(0, pathString.length() - 1);
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
