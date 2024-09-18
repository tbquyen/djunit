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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import jp.co.dgic.testing.common.util.DJUnitUtil;
import jp.co.dgic.testing.virtualmock.asm.AsmConstractorVisitor;
import jp.co.dgic.testing.virtualmock.asm.AsmMethodVisitor;

public class AsmClassVisitor extends ClassVisitor {
  protected AsmClassChecker checker;
  protected String className;
  protected AsmClassWriter classWriter;

  public AsmClassVisitor(String className, AsmClassReader reader) {
    super(DJUnitUtil.ASM_API_VERSION);
    checker = AsmClassChecker.getInstance(className, reader);
    classWriter = new AsmClassWriter();
    cv = this.classWriter;
    reader.accept(this);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    DJUnitUtil.debug("[AsmClassVisitor][visit][modify-class=" + name + "][signature=" + signature + "]");
    DJUnitUtil.debug("[AsmClassVisitor][visit][class-version=" + version + "]");
    this.className = DJUnitUtil.getQualifiedName(name);
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    boolean isStatic = false;
    if ((access & Opcodes.ACC_STATIC) > 0) {
      isStatic = true;
    }

    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

    // is abstract or native
    if ((access & Opcodes.ACC_ABSTRACT) > 0) {
      return mv;
    }
    if ((access & Opcodes.ACC_NATIVE) > 0) {
      return mv;
    }
    if ((access & Opcodes.ACC_BRIDGE) > 0) {
      return mv;
    }
    if (DJUnitUtil.isCoverageMethod(name)) {
      return mv;
    }

    int maxLocals = checker.getMaxLocals(name, descriptor);

    return createMethodVisitor(mv, name, descriptor, signature, isStatic, exceptions, maxLocals);
  }

  private MethodVisitor createMethodVisitor(MethodVisitor mv, String name, String desc, String signature,
      boolean isStatic, String[] exceptions, int maxLocals) {

    if (DJUnitUtil.CONSTRUCTOR_METHOD_NAME.equalsIgnoreCase(name)) {
      DJUnitUtil
      .debug("[AsmClassVisitor][createMethodVisitor] : AsmConstractorVisitor(" + className + "::" + name + ")");
      return new AsmConstractorVisitor(mv, this.className, name, desc, signature, exceptions, maxLocals,
          checker.getSuperClassNames());
    }

    DJUnitUtil.debug("[AsmClassVisitor][createMethodVisitor] : AsmMethodVisitor(" + className + "::" + name + ")");
    return new AsmMethodVisitor(mv, this.className, name, desc, signature, isStatic, exceptions, maxLocals,
        checker.getSuperClassNames());
  }

  public byte[] toByteArray() {
    DJUnitUtil.debug("[AsmClassVisitor][toByteArray][modify-class=" + className + "]");
    return this.classWriter.toByteArray();
  }
}
