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
package jp.co.dgic.testing.common;

import jp.co.dgic.testing.common.asm.AsmClassChecker;
import jp.co.dgic.testing.common.asm.AsmClassReader;
import jp.co.dgic.testing.common.asm.AsmClassVisitor;
import jp.co.dgic.testing.common.util.DJUnitUtil;

public class DJUnitClassModifier {
  /**
   * Returns the modified class, or null if no modification is made.
   * 
   * @param className       the name of the class to be modified.
   * @param classfileBuffer the bytecode of the class.
   * @return the modified bytecode of the class, or null if no modification is made.
   */
  public byte[] getModifiedClass(String className, byte[] classfileBuffer) {
    DJUnitUtil.trace("[DJUnitClassModifier] load target  [" + className + "]");
    if (!DJUnitUtil.isUseVirtualMock()) {
      DJUnitUtil.trace("[DJUnitClassModifier][getModifiedClass]: " + className + " not Use VirtualMock");
      return null;
    }

    if (!DJUnitUtil.isProjectsSource(className)) {
      DJUnitUtil.trace("[DJUnitClassModifier][getModifiedClass]: " + className + " is not Projects Source");
      return null;
    }

    if (DJUnitUtil.isExcluded(className)) {
      DJUnitUtil.trace("[DJUnitClassModifier][getModifiedClass]: " + className + " is Excluded");
      return null;
    }

    try {
      AsmClassReader reader = AsmClassReader.getInstance(className, classfileBuffer);
      AsmClassChecker checker = AsmClassChecker.createInstance(className, reader);

      if (checker.isInterface()) {
        DJUnitUtil.debug("[DJUnitClassModifier][getModifiedClass] : " + className + " is Interface");
        return null;
      }

      if (checker.isAnnotation()) {
        DJUnitUtil.debug("[DJUnitClassModifier][getModifiedClass] : " + className + " is Annotation");
        return null;
      }

      if (checker.isTestCase()) {
        DJUnitUtil.debug("[DJUnitClassModifier][getModifiedClass] : " + className + " is TestCase");
        return null;
      }

      DJUnitUtil.debug("[DJUnitClassModifier] Modified Class : " + className);
      AsmClassVisitor writer = new AsmClassVisitor(className, reader);
      return writer.toByteArray();
    } catch (Throwable e) {
      e.printStackTrace();
    }

    return null;
  }
}
