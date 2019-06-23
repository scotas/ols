package com.scotas.lucene.search.facets.ojvm;

import com.scotas.lucene.indexer.Entry;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.OpenBitSet;


/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class StoredCtx {
  private Entry entry;
  private String indexName;
  private String dfltColumn;
  private String qry;
  private long startTime = 0;
  private OpenBitSet baseBitSet = null;
  private QueryParser parser = null;
  
  public StoredCtx(String i, String q, Entry e, OpenBitSet b, QueryParser p, String c) {
    indexName = i;
    qry = q;
    entry = e;
    baseBitSet = b;
    parser = p;
    dfltColumn = c;
  }

  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  public String getIndexName() {
    return indexName;
  }

  public void setEntry(Entry entry) {
    this.entry = entry;
  }

  public Entry getEntry() {
    return entry;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setQry(String qry) {
    this.qry = qry;
  }

  public String getQry() {
    return qry;
  }

  public void setBaseBitSet(OpenBitSet baseBitSet) {
    this.baseBitSet = baseBitSet;
  }

  public OpenBitSet getBaseBitSet() {
    return baseBitSet;
  }

  public void setParser(QueryParser parser) {
    this.parser = parser;
  }

  public QueryParser getParser() {
    return parser;
  }

  public void setDfltColumn(String dfltColumn) {
    this.dfltColumn = dfltColumn;
  }

  public String getDfltColumn() {
    return dfltColumn;
  }
  }
