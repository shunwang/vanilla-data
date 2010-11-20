package com.google.code.vanilladata.compile;

import com.google.code.vanilladata.core.io.IOUtils;
import com.google.code.vanilladata.core.log.Log;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings({"StaticNonFinalField"})
public class CachedCompiler {
    private static final Log LOGGER = Log.log(CachedCompiler.class);
    private final File sourceDir;
    private final File classDir;

    public CachedCompiler(File sourceDir, File classDir) {
        this.sourceDir = sourceDir;
        this.classDir = classDir;
    }

    public Class loadFromJava(String className, String javaCode) throws ClassNotFoundException {
        return loadFromJava(getClass().getClassLoader(), className, javaCode);
    }

    public Map<String, byte[]> compileFromJava(String className, String javaCode) {
        Iterable<? extends JavaFileObject> compilationUnits;
        if (sourceDir != null) {
            String filename = className.replaceAll("\\.", '\\' + File.separator) + ".java";
            File file = new File(sourceDir, filename);
            IOUtils.writeText(file, javaCode);
            compilationUnits = Compilers.s_standardJavaFileManager.getJavaFileObjects(file);
        } else {
            compilationUnits = Arrays.asList(new JavaSourceFromString(className, javaCode));
        }
        // reuse the same file manager to allow caching of jar files
        Compilers.s_compiler.getTask(null, Compilers.s_fileManager, null, null, null, compilationUnits).call();
        return Compilers.s_fileManager.getAllBuffers();
    }

    public Class loadFromJava(ClassLoader classLoader, String className, String javaCode) throws ClassNotFoundException {
        for (Map.Entry<String, byte[]> entry : compileFromJava(className, javaCode).entrySet()) {
            String className2 = entry.getKey();
            byte[] bytes = entry.getValue();
            if (classDir != null) {
                String filename = className2.replaceAll("\\.", '\\' + File.separator) + ".class";
                boolean changed = IOUtils.writeBytes(new File(classDir, filename), bytes);
                if (changed)
                    LOGGER.info("Updated " + className2 + " in " + classDir);
            }
            Compilers.defineClass(classLoader, className2, bytes);
        }
        Compilers.s_fileManager.clearBuffers();
        return classLoader.loadClass(className);
    }

    public static void close() {
        try {
            Compilers.s_fileManager.close();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
