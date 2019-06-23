package com.scotas.lucene.indexer;

import java.rmi.Remote;
import java.rmi.RemoteException;

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

public interface IndexUpdate  extends Remote {
  void sync(String dir, String [] deleted,  String [] inserted) throws RemoteException;
  void forceClose(String dir) throws RemoteException;
}
