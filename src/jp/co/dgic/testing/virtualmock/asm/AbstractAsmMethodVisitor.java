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
package jp.co.dgic.testing.virtualmock.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import jp.co.dgic.testing.common.asm.AsmClassChecker;
import jp.co.dgic.testing.common.util.DJUnitUtil;
import jp.co.dgic.testing.common.util.VirtualMockUtil;

public abstract class AbstractAsmMethodVisitor extends MethodVisitor implements Opcodes {
  protected static final String MANAGER_PACKAGE_NAME = "jp/co/dgic/testing/virtualmock/";
  protected static final String MANAGER_CLASS_NAME = MANAGER_PACKAGE_NAME + "InternalMockObjectManager";
  protected static final String NULL_RETURN_VALUE_CLASS_NAME = MANAGER_PACKAGE_NAME + "NullReturnValue";

  protected String _className;
  protected String _methodName;
  protected String _desc;
  protected String _signature;
  protected boolean _isStatic = false;
  protected String[] _exceptions;
  protected int _maxLocals = -1;
  protected Type[] _types;
  protected Type _returnType;
  protected String[] _superClassNames;
  protected boolean isAfterDup = false;

  public AbstractAsmMethodVisitor(MethodVisitor methodVisitor, String className, String methodName, String desc,
      String signature, boolean isStatic, String[] exceptions, int maxLocals, String[] superClassNames) {
    super(VirtualMockUtil.ASM_API_VERSION, methodVisitor);
    this._className = className;
    this._methodName = methodName;
    this._desc = desc;
    this._signature = signature;
    this._isStatic = isStatic;
    this._exceptions = exceptions;
    this._maxLocals = maxLocals;
    this._types = Type.getArgumentTypes(desc);
    this._returnType = Type.getReturnType(desc);
    this._superClassNames = superClassNames;
  }

  @Override
  public void visitInsn(int opcode) {
    if (opcode == DUP || opcode == DUP_X1) {
      isAfterDup = true;
    }

    super.visitInsn(opcode);
  }

  @Override
  public void visitTypeInsn(int opcode, String desc) {
    if (opcode == NEW) {
      isAfterDup = false;
    }

    super.visitTypeInsn(opcode, desc);
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    boolean isInterface = isInterface(owner);

    String newOwner = owner;
    if (!isInterface) {
      newOwner = getOwnerClassName(owner, name, desc);
    }

    if (!owner.equals(newOwner)) {
      DJUnitUtil.debug("[AbstractAsmMethodVisitor][visitMethodInsn][owner: " + owner + "][real owner: " + newOwner
          + "][desc: " + desc + "]");
    }

    if (isConstructor(name)) {
      createConstructorCall(opcode, newOwner, name, desc, itf);
      return;
    }

    createMethodCall(opcode, newOwner, name, desc, itf);
  }

