package com.asmtest

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter;


public class AsmTransform extends Transform {
    public AsmTransform() {

    }

    @Override
     String getName() {
        return "asm-test"
    }

    @Override
     void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);

        transformInvocation.inputs.each { TransformInput input ->

            input.directoryInputs.each { DirectoryInput directoryInput ->


                Map<String, ScanData> dataMap = new HashMap<>();

                directoryInput.file.eachFileRecurse { File file ->
                    //  println("---------------:" + file.getAbsoluteFile())
                    if (!isFilter(file.getAbsolutePath())) {
                        writeCode(file, dataMap)
                    }
                }

                dataMap.each { key, value ->
                    /*           println("---------------dataMap file------------" + value.filePath)
                               File file = new File(value.filePath)
                               def optClass = new File(file.getParent(), file.name + ".opt")

                               FileInputStream inputStreamFile = new FileInputStream(file)
                               FileOutputStream outputStream = new FileOutputStream(optClass)

                               outputStream.write(value.code)
                               inputStreamFile.close()
                               outputStream.close()
                               if (file.exists()) {

                                   def isSuccess = file.delete()
                                   println("---------------delete file------------" + "file.delete isSuccess:" + isSuccess+"==" + file.exists())

                               }
                               optClass.renameTo(file)
                               inputStreamFile.close()*/

                }


                /*       directoryInput.changedFiles.each { file ->

                           if(file.value.name())
                           println("---------------:" + file.key.getAbsoluteFile())
                           writeCode(file.key)
                       }*/

                File dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                // 处理完后拷到目标文件

                FileUtils.copyDirectory(directoryInput.file, dest)
                println("-------处理完后拷到目标文件--------")
            }


        }

    }

    private static boolean isFilter(String path) {

        if (!path.endsWith(".class")) {
            return true
        }
        if (path.endsWith("R.class")) {
            return true
        }
        if (path.contains("android\\support")) {
            //  println("------------------android.support")
            return true
        }
        return false

    }

    @Override
     Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
     Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
     boolean isIncremental() {
        return true
    }

    private void writeCode(File file, Map<String, ScanData> dataMap) {

        if (!file.exists()) {
            return
        }

        def inputStream = file.newInputStream()
        ClassReader classReader = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        ScanClassVisitor cv = new ScanClassVisitor(Opcodes.ASM5, cw)
        classReader.accept(cv, ClassReader.EXPAND_FRAMES)
        byte[] code = cw.toByteArray()
        inputStream.close()
        if (cv.isFind) {

            /*     ScanData scanData = new ScanData()
                 scanData.setCode(code)
                 scanData.setFilePath(file.getAbsolutePath())
                 dataMap.put(file.getAbsolutePath(), scanData)*/


            def optClass = new File(file.getParent(), file.name + ".opt")

            FileInputStream inputStreamFile = new FileInputStream(file)
            FileOutputStream outputStream = new FileOutputStream(optClass)

            outputStream.write(code)
            inputStreamFile.close()
            outputStream.close()
            if (file.exists()) {

                def isSuccess = file.delete()
                println("---------------delete file------------" + "file.delete isSuccess:" + isSuccess + "==" + file.exists())

            }
            optClass.renameTo(file)
            inputStreamFile.close()

        }


    }

    class ScanClassVisitor extends ClassVisitor {

        ScanClassVisitor(int api, ClassVisitor cv) {
            super(api, cv)

        }
        def isFind = false
        private boolean isFieldPresent;
        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

           /* if (name.equals("remove") || name.equals("remove2")) {
                isFind = true
                //返回空可移除一个方法
                return null
            }
*/
            MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
            MyAdviceAdapter myAdviceAdapter = new MyAdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, desc)
            println("-------visitMethod--------" + name + myAdviceAdapter.getIsFind())

            return myAdviceAdapter
        }

        @Override
        FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {

            if (name.equals("_start")) {
                isFieldPresent = true;
            }
            return super.visitField(access, name, desc, signature, value)
        }

        @Override
        void visitEnd() {

            if(!isFieldPresent){
                isFind = true
                println("-----------------------------添加一个字段-----visitEnd-------------------:"+Type.getDescriptor(String.class) )
                //添加一个字段
                FieldVisitor fieldVisitor = cv.visitField(0,"_start",Type.getDescriptor(String.class),null,null)
                fieldVisitor.visitEnd()
            }



            super.visitEnd()
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {

            println("-------visitAnnotation--------" + desc)
            return super.visitAnnotation(desc, visible)
        }

        def getIsFind() {
            return isFind
        }

    }

    class MyAdviceAdapter extends AdviceAdapter {
        int startTime
        def isFind = false

        def getIsFind() {
            return isFind
        }

        protected MyAdviceAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc)
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            //判断注解
            if (desc.contains("RemoveMethod")) {
                isFind = true
                println("-------MyAdviceAdapter--------" + desc + getIsFind())

            }
            return super.visitAnnotation(desc, visible)
        }


        /**
         * 进入方法
         */
        @Override
        protected void onMethodEnter() {
            super.onMethodEnter()

            /*        if (isFind) {
                        startTime = newLocal(Type.LONG_TYPE)
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
                        mv.visitVarInsn(LSTORE, startTime)
                    }
        */
        }
        /**
         * 退出方法
         */
        @Override
        protected void onMethodExit(int opcode) {
            super.onMethodExit(opcode)
        }

        @Override
        void visitEnd() {
            super.visitEnd()


        }
    }
}
