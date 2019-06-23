package com.scotas.protocol.ols;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class OLSURLStreamHandlerFactory implements URLStreamHandlerFactory {
    static final Handler handler = new Handler();
    
    public OLSURLStreamHandlerFactory() {
        super();
    }

    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (protocol.equalsIgnoreCase("ols"))
            return handler;
        else
            return null;
    }
}