  public void createMethodCall(int opcode, String owner, String name, String desc, boolean itf) {
    boolean isStaticMethod = (opcode == Opcodes.INVOKESTATIC);
    boolean isInterface = (opcode == Opcodes.INVOKEINTERFACE);

    if (!canReplace(owner, isInterface)) {
      // call real method
      mv.visitMethodInsn(opcode, owner, name, desc, itf);
      return;
    }

    DJUnitUtil.debug("[INVOKE METHOD] : itf = " + itf + ", opcode=" + opcode + (isStaticMethod ? " static " : " ")
        + owner + "#" + name + " " + desc);

    // createCreateArgsArray
    Label hasMockReturnValue = new Label();
    Type[] argTypes = Type.getArgumentTypes(desc);
    if (argTypes == null) {
      argTypes = new Type[0];
    }

    // before method call
    createCopyStackArgsToLocalVariables(isStaticMethod, argTypes);
    createCreateArgsArray(isStaticMethod, argTypes, _maxLocals + 1);

    // call MockObjecManager.indicateCalledAndGetReturnValue
    mv.visitLdcInsn(makeKey(owner.replace('/', '.'), name));
    mv.visitVarInsn(ALOAD, _maxLocals);
    mv.visitMethodInsn(INVOKESTATIC, MANAGER_CLASS_NAME, "indicateCalledAndGetReturnValue",
        "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false);
    mv.visitVarInsn(ASTORE, _maxLocals);

    // if (mock value != null) GOTO LABEL : mock value is NOT null
    mv.visitVarInsn(ALOAD, _maxLocals);
    mv.visitJumpInsn(IFNONNULL, hasMockReturnValue);

    createPutArgsIntoStackFromLocalValriables(isStaticMethod, argTypes);

    // call real method
    mv.visitMethodInsn(opcode, owner, name, desc, itf);

    // after method call
    // GOTO LABEL : next statement
    Label toNextStatement = new Label();
    mv.visitJumpInsn(GOTO, toNextStatement);

    // LABEL : mock value is NOT null
    mv.visitLabel(hasMockReturnValue);

    // throw exception
    String[] exceptions = getExceptions(owner, name, desc);
    createThrowExceptions(exceptions);
    createInvoleThrowException(owner, name);

    Type returnType = Type.getReturnType(desc);
    if (isVoid(returnType, name)) {
      // check return value
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitLdcInsn(makeName(owner, name));
      mv.visitMethodInsn(INVOKESTATIC, MANAGER_CLASS_NAME, "checkReturnTypeIsIgnoreOrNullReturnValue",
          "(Ljava/lang/Object;Ljava/lang/String;)V", false);
    } else {
      String returnTypeClassName = getReturnTypeClassName(returnType, owner, name);

      // check return value
      // instanceof NullReturnValue
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitTypeInsn(INSTANCEOF, NULL_RETURN_VALUE_CLASS_NAME);
      Label toSetNullValue = new Label();
      mv.visitJumpInsn(IFNE, toSetNullValue);
      // instanceof return type
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitTypeInsn(INSTANCEOF, returnTypeClassName);
      Label toMockValueLoad = new Label();
      mv.visitJumpInsn(IFNE, toMockValueLoad);

      // checkReturnTypeIsNullReturnValue
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitLdcInsn(makeName(owner, name));
      mv.visitMethodInsn(INVOKESTATIC, MANAGER_CLASS_NAME, "checkReturnTypeIsNullReturnValue",
          "(Ljava/lang/Object;Ljava/lang/String;)V", false);

      // set null value
      mv.visitLabel(toSetNullValue);
      mv.visitInsn(getZeroOpcodeByType(returnType));

      mv.visitJumpInsn(GOTO, toNextStatement);

      // load mock return value
      mv.visitLabel(toMockValueLoad);
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitTypeInsn(CHECKCAST, returnTypeClassName);

      if (isPrimitive(returnType)) {
        String methodNameOfToValue = getToValueMethodName(returnType);
        String descriptorOfToValue = getToValueDescriptor(returnType);
        mv.visitMethodInsn(INVOKEVIRTUAL, returnTypeClassName, methodNameOfToValue, descriptorOfToValue, false);
      }
    }

    // LABEL : next statement
    mv.visitLabel(toNextStatement);

  }

  public void createConstructorCall(int opcode, String owner, String name, String desc, boolean itf) {

    if (isSuperOrThis(owner)) {
      DJUnitUtil.debug("[AbstractAsmMethodVisitor][createConstructorCall][REAL METHOD]isSuperOrThis : itf = " + itf
          + ", opcode=" + opcode + ", " + owner + "::" + name + " " + desc);
      // call real method
      mv.visitMethodInsn(opcode, owner, name, desc, itf);
      return;
    }

    if (!canNewExprReplace(owner)) {
      DJUnitUtil.debug("[AbstractAsmMethodVisitor][createConstructorCall][REAL METHOD]can'tNewExprReplace : itf = " + itf
          + ", opcode=" + opcode + ", " + owner + "::" + name + " " + desc);
      // call real method
      mv.visitMethodInsn(opcode, owner, name, desc, itf);
      return;
    }

    boolean isStaticMethod = (opcode == Opcodes.INVOKESTATIC);
    DJUnitUtil.debug("[AbstractAsmMethodVisitor][createConstructorCall][REPLACE METHOD] : itf = " + itf + ", opcode="
        + opcode + ", " + owner + "::" + name + " " + desc);

    // createCreateArgsArray
    Label hasMockReturnValue = new Label();
    Type[] argTypes = Type.getArgumentTypes(desc);
    if (argTypes == null) {
      argTypes = new Type[0];
    }

    // before method call
    createCopyStackArgsToLocalVariables(isStaticMethod, argTypes);
    createCreateArgsArray(isStaticMethod, argTypes, _maxLocals + 1);

    // call MockObjecManager.indicateCalledAndGetReturnValueForNewExpr
    mv.visitLdcInsn(makeKey(owner.replace('/', '.'), name));
    mv.visitVarInsn(ALOAD, _maxLocals);
    mv.visitLdcInsn(isOwnSource(owner));
    mv.visitMethodInsn(INVOKESTATIC, MANAGER_CLASS_NAME, "indicateCalledAndGetReturnValueForNewExpr",
        "(Ljava/lang/String;[Ljava/lang/Object;Z)Ljava/lang/Object;", false);
    mv.visitVarInsn(ASTORE, _maxLocals);

    // if (mock value != null) GOTO LABEL : mock value is NOT null
    mv.visitVarInsn(ALOAD, _maxLocals);
    mv.visitJumpInsn(IFNONNULL, hasMockReturnValue);

    createPutArgsIntoStackFromLocalValriables(isStaticMethod, argTypes);

    // call real method
    mv.visitMethodInsn(opcode, owner, name, desc, itf);

    // after method call
    // GOTO LABEL : next statement
    Label toNextStatement = new Label();
    mv.visitJumpInsn(GOTO, toNextStatement);

    // LABEL : mock value is NOT null
    mv.visitLabel(hasMockReturnValue);

    if (isOwnSource(owner)) {
      mv.visitVarInsn(ALOAD, _maxLocals);
      createCreateArgsArray(isStaticMethod, argTypes, _maxLocals + 1);
      mv.visitLdcInsn(makeKey(owner.replace('/', '.'), name));
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitMethodInsn(INVOKESTATIC, MANAGER_CLASS_NAME, "indicateCalled", "(Ljava/lang/String;[Ljava/lang/Object;)V",
          false);
      mv.visitVarInsn(ASTORE, _maxLocals);
    }

    // throw exception
    String[] exceptions = getExceptions(owner, name, desc);
    createThrowExceptions(exceptions);
    createInvoleThrowException(owner, name);

    Type returnType = Type.getReturnType(desc);

    if (!isAfterDup) {
      mv.visitLabel(toNextStatement);
      return;
    }

    mv.visitInsn(POP);

    String returnTypeClassName = getReturnTypeClassName(returnType, owner, name);

    // check return value
    // instanceof NullReturnValue
    mv.visitVarInsn(ALOAD, _maxLocals);
    mv.visitTypeInsn(INSTANCEOF, NULL_RETURN_VALUE_CLASS_NAME);
    Label toSetNullValue = new Label();
    mv.visitJumpInsn(IFNE, toSetNullValue);
    // instanceof return type
    mv.visitVarInsn(ALOAD, _maxLocals);
    mv.visitTypeInsn(INSTANCEOF, returnTypeClassName);
    Label toMockValueLoad = new Label();
    mv.visitJumpInsn(IFNE, toMockValueLoad);

    // checkReturnTypeIsNullReturnValue
    mv.visitVarInsn(ALOAD, _maxLocals);
    mv.visitLdcInsn(makeName(owner, name));
    mv.visitMethodInsn(INVOKESTATIC, MANAGER_CLASS_NAME, "checkReturnTypeForNewExpr",
        "(Ljava/lang/Object;Ljava/lang/String;)V", false);

    // set null value
    mv.visitLabel(toSetNullValue);
    mv.visitInsn(ACONST_NULL);

    mv.visitJumpInsn(GOTO, toNextStatement);

    // load mock return value
    mv.visitLabel(toMockValueLoad);
    mv.visitVarInsn(ALOAD, _maxLocals);
    mv.visitTypeInsn(CHECKCAST, returnTypeClassName);

    if (isPrimitive(returnType)) {
      String methodNameOfToValue = getToValueMethodName(returnType);
      String descriptorOfToValue = getToValueDescriptor(returnType);
      mv.visitMethodInsn(INVOKEVIRTUAL, returnTypeClassName, methodNameOfToValue, descriptorOfToValue, false);
    }

    // LABEL : next statement
    mv.visitLabel(toNextStatement);

  }

  protected void createReturnValueProcess() {
    DJUnitUtil
        .debug("[AbstractAsmMethodVisitor][createReturnValueProcess]: " + this._className + "::" + this._methodName);

    // if (Mock value != null) return mock value
    Label l = new Label();
    mv.visitVarInsn(ALOAD, _maxLocals);
    mv.visitJumpInsn(IFNULL, l);

    // throw exception
    createThrowExceptions(_exceptions);
    createInvoleThrowException(_className, _methodName);

    if (isVoid(_returnType, _methodName)) {
      DJUnitUtil.debug("[AbstractAsmMethodVisitor][createReturnValueProcess] : " + this._className + "::"
          + this._methodName + "() is Void");

      // check return value
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitLdcInsn(makeName(_className, _methodName));
      mv.visitMethodInsn(INVOKESTATIC, MANAGER_CLASS_NAME, "checkReturnTypeIsIgnoreOrNullReturnValue",
          "(Ljava/lang/Object;Ljava/lang/String;)V", false);

      mv.visitInsn(RETURN);
    } else {

      String returnTypeClassName = getReturnTypeClassName(_returnType, _className, _methodName);
      DJUnitUtil.debug("[AbstractAsmMethodVisitor][createReturnValueProcess] : " + this._className + "::"
          + this._methodName + "() return [" + returnTypeClassName + "]");

      // check return value
      // instanceof NullReturnValue
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitTypeInsn(INSTANCEOF, NULL_RETURN_VALUE_CLASS_NAME);
      Label toSetNullValue = new Label();
      mv.visitJumpInsn(IFNE, toSetNullValue);
      // instanceof return type
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitTypeInsn(INSTANCEOF, returnTypeClassName);
      Label toMockValueLoad = new Label();
      mv.visitJumpInsn(IFNE, toMockValueLoad);

      // checkReturnTypeIsNullReturnValue
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitLdcInsn(makeName(_className, _methodName));
      mv.visitMethodInsn(INVOKESTATIC, MANAGER_CLASS_NAME, "checkReturnTypeIsNullReturnValue",
          "(Ljava/lang/Object;Ljava/lang/String;)V", false);

      // set null value
      mv.visitLabel(toSetNullValue);
      mv.visitInsn(getZeroOpcodeByType(_returnType));
      mv.visitInsn(getReturnOpcodeByType(_returnType));

      // load mock return value
      mv.visitLabel(toMockValueLoad);
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitTypeInsn(CHECKCAST, returnTypeClassName);

      // return
      if (isPrimitive(_returnType)) {
        String methodNameOfToValue = getToValueMethodName(_returnType);
        String descriptorOfToValue = getToValueDescriptor(_returnType);
        mv.visitMethodInsn(INVOKEVIRTUAL, returnTypeClassName, methodNameOfToValue, descriptorOfToValue, false);
      }
      mv.visitInsn(getReturnOpcodeByType(_returnType));
    }
    mv.visitLabel(l);
  }

//  protected void createPrintln(String string) {
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//    mv.visitLdcInsn(string);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//  }
//
//  protected void createPrintlnMaxLocalsVariable() {
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//    mv.visitVarInsn(ALOAD, _maxLocals);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
//  }

  protected void createThrowExceptions(String[] exceptions) {
    if (exceptions == null)
      return;

    for (int i = 0; i < exceptions.length; i++) {
      DJUnitUtil.debug("exceptions[" + i + "] : " + exceptions[i]);

      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitTypeInsn(INSTANCEOF, exceptions[i]);
      Label nextException = new Label();
      mv.visitJumpInsn(IFEQ, nextException);
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitTypeInsn(CHECKCAST, exceptions[i]);
      mv.visitInsn(ATHROW);
      mv.visitLabel(nextException);
    }
  }

  protected void createInvoleThrowException(String className, String methodName) {
    mv.visitVarInsn(ALOAD, _maxLocals);
    mv.visitLdcInsn(makeName(className, methodName));
    mv.visitMethodInsn(INVOKESTATIC, MANAGER_CLASS_NAME, "throwException", "(Ljava/lang/Object;Ljava/lang/String;)V",
        false);
  }

  // for begin method
  protected void createCreateArgsArray(boolean isStaticMethod, Type[] argTypes, int varStartIndex) {
    mv.visitIntInsn(BIPUSH, argTypes.length);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitVarInsn(ASTORE, _maxLocals);

    int varIndex = varStartIndex;
    if (!isStaticMethod)
      varIndex++;

    String wrapperType;
    for (int i = 0; i < argTypes.length; i++) {
      mv.visitVarInsn(ALOAD, _maxLocals);
      mv.visitIntInsn(BIPUSH, i);

      if (isPrimitive(argTypes[i])) {
        wrapperType = toWrapperType(argTypes[i]);
        mv.visitTypeInsn(NEW, wrapperType);
        mv.visitInsn(DUP);
        mv.visitVarInsn(getLoadOpcodeByType(argTypes[i]), varIndex);
        mv.visitMethodInsn(INVOKESPECIAL, wrapperType, VirtualMockUtil.CONSTRUCTOR_METHOD_NAME,
            "(" + argTypes[i] + ")V", false);
        if (isTwoEntryType(argTypes[i])) {
          varIndex++;
        }
      } else {
        mv.visitVarInsn(ALOAD, varIndex);
      }
      varIndex++;
      mv.visitInsn(AASTORE);
    }
  }

  // for mehod call
  protected void createCopyStackArgsToLocalVariables(boolean isStaticMethod, Type[] argTypes) {
    int argLength = argTypes.length;
    int argIndex = _maxLocals + argLength;

    argIndex += getTwoEntryTypeCount(argTypes);

    if (!isStaticMethod) {
      argIndex++;
    }

    for (int idx = argLength - 1; idx >= 0; idx--) {
      if (isTwoEntryType(argTypes[idx])) {
        argIndex--;
      }
      mv.visitVarInsn(getStoreOpcodeByType(argTypes[idx]), argIndex);
      argIndex--;
    }

    if (!isStaticMethod) {
      mv.visitVarInsn(ASTORE, argIndex);
    }
  }

  // for mehod call
  protected void createPutArgsIntoStackFromLocalValriables(boolean isStaticMethod, Type[] argTypes) {
    int argLength = argTypes.length;
    int argIndex = _maxLocals + 1;

    if (!isStaticMethod) {
      mv.visitVarInsn(ALOAD, argIndex);
      argIndex++;
    }

    for (int idx = 0; idx < argLength; idx++) {
      mv.visitVarInsn(getLoadOpcodeByType(argTypes[idx]), argIndex);
      if (isTwoEntryType(argTypes[idx])) {
        argIndex++;
      }
      argIndex++;
    }

  }

  protected String[] getExceptions(String className, String methodName, String desc) {
    AsmClassChecker checker = getClassChecker(className);
    if (checker == null) {
      return new String[0];
    }

    return checker.getExceptions(methodName, desc);
  }

  protected String getOwnerClassName(String className, String methodName, String desc) {
    AsmClassChecker acc = getClassChecker(className);
    if (acc == null)
      return className;
    String name = acc.getOwnerName(methodName + desc, className);
    if (name == null)
      return className;
    return name.replace('.', '/');
  }

  private AsmClassChecker getClassChecker(String className) {
    return AsmClassChecker.getInstance(className, null);
  }

  protected int getLoadOpcodeByType(Type type) {
    if (type.equals(Type.BOOLEAN_TYPE))
      return Opcodes.ILOAD;
    if (type.equals(Type.BYTE_TYPE))
      return Opcodes.ILOAD;
    if (type.equals(Type.CHAR_TYPE))
      return Opcodes.ILOAD;
    if (type.equals(Type.SHORT_TYPE))
      return Opcodes.ILOAD;
    if (type.equals(Type.INT_TYPE))
      return Opcodes.ILOAD;
    if (type.equals(Type.LONG_TYPE))
      return Opcodes.LLOAD;
    if (type.equals(Type.DOUBLE_TYPE))
      return Opcodes.DLOAD;
    if (type.equals(Type.FLOAT_TYPE))
      return Opcodes.FLOAD;
    return Opcodes.ALOAD;
  }

  protected int getStoreOpcodeByType(Type type) {
    if (type.equals(Type.BOOLEAN_TYPE))
      return Opcodes.ISTORE;
    if (type.equals(Type.BYTE_TYPE))
      return Opcodes.ISTORE;
    if (type.equals(Type.CHAR_TYPE))
      return Opcodes.ISTORE;
    if (type.equals(Type.SHORT_TYPE))
      return Opcodes.ISTORE;
    if (type.equals(Type.INT_TYPE))
      return Opcodes.ISTORE;
    if (type.equals(Type.LONG_TYPE))
      return Opcodes.LSTORE;
    if (type.equals(Type.DOUBLE_TYPE))
      return Opcodes.DSTORE;
    if (type.equals(Type.FLOAT_TYPE))
      return Opcodes.FSTORE;
    return Opcodes.ASTORE;
  }

  protected int getReturnOpcodeByType(Type type) {
    if (type.equals(Type.BOOLEAN_TYPE))
      return Opcodes.IRETURN;
    if (type.equals(Type.BYTE_TYPE))
      return Opcodes.IRETURN;
    if (type.equals(Type.CHAR_TYPE))
      return Opcodes.IRETURN;
    if (type.equals(Type.SHORT_TYPE))
      return Opcodes.IRETURN;
    if (type.equals(Type.INT_TYPE))
      return Opcodes.IRETURN;
    if (type.equals(Type.LONG_TYPE))
      return Opcodes.LRETURN;
    if (type.equals(Type.DOUBLE_TYPE))
      return Opcodes.DRETURN;
    if (type.equals(Type.FLOAT_TYPE))
      return Opcodes.FRETURN;
    return Opcodes.ARETURN;
  }

  protected String toWrapperType(Type type) {
    if (type.equals(Type.BOOLEAN_TYPE))
      return "java/lang/Boolean";
    if (type.equals(Type.BYTE_TYPE))
      return "java/lang/Byte";
    if (type.equals(Type.CHAR_TYPE))
      return "java/lang/Character";
    if (type.equals(Type.SHORT_TYPE))
      return "java/lang/Short";
    if (type.equals(Type.INT_TYPE))
      return "java/lang/Integer";
    if (type.equals(Type.LONG_TYPE))
      return "java/lang/Long";
    if (type.equals(Type.DOUBLE_TYPE))
      return "java/lang/Double";
    if (type.equals(Type.FLOAT_TYPE))
      return "java/lang/Float";
    return "java/lang/Object";
  }

  protected boolean isPrimitive(Type type) {
    if (type.equals(Type.BOOLEAN_TYPE))
      return true;
    if (type.equals(Type.BYTE_TYPE))
      return true;
    if (type.equals(Type.CHAR_TYPE))
      return true;
    if (type.equals(Type.SHORT_TYPE))
      return true;
    if (type.equals(Type.INT_TYPE))
      return true;
    if (type.equals(Type.LONG_TYPE))
      return true;
    if (type.equals(Type.DOUBLE_TYPE))
      return true;
    if (type.equals(Type.FLOAT_TYPE))
      return true;
    return false;
  }

  protected int getZeroOpcodeByType(Type type) {

    if (type.equals(Type.BOOLEAN_TYPE))
      return ICONST_0;
    if (type.equals(Type.BYTE_TYPE))
      return ICONST_0;
    if (type.equals(Type.CHAR_TYPE))
      return ICONST_0;
    if (type.equals(Type.SHORT_TYPE))
      return ICONST_0;
    if (type.equals(Type.INT_TYPE))
      return ICONST_0;
    if (type.equals(Type.LONG_TYPE))
      return LCONST_0;
    if (type.equals(Type.DOUBLE_TYPE))
      return DCONST_0;
    if (type.equals(Type.FLOAT_TYPE))
      return FCONST_0;

    return ACONST_NULL;
  }

  protected boolean isVoid(Type type, String methodName) {
    if (isConstructor(methodName))
      return false;
    return Type.VOID_TYPE.equals(type);
  }

  protected String getReturnTypeClassName(Type type, String className, String methodName) {
    if (isConstructor(methodName))
      return className;
    if (isPrimitive(type)) {
      return toWrapperType(type);
    }
    if (isArrayType(type))
      return type.toString();
    return type.getClassName().replace('.', '/');
  }

  protected boolean isArrayType(Type type) {
    return type.getSort() == Type.ARRAY;
  }

  protected boolean isConstructor(String methodName) {
    return VirtualMockUtil.CONSTRUCTOR_METHOD_NAME.equals(methodName);
  }

  protected String getToValueMethodName(Type type) {

    if (isPrimitive(type)) {
      return type.getClassName() + "Value";
    }

    throw new IllegalArgumentException("Type[" + type + "] is NOT primitive type.");
  }

  protected String getToValueDescriptor(Type type) {
    if (isPrimitive(type))
      return "()" + type;
    throw new IllegalArgumentException("Type[" + type + "] is NOT primitive type.");
  }

  protected boolean isTwoEntryType(Type type) {
    return Type.LONG_TYPE.equals(type) || Type.DOUBLE_TYPE.equals(type);
  }

  protected int getTwoEntryTypeCount(Type[] types) {
    if (types == null)
      return 0;
    int count = 0;
    for (int i = 0; i < types.length; i++) {
      if (isTwoEntryType(types[i])) {
        count++;
      }
    }
    return count;
  }

  protected boolean isSuperOrThis(String className) {
    if (className == null)
      return false;
    if (_superClassNames == null)
      return false;
    String name = className.replace('/', '.');
    for (int i = 0; i < _superClassNames.length; i++) {
      if (name.equals(_superClassNames[i]))
        return true;
    }
    return false;
  }

  protected boolean canReplace(String className, boolean isInterface) {
    String name = className.replace('/', '.');

    if (DJUnitUtil.isExcluded(name)) {
      return false;
    }
    if (!isInterface && isOwnSource(name)) {
      return false;
    }
    if (isIgnore(name)) {
      return false;
    }

    return true;
  }

  protected boolean canNewExprReplace(String className) {
    String name = className.replace('/', '.');

    if (DJUnitUtil.isExcluded(name)) {
      return false;
    }

    if (isIgnore(name)) {
      if (isOwnSource(name))
        return true;
      return false;
    }

    return true;
  }

  private boolean isIgnore(String className) {
    if (!isIgnoreLibrary())
      return false;
    return !VirtualMockUtil.isNotIgnore(className);
  }

  private boolean isIgnoreLibrary() {
    return VirtualMockUtil.isIgnoreLibrary();
  }

  private boolean isInterface(String className) {
    AsmClassChecker acc = getClassChecker(className);
    if (acc == null)
      return false;
    return acc.isInterface();
  }

  protected boolean isOwnSource(String className) {
    return DJUnitUtil.isOwnSource(className);
  }

  protected String makeKey(String className, String methodName) {
    return className.replace('/', '.') + "." + methodName;
  }

  protected String makeName(String className, String methodName) {
    return className.replace('/', '.') + "#" + methodName;
  }

  protected String getInvokeOpcode(int opcode) {

    if (opcode == INVOKEINTERFACE)
      return "INVOKEINTERFACE";
    if (opcode == INVOKESPECIAL)
      return "INVOKESPECIAL";
    if (opcode == INVOKEVIRTUAL)
      return "INVOKEVIRTUAL";
    if (opcode == INVOKESTATIC)
      return "INVOKESTATIC";

    return "invoke opcode unknown...";
  }
}
