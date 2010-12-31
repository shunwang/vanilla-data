package com.google.code.vanilladata.compile;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/* A file object used to represent source coming from a string.
         */

class JavaSourceFromString extends SimpleJavaFileObject {
    /**
     * The source code of this "file".
     */
    private final String code;

    /**
     * Constructs a new JavaSourceFromString.
     *
     * @param name the name of the compilation unit represented by this file object
     * @param code the source code for the compilation unit represented by this file object
     */
    JavaSourceFromString(String name, String code) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
                Kind.SOURCE);
        this.code = code;
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}
