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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

import jp.co.dgic.testing.common.util.DJUnitUtil;
import jp.co.dgic.testing.common.util.VirtualMockUtil;

public class AsmMethodChecker extends MethodVisitor {
  private static final String JUNIT_TEST_ANNOTATION = "org/junit/";

  private AsmClassChecker checker;
  private String name;
  private String desc;

  public AsmMethodChecker(AsmClassChecker checker, String name, String desc) {
    super(VirtualMockUtil.ASM_API_VERSION);
    this.checker = checker;
    this.name = name;
    this.desc = desc;
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    checker.putMaxLocals(this.name, this.desc, maxLocals);
  }
  
  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    DJUnitUtil.trace("[AsmMethodChecker][visitAnnotation] : " + name + ", " + desc + "," + visible);
    if (desc != null && desc.indexOf(JUNIT_TEST_ANNOTATION) >= 0) {
      checker.setJUnitTestAnnotation(true);
    }

    return super.visitAnnotation(desc, visible);
  }
}
