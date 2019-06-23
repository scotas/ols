package com.scotas.lucene.indexer;

import java.io.IOException;

import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import com.scotas.lucene.store.OJVMDirectory;
import com.scotas.lucene.util.cache.Cache;
import com.scotas.lucene.util.cache.SimpleLRUCache;


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

public class Entry {
    /**
     * Which OJVMDirectory represents
     */
    private OJVMDirectory dir;

    /**
     * Reader used to access to the directory, one for each directory
     */
    private IndexReader reader;

    /**
     * Searcher used to search, one for each directory
     */
    private IndexSearcher search;

    /**
     * this counter is used to check if this directory was updated by another
     * process, any writer activity will increment a counter
     * @see com.scotas.lucene.store.OJVMDirectory#getCachedDirectory
     */
    private long updateCount;

    /**
     * List of active filters for these reader/search
     * Filter are used to implement lcontains() operator outside the where
     * @see com.scotas.lucene.indexer.LuceneDomainIndex TextContains static
     * function
     */
    private HashMap activeFilters;

    /**
     * Analyzer used with these reader/search
     */
    private Analyzer analyzer;

    /**
     * List of cached hits indexed by query string
     * if not other process invalidate this entry to a same query string
     * same list of cached hits
     */
    private HashMap activeHits;

    /**
     * Stored pre-computed TermInfoQueue Arrays information
     * used HighFreqTerms class
     */
    private HashMap cachedTermInfoArrays;
    
    /**
     * If no writer process invalidate this entry asociation between lucene
     * document id and Oracle rowid is not afected
     */
    private Cache cachedRowIds;

    /**
     * 
     */
    private QueryParser parser;

    public Entry(OJVMDirectory dir, IndexReader reader, IndexSearcher search,
                 Analyzer analyzer, QueryParser parser, long updateCount) {
        this.dir = dir;
        this.reader = reader;
        this.search = search;
        this.updateCount = updateCount;
        this.activeFilters = new HashMap();
        this.activeHits = new HashMap();
        this.analyzer = analyzer;
        Parameters par = dir.getParameters();
        int cachedRowIdSize = Integer.parseInt(par.getParameter("CachedRowIdSize","10000"));
        this.cachedRowIds = new SimpleLRUCache(cachedRowIdSize);
        this.cachedTermInfoArrays = new HashMap();
        this.parser = parser;
    }

    /** Two of these are equal iff they reference the same field and type. */
    public boolean equals(Object o) {
        if (o instanceof Entry) {
            Entry other = (Entry)o;
            if (other.dir == dir && other.search == search)
                return true;
        }
        return false;
    }

    /** Composes a hashcode based on the field and type. */
    public int hashCode() {
        return (search == null ? 0 : search.hashCode()) ^
            (dir == null ? 0 : dir.hashCode());
    }

    public OJVMDirectory getDirectory() {
        return this.dir;
    }

    public IndexSearcher getSeacher() {
        return this.search;
    }

    public long getUpdateCount() {
        return this.updateCount;
    }

    public void addFilter(String qry, Filter f) {
        this.activeFilters.put(qry, f);
    }

    public Filter getFilter(String qry) {
        return (Filter)this.activeFilters.get(qry);
    }

    public void setHits(String qry, TopDocs hits) {
        this.activeHits.put(qry, hits);
    }

    public TopDocs getHits(String qry) {
        return (TopDocs)this.activeHits.get(qry);
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Analyzer getAnalyzer() {
        return this.analyzer;
    }

    public void close() throws IOException {
        //System.out.println("Closing cachedSearcher entry "+this);
        this.cachedTermInfoArrays.clear();
        this.cachedRowIds.close();
        this.activeFilters.clear();
        this.activeHits.clear();
        this.reader.close();
        this.dir.close();
        // tell GC that free these resources
        this.search = null;
        this.reader = null;
        this.dir = null;
        this.activeFilters = null;
        this.activeHits = null;
        this.cachedRowIds = null;
        this.cachedTermInfoArrays = null;
    }

    public String toString() {
        StringBuffer result = new StringBuffer("Entry: ");
        result.append("dir: ").append(dir.hashCode()).append(" reader: ").append(reader.toString()).append(" searcher: ").append(search.toString()).append(" updateCount: ").append(updateCount);
        return result.toString();
    }

    public void setReader(IndexReader newreader) {
        this.reader = newreader;
    }

    public IndexReader getReader() {
        return reader;
    }

    public String getRowId(int doc) throws CorruptIndexException, IOException {
        String rowId = null;
        rowId = (String)this.cachedRowIds.get(new Integer(doc));
        if (rowId == null) {
           rowId = this.search.doc(doc).get("rowid");
           this.cachedRowIds.put(new Integer(doc), rowId);
           //System.out.println("caching rowid: "+rowId+" for doc: "+doc);
        }
        return rowId;
    }

  public void setCachedTermInfoArrays(HashMap cachedTermInfoArrays) {
    this.cachedTermInfoArrays = cachedTermInfoArrays;
  }

  public HashMap getCachedTermInfoArrays() {
    return cachedTermInfoArrays;
  }

  public QueryParser getParser() {
    return parser;
  }
}

