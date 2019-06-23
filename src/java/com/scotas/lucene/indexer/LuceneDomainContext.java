package com.scotas.lucene.indexer;


import java.rmi.RemoteException;

import java.util.Hashtable;

import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;


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

public class LuceneDomainContext {
    Float scoreNormCopy = null;
    Hashtable scoreListCopy = null;

    int scanContext = 0;
    boolean storeScore = false;

    Entry entry = null;
    String column = null;
    Highlighter highlighter = null;
    int maxNumFragmentsRequired = 4;
    String fragmentSeparator = "...";
    String masterTable = "";
    IndexScan indexScan;
    
    public LuceneDomainContext() {
    }

    public void setStoreScore(boolean b) {
        this.storeScore = b;
    }

    public boolean isStoreScore() {
        return this.storeScore;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getColumn() {
        return column;
    }

    public void setHighlighter(Highlighter highlighter) {
        this.highlighter = highlighter;
    }

    public Highlighter getHighlighter() {
        return highlighter;
    }

    public void setMaxNumFragmentsRequired(int maxNumFragmentsRequired) {
        this.maxNumFragmentsRequired = maxNumFragmentsRequired;
    }

    public int getMaxNumFragmentsRequired() {
        return maxNumFragmentsRequired;
    }

    public void setFragmentSeparator(String fragmentSeparator) {
        this.fragmentSeparator = fragmentSeparator;
    }

    public String getFragmentSeparator() {
        return fragmentSeparator;
    }

    public void setScanContext(int scanContext) {
        this.scanContext = scanContext;
    }

    public int getScanContext() {
        return scanContext;
    }

    /**
     * @return Hashtable with the association between rowid->score
     * @throws RemoteException
     */
    public Hashtable getScoreList() throws RemoteException {
        return scoreListCopy;
    }

    /**
     * @return a float value with the computed score norm
     * @throws RemoteException
     */
    public float getScoreNorm() throws RemoteException {
        return scoreNormCopy.floatValue();
    }

    /**
     * During fetch process if storeScore is enable gets from remoteSearcher
     * a Hashtable with association between rowid<->docId and store a copy using
     * this setter
     * @param scoreListCopy
     */
    public void setScoreListCopy(Hashtable scoreListCopy) {
        this.scoreListCopy = scoreListCopy;
    }

    /**
     * During fetch process if storeScore is enable gets from remoteSearcher
     * a Float with the scoreNorm value and store it with
     * this setter
     * @param scoreNormCopy
     */
    public void setScoreNormCopy(Float scoreNormCopy) {
        this.scoreNormCopy = scoreNormCopy;
    }

    /**
     * Table associated with this index context, used for filtering deleted rowids
     * @param masterTable
     */
    public void setMasterTable(String masterTable) {
        this.masterTable = masterTable;
    }

    /**
     * @return a table associated with this index context, used for filtering deleted rowids
     */
    public String getMasterTable() {
        return masterTable;
    }

    /**
     * @param indexScan
     */
    public void setIndexScan(IndexScan indexScan) {
        this.indexScan = indexScan;
    }

    /**
     * @return
     */
    public IndexScan getIndexScan() {
        return indexScan;
    }
}

