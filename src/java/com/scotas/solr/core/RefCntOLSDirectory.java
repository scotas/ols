package com.scotas.solr.core;

import com.scotas.lucene.store.OJVMDirectory;

import com.scotas.lucene.store.OJVMUtil;

import java.io.IOException;

import java.sql.SQLException;

import java.util.concurrent.atomic.AtomicInteger;

public class RefCntOLSDirectory extends OJVMDirectory {

  private final AtomicInteger refCount = new AtomicInteger();

  public RefCntOLSDirectory() {
    super();
    refCount.set(1);
  }

  public RefCntOLSDirectory(String prefix) throws IOException {
    super(prefix);
    refCount.set(1);
  }

  public void incRef() {
    refCount.incrementAndGet();
  }

  public void decRef() {
    if (refCount.decrementAndGet() == 1) {
            try {
                super.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
  }

  public final synchronized void close() {
    decRef();
  }

  public final synchronized void forceClose() {
    try {
        super.close();
        OJVMUtil.getConnection().rollback(); // Do not save uncommited changes
    } catch (IOException e) {
        e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public boolean isOpen() {
    return isOpen;
  }

}
