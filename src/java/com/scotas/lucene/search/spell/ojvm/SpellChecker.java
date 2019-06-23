package com.scotas.lucene.search.spell.ojvm;

import com.scotas.lucene.indexer.ContextManager;
import com.scotas.lucene.indexer.Entry;
import com.scotas.lucene.indexer.LuceneDomainIndex;
import com.scotas.lucene.indexer.Parameters;
import com.scotas.lucene.misc.ojvm.StoredCtx;
import com.scotas.lucene.misc.ojvm.TermInfo;
import com.scotas.lucene.store.OJVMDirectory;
import com.scotas.lucene.store.OJVMUtil;

import java.io.IOException;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.aurora.vm.OracleRuntime;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.search.spell.LevensteinDistance;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.NGramDistance;


/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

public class SpellChecker implements SQLData {

    private static BigDecimal key;
    private static String SpellCheckerDefaultField = "word";

    /**
     * Java Util Logging variables and default values
     */
    private static Logger logger = null;

    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = SpellChecker.class.getName();

    static {
        logger = Logger.getLogger(CLASS_NAME);
        logger.setLevel(Level.WARNING);
    }

    final static BigDecimal SUCCESS = new BigDecimal(0);

    final static BigDecimal ERROR = new BigDecimal(1);

    // Implement SQLData interface.
    String sql_type;

    public String getSQLTypeName() throws SQLException {

        return sql_type;
    }

    public void readSQL(SQLInput stream, String typeName) throws SQLException {

        sql_type = typeName;
        key = stream.readBigDecimal();
    }

    public void writeSQL(SQLOutput stream) throws SQLException {

        stream.writeBigDecimal(key);
    }

    public SpellChecker() {

    }

