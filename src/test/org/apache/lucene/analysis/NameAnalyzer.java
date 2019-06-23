package org.apache.lucene.analysis;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * A Person names analyzer implementation. Don't use stop words, lower case the terms and remove accentuation.
 *
 */

import java.io.Reader;

import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import com.scotas.lucene.indexer.LuceneDomainIndex;

public class NameAnalyzer extends Analyzer {

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName,
                                                              Reader reader) {
        if (fieldName == null)
            throw new IllegalArgumentException("fieldName must not be null");
        if (reader == null)
            throw new IllegalArgumentException("reader must not be null");

        // Use Standard Tokenizer
        Tokenizer tokenizer =
            new StandardTokenizer(LuceneDomainIndex.LUCENE_COMPAT_VERSION,
                                  reader);
        // Standar Filter
        TokenStream result =
            new StandardFilter(LuceneDomainIndex.LUCENE_COMPAT_VERSION,
                               tokenizer);
        // Remove accentuation
        result = new ASCIIFoldingFilter(result);
        // Convert to lower case
        result =
                new LowerCaseFilter(LuceneDomainIndex.LUCENE_COMPAT_VERSION, result);
        return new TokenStreamComponents(tokenizer, result);
    }
}
