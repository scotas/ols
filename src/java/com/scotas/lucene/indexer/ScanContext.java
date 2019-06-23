package com.scotas.lucene.indexer;

import java.util.Hashtable;

import java.util.logging.Level;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;


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

public class ScanContext {
    Level logLevel = Level.WARNING;
    float scoreNorm = 1.0f;
    Hashtable scoreList = null;
    TopDocs hits = null;
    Query query = null;
    long startTime = 0;
    int startIndex = 0;
    int endIndex = 0;
    boolean storeScore = false;
    String dir;
    
    public ScanContext() {
        scoreList = null;
    }

    public void setStoreScore(boolean b) {
        this.storeScore = b;
    }

    public boolean isStoreScore() {
        return this.storeScore;
    }

    public void setScoreList(Hashtable scoreList) {
        this.scoreList = scoreList;
    }

    public Hashtable getScoreList() {
        return scoreList;
    }

    public void setHits(TopDocs param) {
        this.hits = param;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartIndex(int param) {
        this.startIndex = param;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setEndIndex(int param) {
        this.endIndex = param;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public TopDocs getHits() {
        return hits;
    }

    public void setQuery(Query reWrited) {
        this.query = reWrited;
    }

    public Query getQuery() {
        return query;
    }

    public void setScoreNorm(float scoreNorm) {
        this.scoreNorm = scoreNorm;
    }

    public float getScoreNorm() {
        return scoreNorm;
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getDir() {
        return dir;
    }
}