    /**
     * @param owner
     *            of the index
     * @param indexName
     * @param spellColumns
     *            coma separated list
     * @param distanceAlg
     *            Levenstein, Jaro, NGram default Levenstein
     * @throws SQLException
     * @throws IOException
     */
    public static void indexDictionary(String owner, String indexName,
                                       String spellColumns,
                                       String distanceAlg) throws SQLException,
                                                                  IOException {

        Logger logger = Logger.getLogger(CLASS_NAME);
        logger.entering(CLASS_NAME, "indexDictionary",
                        new Object[] { owner, indexName, spellColumns,
                                       distanceAlg });
        String directoryPrefix = owner + "." + indexName;
        OJVMDirectory dir = null;
        try {
            long startTime = System.currentTimeMillis();
            dir = OJVMDirectory.getDirectory(directoryPrefix);
            Parameters parameters = dir.getParameters();
            String logLevel = parameters.getParameter("LogLevel", "WARNING");
            logger.setLevel(Level.parse(logLevel));
            String columnName = parameters.getParameter("DefaultColumn");
            logger.info("SPELLCHECK: add spellcheck columns to the index based on " +
                        columnName + " field distance algorithm: " +
                        distanceAlg);

            // Get the MergeFactor to be passed to indexDictionary. If not
            // exists, use the
            // default merge factor
            int mergeFactor =
                Integer.parseInt(parameters.getParameter("MergeFactor",
                                                         "" + LogMergePolicy.DEFAULT_MERGE_FACTOR));

            // Get the AutoTuneMemory to be passed to indexDictionary.
            boolean autoTuneMemory =
                "true".equalsIgnoreCase(parameters.getParameter("AutoTuneMemory",
                                                                "true"));

            int ramMB = 10; // default value used by SpellChecker
            // indexDictionary

            // If AutoTuneMemory is true
            IndexWriterConfig ic = new IndexWriterConfig(LuceneDomainIndex.LUCENE_COMPAT_VERSION,LuceneDomainIndex.getAnalyzer(parameters));
            boolean useCompountFileName =
                "true".equalsIgnoreCase(parameters.getParameter("UseCompoundFile",
                                                                "false"));
            int maxBufferedDocs =
                Integer.parseInt(parameters.getParameter("MaxBufferedDocs",
                                                         "" + ic.DEFAULT_MAX_BUFFERED_DOCS));
            int maxMergeDocs =
                Integer.parseInt(parameters.getParameter("MaxMergeDocs",
                                                         "" + LogDocMergePolicy.DEFAULT_MAX_MERGE_DOCS));
            int maxBufferedDeleteTerms =
                Integer.parseInt(parameters.getParameter("MaxBufferedDeleteTerms",
                                                         "" +
                                                         ic.DEFAULT_MAX_BUFFERED_DELETE_TERMS));
            if (autoTuneMemory)
              ic.setRAMBufferSizeMB(((OracleRuntime.getJavaPoolSize() /
                                          100) * 50) / (1024 * 1024));
            else {
                ic.setRAMBufferSizeMB(OracleRuntime.getJavaPoolSize() / (1024 * 1024));
                ic.setMaxBufferedDocs(maxBufferedDocs);
            }
            ic.setMaxBufferedDeleteTerms(maxBufferedDeleteTerms);
            LogMergePolicy mp;
            if (autoTuneMemory) {
              mp = new LogByteSizeMergePolicy();
            } else {
              mp = new LogDocMergePolicy();
              mp.setMaxMergeDocs(maxMergeDocs);
              mp.setMergeFactor(mergeFactor);
            }
            mp.setUseCompoundFile(useCompountFileName);
            ic.setMergePolicy(mp);

            // Create the spell checker index "inside" current index
            org.apache.lucene.search.spell.SpellChecker spell = null;

            if ("NGram".equalsIgnoreCase(distanceAlg)) {
                spell =
                   new org.apache.lucene.search.spell.SpellChecker(dir, 
                                                                   new NGramDistance());
            } else if ("Jaro".equalsIgnoreCase(distanceAlg)) {
                spell =
                   new org.apache.lucene.search.spell.SpellChecker(dir, 
                                                                   new JaroWinklerDistance());
            } else {
                // Default is Levenstein
                spell =
                   new org.apache.lucene.search.spell.SpellChecker(dir, 
                                                                   new LevensteinDistance());
            }

            // Open the IndexReader
            IndexReader r = DirectoryReader.open(dir);
            AtomicReader ri = r.leaves().get(0).reader();
            try {
                if (spellColumns != null && spellColumns.length() > 0) {
                    // "something" in
                    // SpellColumns parameter
                    // Fields that exists in index
                    // fields passed in SpellColumns parameter
                    String[] index_field = spellColumns.split(",");
                    for (int i = 0; i < index_field.length; i++) {
                        // check if field exists in the index
                        for (AtomicReaderContext rc : r.leaves()) { 
                          AtomicReader ar = rc.reader(); 
                          FieldInfos fis = ar.getFieldInfos(); 
                          for (FieldInfo fi : fis) 
                            if (index_field[i].equals(fi.name)) {
                                 logger.info("SPELLCHECK: add field " +
                                             index_field[i] +
                                             " to spellchecker index");
                                 // create the dictionary for the field
                                 spell.indexDictionary(new LuceneDictionary(r,
                                                                            index_field[i]),
                                                       ic, false);
                            }
                        } 
                    }
                } else {
                    // Nothing in SpellColumns parameter, index the default
                    // column
                    spell.indexDictionary(new LuceneDictionary(r, columnName),
                                          ic, false);
                }
            } finally {
                // Close the reader
                r.close();
                // due is a write operation at the index invalidate cached
                // readers
                if (dir != null)
                    dir.close();
                dir = null;
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("SpellChecker done - Elapsed time: " + elapsedTime +
                        " millisecond.");
            logger.exiting(CLASS_NAME, "indexDictionary");
        } catch (IOException e) {
            SQLException t = new SQLException(e.getLocalizedMessage());
            logger.throwing(CLASS_NAME, "indexDictionary", t);
            throw t;
        }
    }

    /**
     * @param owner
     *            of the index
     * @param indexName
     * @param wordToRespell
     *            a list of words to spell
     * @param highlight
     *            tag used for highlighting default null
     * @param distanceAlg
     *            Levenstein, Jaro, NGram default Levenstein
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static String didYouMean(String owner, String indexName,
                                    String wordToRespell, String highlight,
                                    String distanceAlg) throws SQLException,
                                                               IOException {

        Logger logger = Logger.getLogger(CLASS_NAME);
        String directoryPrefix = owner + "." + indexName;
        logger.entering(CLASS_NAME, "didYouMean",
                        new Object[] { directoryPrefix, wordToRespell });
        try {
            long startTime = System.currentTimeMillis();
            String wordToSuggest = ""; // return string

            Entry entry = OJVMDirectory.getCachedDirectory(directoryPrefix);
            Parameters parameters = entry.getDirectory().getParameters();
            String logLevel = parameters.getParameter("LogLevel", "WARNING");
            logger.setLevel(Level.parse(logLevel));

            boolean doHighlight = highlight != null && highlight.length() > 0;
            logger.info("Do highlight? " + doHighlight);
            logger.info("Using Distance: " + distanceAlg);
            org.apache.lucene.search.spell.SpellChecker spell =
                new org.apache.lucene.search.spell.SpellChecker(entry.getDirectory());

            String[] wordsToRespell = wordToRespell.split("\\s");
            String[] suggestion = wordsToRespell;

            for (int i = 0; i < wordsToRespell.length; i++) {

                if (spell.exist(wordsToRespell[i])) {
                    suggestion[i] = wordsToRespell[i];
                } else {
                    if (distanceAlg.equals("NGram")) {
                        spell.setStringDistance(new NGramDistance());
                    } else if (distanceAlg.equals("Jaro")) {
                        spell.setStringDistance(new JaroWinklerDistance());
                    } else {
                        spell.setStringDistance(new LevensteinDistance());
                    }

                    String[] suggestions =
                        spell.suggestSimilar(wordsToRespell[i], 5);

                    if (suggestions.length > 0) {
                        if (doHighlight) {
                            suggestion[i] =
                                    "<" + highlight + ">" + suggestions[0] +
                                    "</" + highlight + ">";
                        } else {
                            suggestion[i] = suggestions[0];
                        }
                    }
                }
            }

            for (int i = 0; i < suggestion.length; i++) {
                wordToSuggest += suggestion[i] + " ";
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("Elapsed time: " + elapsedTime + " millisecond.");
            logger.exiting(CLASS_NAME, "didYouMean", wordToSuggest);
            return wordToSuggest;

        } catch (IOException e) {
            SQLException t = new SQLException(e.getLocalizedMessage());
            logger.throwing(CLASS_NAME, "didYouMean", t);
            throw t;
        }

    }

    static public BigDecimal ODCITableStart(STRUCT[] sctx, String owner,
                                            String indexName,
                                            String wordToRespell,
                                            BigDecimal numSug,
                                            String highlight,
                                            String distanceAlg) throws SQLException,
                                                                       IOException,
                                                                       ParseException {

        // logger.entering(CLASS_NAME, "ODCITableStart",
        // new Object[] { sctx[0], indexName, term });
        int key;
        long startTime = System.currentTimeMillis();
        long processedTerms = 0;

        Connection conn = OJVMUtil.getConnection();
        Entry entry = OJVMDirectory.getCachedDirectory(indexName);
        Parameters pars = entry.getDirectory().getParameters();
        String logLevel = pars.getParameter("LogLevel", "WARNING");
        logger.setLevel(Level.parse(logLevel));
        logger.info("Getting did you mean suggestions for: " + wordToRespell);

        String field = SpellCheckerDefaultField;

        if (field == null || field.length() == 0)
            field = pars.getParameter("DefaultColumn");

        field = field.intern();

        int qsize;
        if (numSug != null)
            qsize = numSug.intValue();
        else
            qsize = 1;

        if (qsize < 0)
            throw new SQLException("ldidyoumean() - numSug can't be < 0");

        boolean doHighlight = highlight != null && highlight.length() > 0;
        logger.info("Do highlight? " + doHighlight);
        logger.info("Using Distance: " + distanceAlg);
        org.apache.lucene.search.spell.SpellChecker spell =
            new org.apache.lucene.search.spell.SpellChecker(entry.getDirectory());

        if (distanceAlg != null)
            if (distanceAlg.equals("NGram")) {
                spell.setStringDistance(new NGramDistance());
            } else if (distanceAlg.equals("Jaro")) {
                spell.setStringDistance(new JaroWinklerDistance());
            } else {
                spell.setStringDistance(new LevensteinDistance());
            }

        String termKeyStr =
            (wordToRespell != null && wordToRespell.length() > 0) ?
            wordToRespell : "__ALL__";
        if (doHighlight) {
            termKeyStr =
                    indexName.concat(field).concat(termKeyStr).concat(highlight).concat(distanceAlg);
        } else {
            termKeyStr = indexName.concat(field).concat(termKeyStr);
        }

        Object out_arr[] =
            (Object[])entry.getCachedTermInfoArrays().get(termKeyStr);

        if (out_arr == null || out_arr.length < qsize) {

            String[] wordsToRespell = wordToRespell.split("\\s");

            String[][] data = new String[wordsToRespell.length][];

            for (int i = 0; i < wordsToRespell.length; i++) {

                // lower case the term to compare
                wordToRespell = wordToRespell.toLowerCase();

                if (spell.exist(wordsToRespell[i])) {

                    data[i] = new String[qsize];
                    for (int j = 0; j < qsize; j++)
                        data[i][j] = wordsToRespell[i];

                } else {

                    String[] spellSuggestions =
                        spell.suggestSimilar(wordsToRespell[i], 5 * qsize);

                    data[i] = new String[qsize];
                    if (spellSuggestions.length > 0) {

                        for (int j = 0; j < spellSuggestions.length; j++) {

                            float sd =
                                spell.getStringDistance().getDistance(wordsToRespell[i],
                                                                      spellSuggestions[j]) *
                                100.0f;
                            if (sd > 50.f && j < qsize) {

                                if (doHighlight) {
                                    data[i][j] =
                                            "<" + highlight + ">" + spellSuggestions[j] +
                                            "</" + highlight + ">";
                                } else {
                                    data[i][j] = spellSuggestions[j];
                                }
                            }
                        }
                    }
                }
            }

            String[] sb = new String[data[0].length];

            for (int j = 0; j < data[0].length; j++) {
                String sug = new String();
                for (int i = 0; i < data.length; i++) {
                    sug += data[i][j] + " ";
                }
                sb[j] = sug;
            }

            out_arr = new Object[qsize];

            StructDescriptor outDesc =
                StructDescriptor.createDescriptor("LUCENE.TERM_INFO", conn);

            Object[] out_attr = new Object[3];

            HashMap hMap = new HashMap();

            for (int i = 0; i < sb.length; i++) {
                String suggestion = sb[i].trim();

                if (!suggestion.equalsIgnoreCase("null") &&
                    suggestion != null) {
                    if (processedTerms < qsize) {

                        out_attr[0] = suggestion;

                        if (doHighlight) {
                            suggestion =
                                    suggestion.replaceAll("<" + highlight +
                                                          ">", "");
                            suggestion =
                                    suggestion.replaceAll("</" + highlight +
                                                          ">", "");
                        }
                        BigDecimal hits = LuceneDomainIndex.countHits(owner, indexName,
                                                        suggestion);

                        Term term = new Term(field, suggestion);
                        TermInfo termInfo =
                            new TermInfo(term, hits.intValue());

                        if (!hMap.containsKey(termInfo.term.text())) {
                            hMap.put(termInfo.term.text(),
                                     new Integer(termInfo.docFreq));

                            out_attr[1] = new Integer(hits.intValue());

                            if (((Integer)out_attr[1]).intValue() > 0)
                                out_arr[(int)processedTerms++] =
                                        new STRUCT(outDesc, conn, out_attr);
                        }
                    }
                }
            }

            // if processed term is smaller then request suggestions

            if (processedTerms < out_arr.length) {
                Object out_arr_small[] = new Object[(int)processedTerms];
                System.arraycopy(out_arr, 0, out_arr_small, 0,
                                 (int)processedTerms);
                entry.getCachedTermInfoArrays().put(termKeyStr, out_arr_small);
                logger.info("storing small array " + out_arr_small +
                            " length: " + out_arr_small.length + " key: " +
                            termKeyStr);
            } else {
                entry.getCachedTermInfoArrays().put(termKeyStr, out_arr);
                logger.info("storing array " + out_arr + " length: " +
                            out_arr.length + " key: " + termKeyStr);
            }
        } else {
            // only want to get numSug from the cached array
            processedTerms = numSug.intValue();
            // System.out.println("Found a stored array length: " +
            // out_arr.length
            // + " but need only: " + processedTerms);
        }

        // register stored context with cartridge services
        StoredCtx ctx = new StoredCtx(out_arr, numSug.intValue());
        ctx.setStartTime(startTime);
        ctx.setNumTerms((int)processedTerms);
        key = ContextManager.setContext(ctx);

        // create a DIDYOUMEAN_TERM_OT instance and store the key in it
        Object[] impAttr = new Object[1];
        impAttr[0] = new BigDecimal(key);
        StructDescriptor sd =
            new StructDescriptor("LUCENE.DIDYOUMEAN_TERM_OT", conn);
        sctx[0] = new STRUCT(sd, conn, impAttr);
        // logger.info("key: " + key);
        // logger.exiting(CLASS_NAME, "ODCITableStart", SUCCESS);

        return SUCCESS;
    }

    public BigDecimal ODCITableFetch(BigDecimal nrows,
                                     ARRAY[] outSet) throws SQLException,
                                                            InvalidTokenOffsetsException,
                                                            IOException {

        Connection conn = OJVMUtil.getConnection();
        // retrieve stored context using the key
        StoredCtx sctx;
        sctx = (StoredCtx)ContextManager.getContext(key.intValue());
        Object out_arr[] = sctx.getOut_arr();
        // return if no terms found
        if (out_arr.length == 0)
            return SUCCESS;
        int numTerms = sctx.getNumTerms();
        // logger.info("getting array " + out_arr + " length: " + out_arr.length
        // +
        // " expected terms: " + numTerms);
        ArrayDescriptor ad = new ArrayDescriptor("LUCENE.TERM_INFO_SET", conn);
        if (numTerms < out_arr.length) { // get only first part of the array
            Object out_arr_small[] = new Object[numTerms];
            System.arraycopy(out_arr, 0, out_arr_small, 0, numTerms);
            outSet[0] = new ARRAY(ad, conn, out_arr_small);
        } else
            // computed array have the expected size
            outSet[0] = new ARRAY(ad, conn, out_arr);
        // store an empty array for next call
        sctx.setOut_arr(new Object[] { });
        return SUCCESS;
    }

    public BigDecimal ODCITableClose() throws SQLException, IOException {

        StoredCtx sctx;
        sctx = (StoredCtx)ContextManager.clearContext(key.intValue());
        long elapsedTime = System.currentTimeMillis() - sctx.getStartTime();
        logger.info("Elapsed time: " + elapsedTime + " millisecond.");
        // logger.exiting(CLASS_NAME, "ODCITableClose", SUCCESS);
        return SUCCESS;
    }

}
