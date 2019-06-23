package com.scotas.protocol.ols;


import com.scotas.lucene.store.OJVMUtil;
import com.scotas.lucene.util.StringUtils;
import com.scotas.solr.util.OLSClassLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.URLConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;


public class OLSURLConnection extends URLConnection {
    private static final String SELECT_STATEMENT =
        "SELECT FILE_SIZE,DATA FROM %IDX%$T WHERE NAME=?";

    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = OLSURLConnection.class.getName();

    /**
     * Java Util Logging variables and default values
     */
    private static Logger logger = Logger.getLogger(CLASS_NAME);

    private static Connection conn = null;
    private String name;
    private String res;
    private String prefix;
    private int length = 0;

    /**
     * @param url
     */
    public OLSURLConnection(URL url) {
        super(url);
        this.name = url.getPath();
        this.prefix = name.substring(OLSClassLoader.baseResourcePkg.length());
        prefix =
                prefix.substring(0, prefix.indexOf("/conf/")).replace('/', '.');
        this.res = name.substring(name.lastIndexOf('/') + 1);
        logger.info("Return Connection to file: " + this.name + " prefix: " +
                    this.prefix + " res: " + this.res);
    }

    /**
     * @throws IOException
     */
    public void connect() throws IOException {
        try {
            if (conn == null)
                conn = OJVMUtil.getConnection();
        } catch (SQLException e) {
            IOException ie =
                new IOException("Can get the OJVM Connection: " + e.getLocalizedMessage());
            logger.throwing("Can get the OJVM Connection",
                            e.getLocalizedMessage(), ie);
            throw ie;
        }
        logger.info("return");
    }

    /**
     * @return
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        if (conn == null)
            connect();
        try {
            stmt = conn.prepareStatement(StringUtils.replace(SELECT_STATEMENT, "%IDX%",
                                          this.prefix));
            stmt.setString(1, this.res);
            rs = stmt.executeQuery();
            if (rs.next()) {
                this.length = rs.getInt(1);
                InputStream is = rs.getBlob(2).getBinaryStream();
                logger.info("Found file: " + this.name + " prefix: " +
                            this.prefix + " res: " + this.res);
                logger.info("returning " + is);
                return is;
            } else {
                FileNotFoundException t =
                    new FileNotFoundException("name: " + this.name +
                                              " prefix: " + this.prefix +
                                              " res: " + this.res);
                logger.throwing(this.CLASS_NAME, "getInputStream", t);
                throw t;
            }
        } catch (SQLException e) {
            IOException ie =
                new IOException("SQLException when trying to load: " +
                                this.res + " prefix: " + this.prefix +
                                " error: " + e.getLocalizedMessage());
            logger.throwing(this.CLASS_NAME, "getInputStream", ie);
            throw ie;
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
    }
}
