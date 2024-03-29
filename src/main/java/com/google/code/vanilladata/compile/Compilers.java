package com.google.code.vanilladata.compile;

import com.google.code.vanilladata.core.io.IOUtils;
import com.sun.tools.javac.api.JavacTool;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * This class support loading and debugging Java Classes dynamically.
 */
public class Compilers {
    public static final boolean DEBUGGING = ManagementFactory.getRuntimeMXBean().getInputArguments().contains("-Xdebug");

    private static final Method DEFINE_CLASS_METHOD;
    public static final CachedCompiler CACHED_COMPILER = new CachedCompiler(null, null);

    static {
        try {
            DEFINE_CLASS_METHOD = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            DEFINE_CLASS_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static final String JAVA_CLASS_PATH = "java.class.path";

    static JavaCompiler s_compiler;
    static StandardJavaFileManager s_standardJavaFileManager;
    static MyJavaFileManager s_fileManager;

    static void reset() {
        s_compiler = ToolProvider.getSystemJavaCompiler();
        if (s_compiler == null)
            s_compiler = JavacTool.create();

        s_standardJavaFileManager = s_compiler.getStandardFileManager(null, null, null);
        s_fileManager = new MyJavaFileManager(s_standardJavaFileManager);
    }

    static {
        reset();
    }

    private Compilers() {
    }

    /**
     * Load a java class file from the classpath or local file system.
     *
     * @param className    expected class name of the outer class.
     * @param resourceName as the full file name with extension.
     * @return the outer class loaded.
     * @throws IOException            the resource could not be loaded.
     * @throws ClassNotFoundException the class name didn't match or failed to initialise.
     */
    public static Class loadFromResource(String className, String resourceName) throws IOException, ClassNotFoundException {
        return loadFromJava(className, IOUtils.readText(resourceName));
    }

    /**
     * Load a java class file from the classpath or local file system.
     *
     * @param classLoader  to load class into.
     * @param className    expected class name of the outer class.
     * @param resourceName as the full file name with extension.
     * @return the outer class loaded.
     * @throws IOException            the resource could not be loaded.
     * @throws ClassNotFoundException the class name didn't match or failed to initialise.
     */
    public static Class loadFromResource(ClassLoader classLoader, String className, String resourceName) throws IOException, ClassNotFoundException {
        return loadFromJava(classLoader, className, IOUtils.readText(resourceName));
    }

    /**
     * Load a java class from text.
     *
     * @param className expected class name of the outer class.
     * @param javaCode  to compile and load.
     * @return the outer class loaded.
     * @throws ClassNotFoundException the class name didn't match or failed to initialise.
     */
    public static Class loadFromJava(String className, String javaCode) throws ClassNotFoundException {
        return CACHED_COMPILER.loadFromJava(Compilers.class.getClassLoader(), className, javaCode);
    }

    /**
     * Load a java class from text.
     *
     * @param classLoader to load class into.
     * @param className   expected class name of the outer class.
     * @param javaCode    to compile and load.
     * @return the outer class loaded.
     * @throws ClassNotFoundException the class name didn't match or failed to initialise.
     */
    public static Class loadFromJava(ClassLoader classLoader, String className, String javaCode) throws ClassNotFoundException {
        return CACHED_COMPILER.loadFromJava(classLoader, className, javaCode);
    }

    /**
     * Add a directory to the class path for compiling.  This can be required with custom
     *
     * @param dir to add.
     * @return whether the directory was found, if not it is not added either.
     */
    public static boolean addClassPath(String dir) {
        File file = new File(dir);
        if (file.exists()) {
            String path;
            try {
                path = file.getCanonicalPath();
            } catch (IOException ignored) {
                path = file.getAbsolutePath();
            }
            if (!Arrays.asList(System.getProperty(JAVA_CLASS_PATH).split(File.pathSeparator)).contains(path))
                System.setProperty(JAVA_CLASS_PATH, System.getProperty(JAVA_CLASS_PATH) + File.pathSeparator + path);
        } else {
            return false;
        }
        reset();
        return true;
    }

    /**
     * Define a class for byte code.
     *
     * @param className expected to load.
     * @param bytes     of the byte code.
     */
    public static void defineClass(String className, byte[] bytes) {
        defineClass(Thread.currentThread().getContextClassLoader(), className, bytes);
    }

    /**
     * Define a class for byte code.
     *
     * @param classLoader to load the class into.
     * @param className   expected to load.
     * @param bytes       of the byte code.
     */
    public static void defineClass(ClassLoader classLoader, String className, byte[] bytes) {
        try {
            DEFINE_CLASS_METHOD.invoke(classLoader, className, bytes, 0, bytes.length);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new AssertionError(e.getCause());
        }
    }
}
