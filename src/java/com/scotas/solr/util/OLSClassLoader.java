package com.scotas.solr.util;


import com.scotas.protocol.ols.Handler;
import com.scotas.protocol.ols.OLSURLConnection;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Enumeration;
import java.util.Vector;


public class OLSClassLoader extends ClassLoader {
    public static final String baseResourcePkg = "/com/scotas/solr/";
    
    public OLSClassLoader() {
        super(OLSClassLoader.class.getClassLoader());
    }

    public OLSClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }


    @Override
    public java.io.InputStream getResourceAsStream(String name) {
        if (name.startsWith(baseResourcePkg)) {
            InputStream is;
            try {
                is = (new OLSURLConnection(new URL(null, "ols:" + name, new Handler()))).getInputStream();
            } catch (MalformedURLException e) {
                is = this.getParent().getResourceAsStream(name);
            } catch (IOException e) {
                is = this.getParent().getResourceAsStream(name);
            }
            return is;
        }
        return this.getParent().getResourceAsStream(name);
    }

    @Override
    protected URL findResource(String name) {
        URL url;
        if (name.startsWith(baseResourcePkg)) {
            try {
                url = new URL(null, "ols:" + name, new Handler());
            } catch (MalformedURLException e) {
                url = super.findResource(name);
            }
        } else
            url = super.findResource(name);
        return url;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> urlList = null;
        URL url = findResource(name);
        if (url != null) {
            Vector v = new Vector(1);
            v.add(0, url);
            urlList = v.elements();
        } else
            urlList = super.findResources(name);
        return urlList;
    }
}
