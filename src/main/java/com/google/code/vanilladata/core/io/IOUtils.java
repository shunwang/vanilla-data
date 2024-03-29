package com.google.code.vanilladata.core.io;

import com.google.code.vanilladata.core.log.Log;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;


public class IOUtils {
    private static final Log LOG = Log.log(IOUtils.class);
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private IOUtils() {
    }

    public static String readText(String resourceName) throws IOException {
        if (resourceName.startsWith("="))
            return resourceName.substring(1);
        StringWriter sw = new StringWriter();
        Reader isr = new InputStreamReader(getInputStream(resourceName), UTF_8);
        try {
            char[] chars = new char[8 * 1024];
            int len;
            while ((len = isr.read(chars)) > 0)
                sw.write(chars, 0, len);
        } finally {
            close(isr);
        }
        return sw.toString();
    }

    public static String readText(File file) {
        byte[] bytes = readBytes(file);
        return decodeUTF8(bytes);
    }

    private static String decodeUTF8(byte[] bytes) {
        try {
            return new String(bytes, UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings({"ReturnOfNull"})
    public static byte[] readBytes(File file) {
        if (!file.exists()) return null;
        long len = file.length();
        if (len > Runtime.getRuntime().totalMemory() / 10)
            throw new IllegalStateException("Attempted to read large file " + file + " was " + len + " bytes.");
        byte[] bytes = new byte[(int) len];
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(bytes);
        } catch (IOException e) {
            close(dis);
            LOG.error("Unable to read " + file, e);
            throw new IllegalStateException("Unable to read file " + file, e);
        }

        return bytes;
    }

    public static void close(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException e) {
                if (LOG.isTraceEnabled()) LOG.trace("Failed to close " + closeable, e);
            }
    }

    public static boolean writeText(File file, String text) {
        return writeBytes(file, encodeUTF8(text));
    }

    private static byte[] encodeUTF8(String text) {
        try {
            return text.getBytes(UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static boolean writeBytes(File file, byte[] bytes) {
        File parentDir = file.getParentFile();
        if (!parentDir.isDirectory() && !parentDir.mkdirs())
            throw new IllegalStateException("Unable to create directory " + parentDir);
        // only write to disk if it has changed.
        File bak = null;
        if (file.exists()) {
            byte[] bytes2 = readBytes(file);
            if (Arrays.equals(bytes, bytes2))
                return false;
            bak = new File(parentDir, file.getName() + ".bak");
            file.renameTo(bak);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes);
        } catch (IOException e) {
            close(fos);
            LOG.error("Unable to write " + file + " as " + decodeUTF8(bytes), e);
            file.delete();
            if (bak != null)
                bak.renameTo(file);
            throw new IllegalStateException("Unable to write " + file, e);
        }
        return true;
    }

    public static InputStream getInputStream(String filename) throws IOException {
        InputStream is = getInputStream0(filename);
        if (filename.endsWith(".gz"))
            return new GZIPInputStream(is);
        return is;
    }

    private static InputStream getInputStream0(String filename) throws IOException {
        if (filename.length() == 0) throw new IllegalArgumentException("The file name cannot be empty.");
        if (filename.charAt(0) == '=') return new ByteArrayInputStream(encodeUTF8(filename.substring(1)));
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = contextClassLoader.getResourceAsStream(filename);
        if (is != null) return is;
        InputStream is2 = contextClassLoader.getResourceAsStream('/' + filename);
        if (is2 != null) return is2;
        try {
            return new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            try {
                return new GZIPInputStream(new FileInputStream(filename + ".gz"));
            } catch (FileNotFoundException e1) {
                throw e;
            }
        }
    }

    public static LineNumberReader getLineNumberReader(String filename) throws IOException {
        return new LineNumberReader(new InputStreamReader(getInputStream(filename), UTF_8));
    }

    public static File findFile(String path) throws FileNotFoundException {
        File file = new File(path);
        do {
            if (file.exists()) return file;
            String path2 = file.getPath();
            int pos = path2.indexOf(File.separator);
            if (pos < 0)
                throw new FileNotFoundException("Unable to derive the directory required from " + path);
            file = new File(path2.substring(pos + 1));
        } while (true);
    }
}
