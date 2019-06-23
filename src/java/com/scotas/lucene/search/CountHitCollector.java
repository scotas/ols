package com.scotas.lucene.search;

import java.io.IOException;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.index.AtomicReaderContext;

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

public class CountHitCollector extends Collector {
  int numHits = 0;
  OpenBitSet bits = null;
  private int base = 0;

  /**
   * An specialized collector to only count hits, used by countHits and facets
   * numBits should be reader.numDocs() to hold all specific bits for a particular
   * reader.
   * if numBits == 0 means that we only want to count hits
   * @param numBits
   */
  public CountHitCollector(int numBits) {
    if (numBits>0)
      bits = new OpenBitSet(numBits);
  }
  
  public void setScorer(Scorer scorer) throws IOException {
    // score is not needed by this collector
  }

  public final void collect(int doc) {
    numHits++; // got a new hit
    if (bits != null)
      bits.set(doc + base);
  }

  public void setNextReader(AtomicReaderContext context) throws IOException {
    this.base = context.docBase;
  }

  public boolean acceptsDocsOutOfOrder() {
    return true;
  }

  public int getNumHits() {
    return numHits;
  }

  public OpenBitSet getBits() {
    return bits;
  }
}
