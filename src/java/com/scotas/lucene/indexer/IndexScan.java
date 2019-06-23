package com.scotas.lucene.indexer;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.Hashtable;
import java.math.BigDecimal;


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

public interface IndexScan extends Remote {
    int start(String dir, String qry, String sort, boolean storeScore, boolean firstRowHint) throws RemoteException;
    String[] fetch(int ctx, java.math.BigDecimal nrows) throws RemoteException;
    Query getQuery(int ctx) throws RemoteException;
    Hashtable getScoreList(int ctx) throws RemoteException;
    TopDocs getHits(int ctx) throws RemoteException;
    BigDecimal getScoreValue(int ctx, int docId) throws RemoteException;
    void close(int ctx) throws RemoteException;
    void refreshCache(String dir) throws RemoteException;
    int getNumHits(String dir, String cmpval) throws RemoteException;
}
