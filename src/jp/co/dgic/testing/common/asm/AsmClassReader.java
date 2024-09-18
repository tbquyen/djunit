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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public class AsmClassReader extends ClassReader {
  private AsmClassReader(byte[] bytecodes) {
    super(bytecodes);
  }

  private AsmClassReader(String name) throws IOException {
    super(name);
  }

  public static AsmClassReader getInstance(String className, byte[] bytecodes) throws IOException {
    if (bytecodes != null) {
      return new AsmClassReader(bytecodes);
    }

    return new AsmClassReader(className);
  }

  public void accept(ClassVisitor cv) {
    super.accept(cv, ClassReader.SKIP_FRAMES);
  }
}
