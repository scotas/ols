package org.apache.lucene.analysis;

import com.scotas.lucene.indexer.LuceneDomainIndex;

import java.io.Reader;

import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;


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

public class NGramAnalyzer extends Analyzer {
    public static final int DEFAULT_MIN_NGRAM_SIZE = 1;
    public static final int DEFAULT_MAX_NGRAM_SIZE = 2;

    public NGramAnalyzer() {
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName,
                                                              Reader reader) {
        if (fieldName == null)
            throw new IllegalArgumentException("fieldName must not be null");
        if (reader == null)
            throw new IllegalArgumentException("reader must not be null");

        Tokenizer tokenizer =
            new StandardTokenizer(LuceneDomainIndex.LUCENE_COMPAT_VERSION,
                                  reader);
        TokenStream result =
            new StandardFilter(LuceneDomainIndex.LUCENE_COMPAT_VERSION,
                               tokenizer);
        result = new ASCIIFoldingFilter(result);
        // Convert to lowercase after stemming!
        result =
                new LowerCaseFilter(LuceneDomainIndex.LUCENE_COMPAT_VERSION, result);
        result =
                new NGramTokenFilter(result, DEFAULT_MIN_NGRAM_SIZE, DEFAULT_MAX_NGRAM_SIZE);
        return new TokenStreamComponents(tokenizer, result);
    }
}
