package com.scotas.solr.core;

import java.io.IOException;

import oracle.aurora.vm.OracleRuntime;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NRTCachingDirectory;

/**
 * Factory to instantiate {@link org.apache.lucene.store.NRTCachingDirectory}
 */
public class NRTCachingDirectoryFactory extends OLSDirectoryFactory {

  @Override
  protected Directory create(String path) throws IOException {
    double mbFree = ((OracleRuntime.getJavaPoolSize() / 100) *
                                   50) / (1024 * 1024);
    Directory dir = super.create(path);
    return new NRTCachingDirectory(dir, mbFree/12, mbFree);
  }

}
