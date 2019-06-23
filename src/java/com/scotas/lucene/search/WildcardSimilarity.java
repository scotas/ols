package com.scotas.lucene.search;

import java.util.Vector;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.Norm;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;

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

public class WildcardSimilarity extends Similarity {

  /**
   *
   */
  private static final long serialVersionUID = 3851926431995369662L;

  public float queryNorm(float sumOfSquaredWeights) {

    return 1.0f;
  }

  public float tf(float freq) {

    return freq;
  }

  public float sloppyFreq(int distance) {

    return 1.0f;
  }

  public float idf(Vector terms, IndexSearcher searcher) {

    return 1.0f;
  }

  public float idf(int docFreq, int numDocs) {

    return 1.0f;
  }

  public float coord(int overlap, int maxOverlap) {

    return 1.0f;
  }

    public float computeNorm(String field, FieldInvertState state) {
        return 0.0f;
    }

    @Override
    public void computeNorm(FieldInvertState state, Norm norm) {
    }

    @Override
    public Similarity.SimWeight computeWeight(float queryBoost,
                                              CollectionStatistics collectionStats,
                                              TermStatistics[] termStats) {
        return null;
    }

    @Override
    public Similarity.ExactSimScorer exactSimScorer(Similarity.SimWeight weight,
                                                    AtomicReaderContext context) {
        return null;
    }

    @Override
    public Similarity.SloppySimScorer sloppySimScorer(Similarity.SimWeight weight,
                                                      AtomicReaderContext context) {
        return null;
    }
}
