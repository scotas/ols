package com.scotas.solr.odci;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;


public class SolrIndexContext {
    String masterTable;
    float maxScore;
    long numFound;
    long startIndex;
    long endIndex;
    Hashtable scoreListCopy = null;
    Hashtable highlightListCopy = null;
    Hashtable mltListCopy = null;
    boolean storeScore = false;
    boolean storeHighlight = false;
    boolean storeMlt = false;
    Iterator iterator; 
    Iterator<Map.Entry<String, Object>> highlightIterator; 
    Iterator<Map.Entry<String, Object>> mltIterator; 
    String highlightCol;
    String mltCol;
    
    public SolrIndexContext() {
        super();
    }
    
    public String getMasterTable() {
      return this.masterTable;
    }
    
    public void setMasterTable(String masterTable) {
       this.masterTable = masterTable;
    }

    public void setMaxScore(float maxScore) {
        this.maxScore = maxScore;
    }

    public float getMaxScore() {
        return maxScore;
    }

    public void setNumFound(long numFound) {
        this.numFound = numFound;
    }

    public long getNumFound() {
        return numFound;
    }

    public void setStartIndex(long startIndex) {
        this.startIndex = startIndex;
    }

    public long getStartIndex() {
        return startIndex;
    }

    public void setScoreListCopy(Hashtable scoreListCopy) {
        this.scoreListCopy = scoreListCopy;
    }

    public Hashtable getScoreListCopy() {
        return scoreListCopy;
    }

    public void setStoreScore(boolean storeScore) {
        this.storeScore = storeScore;
    }

    public boolean isStoreScore() {
        return storeScore;
    }

    public void setEndIndex(long endIndex) {
        this.endIndex = endIndex;
    }

    public long getEndIndex() {
        return endIndex;
    }

    public void setIterator(Iterator iterator) {
        this.iterator = iterator;
    }

    public Iterator getIterator() {
        return iterator;
    }

    public void setHighlightIterator(Iterator<Map.Entry<String, Object>> highlightIterator) {
        this.highlightIterator = highlightIterator;
    }

    public Iterator getHighlightIterator() {
        return highlightIterator;
    }

    public void setHighlightCol(String highlightCol) {
        this.highlightCol = highlightCol;
    }

    public String getHighlightCol() {
        return highlightCol;
    }

    public void setStoreHighlight(boolean storeHighlight) {
        this.storeHighlight = storeHighlight;
    }

    public boolean isStoreHighlight() {
        return storeHighlight;
    }

    public void setHighlightListCopy(Hashtable highlightListCopy) {
        this.highlightListCopy = highlightListCopy;
    }

    public Hashtable getHighlightListCopy() {
        return highlightListCopy;
    }

    public void setMltIterator(Iterator<Map.Entry<String, Object>> mltIterator) {
        this.mltIterator = mltIterator;
    }

    public Iterator getMltIterator() {
        return mltIterator;
    }

    public void setMltCol(String mltCol) {
        this.mltCol = mltCol;
    }

    public String getMltCol() {
        return mltCol;
    }

    public void setStoreMlt(boolean storeMlt) {
        this.storeMlt = storeMlt;
    }

    public boolean isStoreMlt() {
        return storeMlt;
    }

    public void setMltListCopy(Hashtable mltListCopy) {
        this.mltListCopy = mltListCopy;
    }

    public Hashtable getMltListCopy() {
        return mltListCopy;
    }
}
