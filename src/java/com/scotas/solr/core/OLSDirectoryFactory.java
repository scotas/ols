package com.scotas.solr.core;


import com.scotas.lucene.store.OJVMDirectory;
import com.scotas.solr.util.OLSClassLoader;

import java.io.IOException;

import java.sql.Connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.store.Directory;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CachingDirectoryFactory;
import org.apache.solr.core.CachingDirectoryFactory.CloseListener;
import org.apache.solr.core.DirectoryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Directory provider for using lucene RAMDirectory
 */
public class OLSDirectoryFactory extends DirectoryFactory {

    protected class CacheValue {
      public Directory directory;
      public int refCnt = 1;
      public String path;
      public boolean doneWithDir = false;
      public String toString() {
        return "CachedDir<<" + directory.toString() + ";refCount=" + refCnt + ";path=" + path + ";done=" + doneWithDir + ">>";
      }
    }
    
    private static Logger log = LoggerFactory
        .getLogger(OLSDirectoryFactory.class);
    
    private Connection conn = null;

    protected Map<String,CacheValue> byPathCache = new HashMap<String,CacheValue>();
    
    protected Map<Directory,CacheValue> byDirectoryCache = new HashMap<Directory,CacheValue>();
    
    protected Map<Directory,List<CloseListener>> closeListeners = new HashMap<Directory,List<CloseListener>>();
    
    @Override
    protected Directory create(String path) throws IOException {
        log.info("create(" + path + ")");
        OJVMDirectory dir = OJVMDirectory.getDirectory(path);
        this.conn = dir.getConnection();
        return dir;
    }

    @Override
    public final Directory get(String path, String rawLockType, boolean forceNew)
        throws IOException {
        log.info("get(" + path + "," + rawLockType + "," + forceNew + ")");
        String prefix = getPrefix(path);
        synchronized (this) {
          CacheValue cacheValue = byPathCache.get(prefix);
          Directory directory = null;
          if (cacheValue != null) {
            directory = cacheValue.directory;
            if (forceNew) {
              cacheValue.doneWithDir = true;
              if (cacheValue.refCnt == 0) {
                close(cacheValue.directory);
              }
            }
          }
          
          if (directory == null || forceNew) {
            directory = create(prefix);
            CacheValue newCacheValue = new CacheValue();
            newCacheValue.directory = directory;
            newCacheValue.path = prefix;
            
            byDirectoryCache.put(directory, newCacheValue);
            byPathCache.put(prefix, newCacheValue);
            log.info("return new directory for " + prefix + " forceNew:" + forceNew + "cachedValue:" + newCacheValue);
          } else {
            cacheValue.refCnt++;
            log.info("return openned directory for " + prefix + "cachedValue:" + cacheValue);
          }
          
          return directory;
        }
    }

    private static String getPrefix(String path) {
        if (!path.startsWith(OLSClassLoader.baseResourcePkg))
            throw new IllegalArgumentException("Directory dir not start with: " +
                                               OLSClassLoader.baseResourcePkg);
        String prefix =
            path.substring(OLSClassLoader.baseResourcePkg.length());
        prefix = prefix.substring(0, prefix.indexOf("/data/index"));
        return prefix.replace('/', '.');
    }

    /**
     * @param path (/com/scotas/solr/LUCENE/TEST_IDX/data/index)
     * @return
     */
    public boolean exists(String path) {
        log.info("exists(" + path + ")");
        return true;
    }

    @Override
    public void doneWithDirectory(Directory directory) throws IOException {
      log.info("doneWithDirectory(" + directory + ")");
      synchronized (this) {
        CacheValue cacheValue = byDirectoryCache.get(directory);
        if (cacheValue == null) {
          throw new IllegalArgumentException("Unknown directory: " + directory
              + " " + byDirectoryCache);
        }
        cacheValue.doneWithDir = true;
        if (cacheValue.refCnt == 0) {
          cacheValue.refCnt++; // this will go back to 0 in close
          close(directory);
        }
      }
    }

    @Override
    public void addCloseListener(Directory dir, CachingDirectoryFactory.CloseListener closeListener) {
      log.info("addCloseListener(" + dir + "," + closeListener + ")");
      synchronized (this) {
        if (!byDirectoryCache.containsKey(dir)) {
          throw new IllegalArgumentException("Unknown directory: " + dir
              + " " + byDirectoryCache);
        }
        List<CloseListener> listeners = closeListeners.get(dir);
        if (listeners == null) {
          listeners = new ArrayList<CloseListener>();
          closeListeners.put(dir, listeners);
        }
        listeners.add(closeListener);
        
        closeListeners.put(dir, listeners);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.solr.core.DirectoryFactory#close()
     */
    @Override
    public void close() throws IOException {
      log.info("close()");
      synchronized (this) {
        for (CacheValue val : byDirectoryCache.values()) {
          try {
            val.directory.close();
          } catch (Throwable t) {
            SolrException.log(log, "Error closing directory", t);
          }
        }
        byDirectoryCache.clear();
        byPathCache.clear();
      }
    }

    private void close(Directory directory) throws IOException {
      log.info("close(" + directory + ")");
      synchronized (this) {
        CacheValue cacheValue = byDirectoryCache.get(directory);
        if (cacheValue == null) {
          throw new IllegalArgumentException("Unknown directory: " + directory
              + " " + byDirectoryCache);
        }

        log.debug("Closing: {}", cacheValue);

        cacheValue.refCnt--;
        if (cacheValue.refCnt == 0 && cacheValue.doneWithDir) {
          log.info("Closing directory:" + cacheValue.path);
          directory.close();
          byDirectoryCache.remove(directory);
          byPathCache.remove(cacheValue.path);
          List<CloseListener> listeners = closeListeners.remove(directory);
          if (listeners != null) {
            for (CloseListener listener : listeners) {
              listener.onClose();
            }
            closeListeners.remove(directory);
          }
        }
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.solr.core.DirectoryFactory#get(java.lang.String,
     * java.lang.String)
     */
    @Override
    public final Directory get(String path, String rawLockType)
        throws IOException {
      return get(path, rawLockType, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.solr.core.DirectoryFactory#incRef(org.apache.lucene.store.Directory
     * )
     */
    public void incRef(Directory directory) {
      log.info("incRef(" + directory + ")");
      synchronized (this) {
        CacheValue cacheValue = byDirectoryCache.get(directory);
        if (cacheValue == null) {
          throw new IllegalArgumentException("Unknown directory: " + directory);
        }
        
        cacheValue.refCnt++;
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.solr.core.DirectoryFactory#release(org.apache.lucene.store.Directory
     * )
     */
    @Override
    public void release(Directory directory) throws IOException {
      log.info("release(" + directory + ")");
      if (directory == null) {
        throw new NullPointerException();
      }
      close(directory);
    }

    public void init(NamedList args) {}

    public Connection getConnection() {
        return this.conn;
    }
}
