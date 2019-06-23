package com.scotas.lucene.indexer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.scotas.lucene.store.OJVMUtil;
import com.scotas.lucene.util.StringUtils;

import java.util.ArrayList;
import java.util.Random;

import oracle.aurora.vm.OracleRuntime;

import oracle.sql.BLOB;

import org.slf4j.LoggerFactory;

/**
 * A class to store Lucene Domain Index parameters
 * OJVMDirectory class store these parameters on
 * lucene_index table
 * @see com.scotas.lucene.store.OJVMDirectory
 */
public class Parameters implements Serializable {
    static final String CLASS_NAME = Parameters.class.getName();

    private static org.slf4j.Logger logger =
        LoggerFactory.getLogger(CLASS_NAME);

    private static final Random RANDOM = new Random();

    /**
     * List of valid parameter for Lucene Domain Index implementation
     * This parameter are valid for DDL statements create index ... parameters(..)
     * or alter index .... parameters(...)
     */
    private static final String[] paramList =
    { "Analyzer", "MergeFactor", "MaxBufferedDocs", "MaxMergeDocs",
      "MaxBufferedDeleteTerms", "UserDataStore", "FormatCols", "ExtraCols",
      "ExtraTabs", "WhereCondition", "SyncMode", "UseCompoundFile",
      "AutoTuneMemory", "LobStorageParameters", "LogLevel", "BatchCount",
      "IncludeMasterColumn", "PopulateIndex", "DefaultColumn",
      "DefaultOperator", "Formatter", "MaxNumFragmentsRequired",
      "FragmentSeparator", "FragmentSize", "PerFieldAnalyzer",
      "NormalizeScore", "PreserveDocIdOrder", "CachedRowIdSize",
      "ParallelDegree", "RewriteScore", "SimilarityMethod", "IndexOnRam",
      "HighlightColumn", "LockMasterTable", "Searcher", "Updater",
      "MaxFieldLength", "AutoCommitMaxTime",
      // Solr Specific parameters
      "WaitFlush", "WaitSearcher", "ExpungeDeletes", "MaxSegments", "CommitOnSync",
      "MltColumn", "MltMinTf", "MltMinDf", "MltCount", "SoftCommit",
      "FacetedCols","UseFastVectorHighlighter" };

    /**
     * List of internal parameter for Lucene Domain Index implementation
     * This parameter are internal and not be exported as parameter list to be stored
     * in system views
     */
    private static final String[] internalList =
    { "ColName", "TypeName", "TableSchema", "TableName", "Partition" };

    private static final String[][] solrConfFiles =
    { { "solrconfig.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
    "<config>\n" + 
    "  <!-- \n" + 
    "     Default solrconfig.xml for OLS\n" + 
    "  -->\n" + 
    "  <luceneMatchVersion>LUCENE_40</luceneMatchVersion>\n" + 
    "  <dataDir>${solr.data.dir:}</dataDir>\n" + 
    "  <directoryFactory name=\"DirectoryFactory\" class=\"com.scotas.solr.core.NRTCachingDirectoryFactory\"/>\n" + 
    "  <indexConfig>\n" + 
    "    <ramBufferSizeMB>"+ ((OracleRuntime.getJavaPoolSize() / 100) *
                                   50) / (1024 * 1024)+ "</ramBufferSizeMB>\n" + 
    "    <mergeFactor>10</mergeFactor>\n" + 
    "    <lockType>simple</lockType>\n" + 
    "    <deletionPolicy class=\"solr.SolrDeletionPolicy\">\n" + 
    "        <str name=\"maxCommitsToKeep\">0</str>\n" + 
    "        <str name=\"maxOptimizedCommitsToKeep\">0</str>\n" + 
    "    </deletionPolicy>\n" + 
    "  </indexConfig>\n" + 
    "  <jmx/>\n" + 
    "  <updateHandler class=\"solr.DirectUpdateHandler2\">\n" + 
    "    <autoCommit> \n" + 
    "        <maxTime>30000</maxTime> \n" + 
    "        <openSearcher>false</openSearcher> \n" + 
    "    </autoCommit>\n" + 
    "    <autoSoftCommit> \n" + 
    "        <maxTime>60000</maxTime> \n" + 
    "    </autoSoftCommit>\n" + 
    "    <listener event=\"postCommit\" class=\"com.scotas.solr.core.RowIDLoaderEventListener\">\n" + 
    "      <str name=\"action\">commit</str>\n" + 
    "      <str name=\"purgeDeletedFiles\">true</str>\n" + 
    "    </listener>\n" + 
    "    <listener event=\"postOptimize\" class=\"com.scotas.solr.core.RowIDLoaderEventListener\">\n" + 
    "      <str name=\"action\">optimize</str>\n" + 
    "      <str name=\"purgeDeletedFiles\">true</str>\n" + 
    "    </listener>\n" + 
    " </updateHandler>\n" + 
    "  <query>\n" + 
    "    <maxBooleanClauses>1024</maxBooleanClauses>\n" + 
    "    <filterCache class=\"solr.FastLRUCache\" size=\"512\" initialSize=\"512\" autowarmCount=\"0\"/>\n" + 
    "    <queryResultCache class=\"solr.LRUCache\" size=\"512\" initialSize=\"512\" autowarmCount=\"0\"/>\n" + 
    "    <documentCache class=\"solr.LRUCache\" size=\"512\" initialSize=\"512\" autowarmCount=\"0\"/>\n" + 
    "    <enableLazyFieldLoading>true</enableLazyFieldLoading>\n" + 
    "    <queryResultWindowSize>20</queryResultWindowSize>\n" + 
    "    <queryResultMaxDocsCached>200</queryResultMaxDocsCached>\n" + 
    "    <useColdSearcher>false</useColdSearcher>\n" + 
    "    <maxWarmingSearchers>2</maxWarmingSearchers>\n" + 
    "  </query>\n" + 
    "  <requestDispatcher handleSelect=\"true\">\n" + 
    "    <requestParsers enableRemoteStreaming=\"true\" multipartUploadLimitInKB=\"2048000\"/>\n" + 
    "    <httpCaching lastModifiedFrom=\"openTime\" etagSeed=\"Solr\">\n" + 
    "    </httpCaching>\n" + 
    "  </requestDispatcher>\n" + 
    "  <requestHandler name=\"standard\" class=\"solr.SearchHandler\" default=\"true\">\n" + 
    "    <lst name=\"defaults\">\n" + 
    "      <str name=\"echoParams\">explicit</str>\n" + 
    "    </lst>\n" + 
    "    <arr name=\"components\">\n" + 
    "      <str>query</str>\n" + 
    "      <str>facet</str>\n" + 
    "      <str>mlt</str>\n" + 
    "      <str>highlight</str>\n" + 
    "      <str>spellcheck</str>\n" + 
    "    </arr>\n" + 
    "  </requestHandler>\n" + 
    "  <searchComponent name=\"spellcheck\" class=\"solr.SpellCheckComponent\">\n" + 
    "    <str name=\"queryAnalyzerFieldType\">textSpell</str>\n" + 
    "    <lst name=\"spellchecker\">\n" + 
    "      <str name=\"name\">default</str>\n" + 
    "      <str name=\"field\">title</str>\n" + 
    "      <str name=\"classname\">solr.DirectSolrSpellChecker</str>\n" + 
    "      <str name=\"distanceMeasure\">internal</str>\n" + 
    "      <int name=\"minPrefix\">2</int>\n" + 
    "      <int name=\"minQueryLength\">3</int>\n" + 
    "    </lst>\n" + 
    "  </searchComponent>\n" + 
    "  <searchComponent name=\"termsComponent\" class=\"org.apache.solr.handler.component.TermsComponent\"/>\n" + 
    "  <searchComponent name=\"elevator\" class=\"solr.QueryElevationComponent\">\n" + 
    "    <str name=\"queryFieldType\">string</str>\n" + 
    "    <str name=\"config-file\">elevate.xml</str>\n" + 
    "  </searchComponent>\n" + 
    "  <searchComponent class=\"solr.HighlightComponent\" name=\"highlight\">\n" + 
    "    <highlighting>\n" + 
    "      <fragmenter name=\"gap\" class=\"org.apache.solr.highlight.GapFragmenter\" default=\"true\">\n" + 
    "        <lst name=\"defaults\">\n" + 
    "          <int name=\"hl.fragsize\">100</int>\n" + 
    "        </lst>\n" + 
    "      </fragmenter>\n" + 
    "      <fragmenter name=\"regex\" class=\"org.apache.solr.highlight.RegexFragmenter\">\n" + 
    "        <lst name=\"defaults\">\n" + 
    "          <int name=\"hl.fragsize\">70</int>\n" + 
    "          <float name=\"hl.regex.slop\">0.5</float>\n" + 
    "          <str name=\"hl.regex.pattern\">[-\\w ,/\\n\\&quot;&apos;]{20,200}</str>\n" + 
    "        </lst>\n" + 
    "      </fragmenter>\n" + 
    "      <formatter name=\"html\" class=\"org.apache.solr.highlight.HtmlFormatter\" default=\"true\">\n" + 
    "        <lst name=\"defaults\">\n" + 
    "          <str name=\"hl.simple.pre\"><![CDATA[<em>]]></str>\n" + 
    "          <str name=\"hl.simple.post\"><![CDATA[</em>]]></str>\n" + 
    "        </lst>\n" + 
    "      </formatter>\n" + 
    "      <encoder name=\"html\" class=\"org.apache.solr.highlight.HtmlEncoder\" default=\"true\"/>\n" + 
    "      <fragListBuilder name=\"simple\" class=\"org.apache.solr.highlight.SimpleFragListBuilder\" default=\"true\"/>\n" + 
    "      <fragListBuilder name=\"single\" class=\"org.apache.solr.highlight.SingleFragListBuilder\"/>\n" + 
    "      <fragmentsBuilder name=\"default\" class=\"org.apache.solr.highlight.ScoreOrderFragmentsBuilder\" default=\"true\">\n" + 
    "      </fragmentsBuilder>\n" + 
    "      <fragmentsBuilder name=\"colored\" class=\"org.apache.solr.highlight.ScoreOrderFragmentsBuilder\">\n" + 
    "        <lst name=\"defaults\">\n" + 
    "          <str name=\"hl.tag.pre\"><![CDATA[\n" + 
    "               <b style=\"background:yellow\">,<b style=\"background:lawgreen\">,\n" + 
    "               <b style=\"background:aquamarine\">,<b style=\"background:magenta\">,\n" + 
    "               <b style=\"background:palegreen\">,<b style=\"background:coral\">,\n" + 
    "               <b style=\"background:wheat\">,<b style=\"background:khaki\">,\n" + 
    "               <b style=\"background:lime\">,<b style=\"background:deepskyblue\">]]></str>\n" + 
    "          <str name=\"hl.tag.post\"><![CDATA[</b>]]></str>\n" + 
    "        </lst>\n" + 
    "      </fragmentsBuilder>\n" + 
    "    </highlighting>\n" + 
    "  </searchComponent>\n" + 
    "  <admin>\n" + 
    "    <defaultQuery>solr</defaultQuery>\n" + 
    "  </admin>\n" + 
    "  <codecFactory class=\"solr.SchemaCodecFactory\"/>\n" + 
    "</config>\n" },
      { "schema.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + 
      "<schema name=\"OLS.DEFAULT\" version=\"1.5\">\n" + 
      "  <types>\n" + 
      "    <fieldType name=\"string\" class=\"solr.StrField\" sortMissingLast=\"true\" omitNorms=\"true\"/>\n" + 
      "    <fieldType name=\"string_pulsing\" class=\"solr.StrField\" postingsFormat=\"Pulsing40\"/>\n" + 
      "    <fieldType name=\"boolean\" class=\"solr.BoolField\" sortMissingLast=\"true\"/>\n" + 
      "    <fieldtype name=\"binary\" class=\"solr.BinaryField\"/>\n" + 
      "    <fieldType name=\"int\" class=\"solr.TrieIntField\" precisionStep=\"0\" positionIncrementGap=\"0\"/>\n" + 
      "    <fieldType name=\"float\" class=\"solr.TrieFloatField\" precisionStep=\"0\" positionIncrementGap=\"0\"/>\n" + 
      "    <fieldType name=\"long\" class=\"solr.TrieLongField\" precisionStep=\"0\" positionIncrementGap=\"0\"/>\n" + 
      "    <fieldType name=\"double\" class=\"solr.TrieDoubleField\" precisionStep=\"0\" positionIncrementGap=\"0\"/>\n" + 
      "    <fieldType name=\"tint\" class=\"solr.TrieIntField\" precisionStep=\"8\" positionIncrementGap=\"0\"/>\n" + 
      "    <fieldType name=\"tfloat\" class=\"solr.TrieFloatField\" precisionStep=\"8\" positionIncrementGap=\"0\"/>\n" + 
      "    <fieldType name=\"tlong\" class=\"solr.TrieLongField\" precisionStep=\"8\" positionIncrementGap=\"0\"/>\n" + 
      "    <fieldType name=\"tdouble\" class=\"solr.TrieDoubleField\" precisionStep=\"8\" positionIncrementGap=\"0\"/>\n" + 
      "    <fieldType name=\"date\" class=\"solr.TrieDateField\" precisionStep=\"0\" positionIncrementGap=\"0\"/>\n" + 
      "    <fieldType name=\"tdate\" class=\"solr.TrieDateField\" precisionStep=\"6\" positionIncrementGap=\"0\"/>\n" + 
      "    <fieldType name=\"pint\" class=\"solr.IntField\"/>\n" + 
      "    <fieldType name=\"plong\" class=\"solr.LongField\"/>\n" + 
      "    <fieldType name=\"pfloat\" class=\"solr.FloatField\"/>\n" + 
      "    <fieldType name=\"pdouble\" class=\"solr.DoubleField\"/>\n" + 
      "    <fieldType name=\"pdate\" class=\"solr.DateField\" sortMissingLast=\"true\"/>\n" + 
      "    <fieldType name=\"random\" class=\"solr.RandomSortField\" indexed=\"true\" />\n" + 
      "    <fieldType name=\"sint\" class=\"solr.SortableIntField\" sortMissingLast=\"true\" omitNorms=\"true\"/>\n" + 
      "    <fieldType name=\"slong\" class=\"solr.SortableLongField\" sortMissingLast=\"true\" omitNorms=\"true\"/>\n" + 
      "    <fieldType name=\"sfloat\" class=\"solr.SortableFloatField\" sortMissingLast=\"true\" omitNorms=\"true\"/>\n" + 
      "    <fieldType name=\"sdouble\" class=\"solr.SortableDoubleField\" sortMissingLast=\"true\" omitNorms=\"true\"/>\n" + 
      "    <fieldType name=\"text_ws\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" + 
      "      <analyzer>\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.core.WhitespaceTokenizerFactory\"/>\n" + 
      "      </analyzer>\n" + 
      "    </fieldType>\n" + 
      "    <fieldType name=\"text_general\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" + 
      "      <analyzer type=\"index\">\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.standard.StandardTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" enablePositionIncrements=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "      </analyzer>\n" + 
      "      <analyzer type=\"query\">\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.standard.StandardTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" enablePositionIncrements=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.synonym.SynonymFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "      </analyzer>\n" + 
      "    </fieldType>\n" + 
      "    <fieldType name=\"text_general_rev\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" + 
      "      <analyzer type=\"index\">\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.standard.StandardTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" enablePositionIncrements=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "        <filter class=\"solr.ReversedWildcardFilterFactory\" withOriginal=\"true\"\n" + 
      "           maxPosAsterisk=\"3\" maxPosQuestion=\"2\" maxFractionAsterisk=\"0.33\"/>\n" + 
      "      </analyzer>\n" + 
      "      <analyzer type=\"query\">\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.standard.StandardTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" enablePositionIncrements=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.synonym.SynonymFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "      </analyzer>\n" + 
      "    </fieldType>\n" + 
      "    <fieldType name=\"text_en\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" + 
      "      <analyzer type=\"index\">\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.standard.StandardTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" enablePositionIncrements=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "	<filter class=\"org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilterFactory\" protected=\"protwords.txt\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.en.PorterStemFilterFactory\"/>\n" + 
      "      </analyzer>\n" + 
      "      <analyzer type=\"query\">\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.standard.StandardTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.synonym.SynonymFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" enablePositionIncrements=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "	<filter class=\"org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilterFactory\" protected=\"protwords.txt\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.en.PorterStemFilterFactory\"/>\n" + 
      "      </analyzer>\n" + 
      "    </fieldType>\n" + 
      "    <fieldType name=\"text_en_splitting\" class=\"solr.TextField\" positionIncrementGap=\"100\" autoGeneratePhraseQueries=\"true\">\n" + 
      "      <analyzer type=\"index\">\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.core.WhitespaceTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" enablePositionIncrements=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.miscellaneous.WordDelimiterFilterFactory\" generateWordParts=\"1\" generateNumberParts=\"1\" catenateWords=\"1\" catenateNumbers=\"1\" catenateAll=\"0\" splitOnCaseChange=\"1\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilterFactory\" protected=\"protwords.txt\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.en.PorterStemFilterFactory\"/>\n" + 
      "      </analyzer>\n" + 
      "      <analyzer type=\"query\">\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.core.WhitespaceTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.synonym.SynonymFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" enablePositionIncrements=\"true\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.miscellaneous.WordDelimiterFilterFactory\" generateWordParts=\"1\" generateNumberParts=\"1\" catenateWords=\"0\" catenateNumbers=\"0\" catenateAll=\"0\" splitOnCaseChange=\"1\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilterFactory\" protected=\"protwords.txt\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.en.PorterStemFilterFactory\"/>\n" + 
      "      </analyzer>\n" + 
      "    </fieldType>\n" + 
      "    <fieldType name=\"text_ngram\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" + 
      "      <analyzer>\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.standard.StandardTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.ngram.NGramFilterFactory\" minGramSize=\"2\" maxGramSize=\"2\" />\n" + 
      "      </analyzer>\n" + 
      "    </fieldType>" +
      "    <fieldType name=\"text_edge_ngram\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" + 
      "      <analyzer type=\"index\">\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.standard.StandardTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory\" minGramSize=\"2\" maxGramSize=\"45\" />\n" + 
      "      </analyzer>\n" +
      "      <analyzer type=\"query\">\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.standard.StandardTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\"/>\n" + 
      "      </analyzer>\n" + 
      "    </fieldType>\n" +
      "    <fieldType name=\"alphaOnlySort\" class=\"solr.TextField\" sortMissingLast=\"true\" omitNorms=\"true\">\n" + 
      "      <analyzer>\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.core.KeywordTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\" />\n" + 
      "        <filter class=\"org.apache.lucene.analysis.miscellaneous.TrimFilterFactory\" />\n" + 
      "        <filter class=\"org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory\"\n" + 
      "                pattern=\"([^a-z])\" replacement=\"\" replace=\"all\"\n" + 
      "        />\n" + 
      "      </analyzer>\n" + 
      "    </fieldType>\n" + 
      "    <fieldtype name=\"phonetic\" stored=\"false\" indexed=\"true\" class=\"solr.TextField\" >\n" + 
      "      <analyzer>\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.standard.StandardTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.phonetic.DoubleMetaphoneFilterFactory\" inject=\"false\"/>\n" + 
      "      </analyzer>\n" + 
      "    </fieldtype>\n" + 
      "\n" + 
      "    <fieldtype name=\"payloads\" stored=\"false\" indexed=\"true\" class=\"solr.TextField\" >\n" + 
      "      <analyzer>\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.core.WhitespaceTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilterFactory\" encoder=\"float\"/>\n" + 
      "      </analyzer>\n" + 
      "    </fieldtype>\n" + 
      "    <fieldType name=\"lowercase\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" + 
      "      <analyzer>\n" + 
      "        <tokenizer class=\"org.apache.lucene.analysis.core.KeywordTokenizerFactory\"/>\n" + 
      "        <filter class=\"org.apache.lucene.analysis.core.LowerCaseFilterFactory\" />\n" + 
      "      </analyzer>\n" + 
      "    </fieldType>\n" + 
      "    <fieldtype name=\"ignored\" stored=\"false\" indexed=\"false\" multiValued=\"true\" class=\"solr.StrField\" />\n" + 
      "    <fieldType name=\"point\" class=\"solr.PointType\" dimension=\"2\" subFieldSuffix=\"_d\"/>\n" + 
      "    <fieldType name=\"location\" class=\"solr.LatLonType\" subFieldSuffix=\"_coordinate\"/>\n" + 
      "    <fieldtype name=\"geohash\" class=\"solr.GeoHashField\"/>\n" + 
      " </types>\n" + 
      " <fields>\n" + 
      "   <field name=\"rowid\" type=\"string\" indexed=\"true\" stored=\"true\" required=\"true\" /> \n" + 
      "\n" + 
      "   <field name=\"title\"    type=\"text_general\"      indexed=\"true\" stored=\"true\"  multiValued=\"true\"/>\n" + 
      "   <field name=\"text\"     type=\"text_en_splitting\" indexed=\"true\" stored=\"false\" multiValued=\"true\"/>\n" + 
      "   <field name=\"features\" type=\"text_en_splitting\" indexed=\"true\" stored=\"true\"  multiValued=\"true\"/>\n" + 
      "   <field name=\"text_rev\" type=\"text_general_rev\"  indexed=\"true\" stored=\"false\" multiValued=\"true\"/>\n" + 
      "\n" + 
      "   <dynamicField name=\"*_i\"    type=\"int\"             indexed=\"true\" stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_in\"   type=\"int\"             indexed=\"true\" stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_s\"    type=\"string\"          indexed=\"true\" stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_sn\"   type=\"string\"          indexed=\"true\" stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_l\"    type=\"long\"            indexed=\"true\" stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_ln\"   type=\"long\"            indexed=\"true\" stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_t\"    type=\"text_general\"    indexed=\"true\" stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tn\"   type=\"text_general\"    indexed=\"true\" stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_tlo\"  type=\"lowercase\"       indexed=\"true\" stored=\"true\"  multiValued=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tlon\" type=\"lowercase\"       indexed=\"true\" stored=\"false\" multiValued=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tw\"   type=\"text_ws\"         indexed=\"true\" stored=\"true\"  multiValued=\"true\"/>\n" + 
      "   <dynamicField name=\"*_twn\"  type=\"text_ws\"         indexed=\"true\" stored=\"false\" multiValued=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tg\"   type=\"text_general\"    indexed=\"true\" stored=\"true\"  multiValued=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tgn\"  type=\"text_general\"    indexed=\"true\" stored=\"false\" multiValued=\"true\"/>\n" + 
      "   <dynamicField name=\"*_ts\"   type=\"string\"          indexed=\"true\" stored=\"true\"  multiValued=\"true\" termVectors=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tsn\"  type=\"string\"          indexed=\"true\" stored=\"false\" multiValued=\"true\" termVectors=\"true\"/>\n" + 
      "   <dynamicField name=\"*_b\"    type=\"boolean\"         indexed=\"true\" stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_bn\"   type=\"boolean\"         indexed=\"true\" stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_f\"    type=\"float\"           indexed=\"true\" stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_fn\"   type=\"float\"           indexed=\"true\" stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_d\"    type=\"double\"          indexed=\"true\" stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_dn\"   type=\"double\"          indexed=\"true\" stored=\"false\"/>\n" +
      "   <dynamicField name=\"*_ng\"   type=\"text_ngram\"      indexed=\"true\" stored=\"false\"/>" +
      "   <dynamicField name=\"*_eg\"   type=\"text_edge_ngram\" indexed=\"true\" stored=\"false\"/>\n" + 
      "\n" + 
      "   <dynamicField name=\"*_coordinate\" type=\"tdouble\" indexed=\"true\"  stored=\"false\"/>\n" + 
      "\n" + 
      "   <dynamicField name=\"*_dt\"  type=\"date\"     indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_dtn\" type=\"date\"     indexed=\"true\"  stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_p\"   type=\"location\" indexed=\"true\" stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_pn\"  type=\"location\" indexed=\"true\" stored=\"false\"/>\n" + 
      "\n" + 
      "   <dynamicField name=\"*_ti\"   type=\"tint\"    indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tin\"  type=\"tint\"    indexed=\"true\"  stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_tl\"   type=\"tlong\"   indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tln\"  type=\"tlong\"   indexed=\"true\"  stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_tf\"   type=\"tfloat\"  indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tfn\"  type=\"tfloat\"  indexed=\"true\"  stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_td\"   type=\"tdouble\" indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tdn\"  type=\"tdouble\" indexed=\"true\"  stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_tdt\"  type=\"tdate\"   indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_tdtn\" type=\"tdate\"   indexed=\"true\"  stored=\"false\"/>\n" + 
      "\n" + 
      "   <dynamicField name=\"*_pi\"  type=\"pint\"     indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_pin\" type=\"pint\"     indexed=\"true\"  stored=\"false\"/>\n" + 
      "\n" + 
      "   <dynamicField name=\"*_ph\"  type=\"phonetic\" indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_phn\" type=\"phonetic\" indexed=\"true\"  stored=\"false\"/>\n" + 
      "\n" + 
      "   <dynamicField name=\"*_si\"  type=\"sint\"    indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_sin\" type=\"sint\"    indexed=\"true\"  stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_sl\"  type=\"slong\"   indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_sln\" type=\"slong\"   indexed=\"true\"  stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_sf\"  type=\"sfloat\"  indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_sfn\" type=\"sfloat\"  indexed=\"true\"  stored=\"false\"/>\n" + 
      "   <dynamicField name=\"*_sd\"  type=\"sdouble\" indexed=\"true\"  stored=\"true\"/>\n" + 
      "   <dynamicField name=\"*_sdn\" type=\"sdouble\" indexed=\"true\"  stored=\"false\"/>\n" + 
      "\n" + 
      "   <dynamicField name=\"ignored_*\" type=\"ignored\" multiValued=\"true\"/>\n" + 
      "   <dynamicField name=\"attr_*\" type=\"text_general\" indexed=\"true\" stored=\"true\" multiValued=\"true\"/>\n" + 
      "\n" + 
      "   <dynamicField name=\"random_*\" type=\"random\" />   \n" + 
      " </fields>\n" + 
      " \n" + 
      " <uniqueKey>rowid</uniqueKey>\n" + 
      " \n" + 
      " <defaultSearchField>text</defaultSearchField>\n" + 
      " \n" + 
      " <solrQueryParser defaultOperator=\"OR\"/>\n" + 
      "	   \n" + 
      " <copyField source=\"*_t\"   dest=\"text\" maxChars=\"3000\"/>\n" + 
      " <copyField source=\"*_tn\"  dest=\"text\" maxChars=\"3000\"/>\n" + 
      " <copyField source=\"*_tw\"  dest=\"text\" maxChars=\"3000\"/>\n" + 
      " <copyField source=\"*_twn\" dest=\"text\" maxChars=\"3000\"/>\n" + 
      " <copyField source=\"*_tg\"  dest=\"text\" maxChars=\"3000\"/>\n" + 
      " <copyField source=\"*_tgn\" dest=\"text\" maxChars=\"3000\"/>\n" + 
      " <copyField source=\"*_ts\"  dest=\"text\" maxChars=\"3000\"/>\n" + 
      " <copyField source=\"*_tsn\" dest=\"text\" maxChars=\"3000\"/>\n" +
      "</schema>\n" },
      { "elevate.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + 
      "<elevate>\n" + 
      " <query text=\"foo bar\">\n" + 
      "  <doc id=\"1\" />\n" + 
      "  <doc id=\"2\" />\n" + 
      "  <doc id=\"3\" />\n" + 
      " </query>\n" + 
      " \n" + 
      " <query text=\"ipod\">\n" + 
      "   <doc id=\"MA147LL/A\" />  <!-- put the actual ipod at the top -->\n" + 
      "   <doc id=\"IW-02\" exclude=\"true\" /> <!-- exclude this cable -->\n" + 
      " </query>\n" + 
      " \n" + 
      "</elevate>\n" },
      { "protwords.txt", "cats\n" + 
      "ridding\n" + 
      "c#\n" + 
      "c++\n" + 
      ".net\n" },
      { "stopwords.txt", "a\n" + 
      "an\n" + 
      "and\n" + 
      "are\n" + 
      "as\n" + 
      "at\n" + 
      "be\n" + 
      "but\n" + 
      "by\n" + 
      "for\n" + 
      "if\n" + 
      "in\n" + 
      "into\n" + 
      "is\n" + 
      "it\n" + 
      "no\n" + 
      "not\n" + 
      "of\n" + 
      "on\n" + 
      "or\n" + 
      "s\n" + 
      "such\n" + 
      "t\n" + 
      "that\n" + 
      "the\n" + 
      "their\n" + 
      "then\n" + 
      "there\n" + 
      "these\n" + 
      "they\n" + 
      "this\n" + 
      "to\n" + 
      "was\n" + 
      "will\n" + 
      "with\n" },
      { "synonyms.txt", "aaa => aaaa\n" + 
      "bbb => bbbb1 bbbb2\n" + 
      "ccc => cccc1,cccc2\n" + 
      "a\\=>a => b\\=>b\n" + 
      "a\\,a => b\\,b\n" + 
      "fooaaa,baraaa,bazaaa\n" + 
      "\n" + 
      "GB,gib,gigabyte,gigabytes\n" + 
      "MB,mib,megabyte,megabytes\n" + 
      "Television, Televisions, TV, TVs\n" + 
      "\n" + 
      "pixima => pixma\n" } };

    /**
     * SQL command to check if there is parameters stored
     */
    private static final String countParameterSelectStmt =
        "SELECT NAME FROM %IDX%$T" + " WHERE NAME = 'parameters'";

    /**
     * SQL command to load a byte array from Lucene Domain Index storage table
     */
    private static final String getParameterDataStmt =
        "SELECT DATA FROM %IDX%$T" + " WHERE NAME='parameters'";

    /**
     * SQL command to load a host,port list of servers order by host_name,port
     * using BG_PROCESS_NAME
     */
    private static final String selServerDataStmt =
        "SELECT HOST_NAME||':'||PORT FROM LUCENE.BG_PROCESS WHERE BG_PROCESS_NAME = ? ORDER BY HOST_NAME,PORT";

    /**
     * SQL command to load a host port list of registerd flash servers order by query time
     */
    static final String selFlashbackServStmt = 
        "SELECT HOST_NAME||':'||PORT FROM LUCENE.FB_PROCESS ORDER BY QUERY_TIME";

    /**
     * List of registered Solr Servers, string have host:port syntax
     */
    private static String[] solrServerList = null;
    
    /**
     * List of registered Flashback Solr Servers, string have host:port syntax
     */
    private static String[] solrFlashbackServerList = null;

    /**
     * List of registered Lucene Index Scan Servers, string have host:port syntax
     */
    private static String[] luceneServerList = null;

    /**
     * List of registered Lucene Index Scan Servers, string have host:port syntax
     */
    private static String[] luceneUpdaterList = null;

    /**
     * SQL command to update a byte array into Lucene Domain Index storage table
     */
    private static final String updateParameterStmt =
        "UPDATE %IDX%$T SET LAST_MODIFIED=sysdate,FILE_SIZE=?,DATA=? " +
        " WHERE NAME = 'parameters'";

    /**
     * SQL command to update write.lock counter of Lucene Domain Index storage
     */
    private static final String updateCountStmt =
        "UPDATE %IDX%$T SET FILE_SIZE=FILE_SIZE+1 " +
        " WHERE NAME ='updateCount'";

    /**
     * SQL command to create a new a byte array storage into Lucene Domain Index table
     */
    private static final String insertParameterStmt =
        "INSERT INTO %IDX%$T (NAME,LAST_MODIFIED,FILE_SIZE,DATA,DELETED) " +
        " VALUES ('parameters',sysdate,?,?,'N')";

    /**
     * SQL command to create a new write.lock counter into Lucene Domain Index table
     */
    private static final String insertCountStmt =
        "INSERT INTO %IDX%$T (NAME,LAST_MODIFIED,FILE_SIZE,DATA,DELETED) " +
        " VALUES ('updateCount',sysdate,0,empty_blob(),'N')";

    /**
     * SQL command to create a new a byte array storage into Lucene Domain Index table
     */
    private static final String insertSolrFileStmt =
        "INSERT INTO %IDX%$T (NAME,LAST_MODIFIED,FILE_SIZE,DATA,DELETED) " +
        " VALUES (?,sysdate,?,?,'N')";

    /**
     * In memory Cache for parameter's storage
     */
    private static transient HashMap cachedParameters = new HashMap();

    @SuppressWarnings("compatibility:3462652032862853621")
    private static final long serialVersionUID = 1L;

    /**
     * In memory storage for parameters for faster response
     * each OJVM session will have his own cache
     */
    private HashMap parms = new HashMap();

    /**
     * Load from Lucene Domain Index store a parameters table and store
     * it in memory for faster response time
     * @param prefix Lucene Domain Index name with syntax OWNER.IDX_NAME usually uppercase
     * @return a Parameters storage
     */
    public static Parameters getParameters(String prefix) {
        Parameters par = (Parameters)cachedParameters.get(prefix);
        if (par == null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            Connection conn = null;
            try {
                conn = OJVMUtil.getConnection();
                stmt = conn.prepareStatement(
                              StringUtils.replace(getParameterDataStmt, "%IDX%",
                                                  prefix));
                rs = stmt.executeQuery();
                if (rs.next())
                    par = new Parameters(rs.getBytes(1));
                else
                    throw new InstantiationError("Parameters.getParamaters: Oops this index '" +
                                                 prefix +
                                                 "' has no parameters, re-create it.");
                cachedParameters.put(prefix, par);
                //System.out.println(".getParameters: load from DB for index: "+prefix);
            } catch (SQLException sqe) {
                throw new InstantiationError(sqe.getMessage());
            } finally {
                OJVMUtil.closeDbResources(stmt, rs);
            }
        }
        return par;
    }

    /**
     * Refresh in-memory cache of parameters
     * User want to call this method when other session re-create a Lucene Domain
     * Index with different parameters.
     * This method only clear a HashMap and when Lucene Domain Index needs a Parameters
     * storage for an index simply it will reloaded from his storage table
     */
    public static void refreshCache() {
        cachedParameters.clear();
        solrServerList = null;
        solrFlashbackServerList = null;
        luceneServerList = null;
        luceneUpdaterList = null;
    }

    /**
     * Refresh in-memory cache of parameters for a given index prefix
     * User want to call this method when other session re-create a Lucene Domain
     * Index with different parameters.
     * This method only removes the parameter structure of an specific index
     */
    public static void refreshCache(String prefix) {
        if (cachedParameters.containsKey(prefix)) // sanity checks
            cachedParameters.remove(prefix);
    }

    public Parameters() {
    }

    /**
     * Create a in-memory parameter storage using a byte array serializing version
     * @param st array of bytes with a serialize versio of the object
     * @throws SQLException
     */
    public Parameters(byte[] st) throws SQLException {
        try {
            ByteArrayInputStream i = new ByteArrayInputStream(st);
            ObjectInputStream in = new ObjectInputStream(i);
            int size_t = in.readInt();
            for (int j = 0; j < size_t; j++) {
                String name = (String)in.readObject();
                String value = (String)in.readObject();
                parms.put(name, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        }
    }

    /**
     * Set a name,value pair parameter
     * @param name
     * @param value
     */
    public void setParameter(String name, String value) {
        parms.put(name, value);
    }

    /**
     * get a parameter value using name as key and prefix
     * @param prefix index name using syntax OWNER.IDX_NAME usually uppercase
     * @param name Lucene Domain Index parameter name
     * @return
     */
    public static String getParameterByIndex(String prefix, String name) {
        Parameters par = Parameters.getParameters(prefix);
        return par.getParameter(name);
    }

    /**
     * get a parameter value using name as key
     * @param name
     * @return
     */
    public String getParameter(String name) {
        return (String)parms.get(name);
    }

    /**
     * remove an stored parameter by using a given name as key
     * @param name
     */
    public void removeParameter(String name) {
        parms.remove(name);
    }

    /**
     * get a parameter value using name as key
     * if this parameter doesn't exist return a default value
     * @param name
     * @param dfltValue
     * @return
     */
    public String getParameter(String name, String dfltValue) {
        String result = (String)parms.get(name);
        if (result == null || result.length() == 0)
            return dfltValue;
        else
            return result;
    }

    /**
     * return a number a parameter stored
     * @return
     */
    public int getSize() {
        return this.parms.size();
    }

    /**
     * Return a serialized version of the parameter store to save in BLOB column
     * @return
     * @throws SQLException
     */
    public byte[] getBytes() throws SQLException {
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(o);
            int size = parms.size();
            out.writeInt(size);
            Set keys = parms.keySet();
            Iterator ite = keys.iterator();
            while (ite.hasNext()) {
                String name = (String)ite.next();
                String value = (String)parms.get(name);
                out.writeObject(name);
                out.writeObject(value);
            }
            out.flush();
            return o.toByteArray();
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    /**
     * Pretty print all user parameters
     * if its >=1000 return only parameters name used
     * @return
     */
    public String toString() {
        String parStr = getUserParameters();
        if (parStr.length() > 1000) {
            StringBuffer result =
                new StringBuffer("ExtendedParameters:true;ParamList:");
            Set keys = parms.keySet();
            Iterator ite = keys.iterator();
            while (ite.hasNext()) {
                String name = (String)ite.next();
                // do not return internal parameters
                if (isValidParameterName(name))
                    result.append(name).append(",");
            }
            return result.toString().substring(0,result.toString().length()-1);
        } else
            return parStr;
    }

    /**
     * Pretty print all user's parameters
     * @return
     */
    public String getUserParameters() {
        if (parms.isEmpty()) // Sanity checks
            return "";
        StringBuffer result = new StringBuffer();
        Set keys = parms.keySet();
        Iterator ite = keys.iterator();
        while (ite.hasNext()) {
            String name = (String)ite.next();
            String value;
            if (isValidParameterName(name)) {
                // do not return internal parameters
                value = (String)parms.get(name);
                result.append(name).append(":");
                result.append(value).append(";");
            }
        }
        return result.toString().substring(0,result.toString().length()-1);
    }

    /**
     * Save this parameter instance into a prefix OJVMDirectory table
     * @param prefix
     */
    public void save(String prefix) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        try {
            conn = OJVMUtil.getConnection();
            stmt = conn.prepareStatement(
                         StringUtils.replace(countParameterSelectStmt, "%IDX%",
                                             prefix));
            rs = stmt.executeQuery();
            boolean exists = rs.next();
            rs.close();
            rs = null;
            stmt.close();
            if (exists) { // update
                stmt = conn.prepareStatement(
                            StringUtils.replace(updateParameterStmt, "%IDX%",
                                                prefix));
                stmt.setInt(1, this.getSize());
                stmt.setBytes(2, this.getBytes());
                stmt.execute();
                stmt.close();
                stmt = conn.prepareStatement(StringUtils.replace(
                                             updateCountStmt, "%IDX%", prefix));
                stmt.execute();
            } else {
                stmt = conn.prepareStatement(
                               StringUtils.replace(insertParameterStmt, "%IDX%",
                                                   prefix));
                stmt.setInt(1, this.getSize());
                stmt.setBytes(2, this.getBytes());
                stmt.execute();
                stmt.close();
                stmt = conn.prepareStatement(
                         StringUtils.replace(insertCountStmt, "%IDX%", prefix));
                stmt.execute();
                cachedParameters.put(prefix, this);
            }
        } catch (SQLException sqe) {
            throw new InstantiationError(sqe.getMessage());
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
    }

    /**
     * Verify a given parameter name against a list of valid parameters
     * @param parameterName
     * @return true or false if is valid parameter
     */
    public static boolean isValidParameterName(String parameterName) {
        for (int i = 0; i < paramList.length; i++)
            if (paramList[i].equals(parameterName))
                return true;
        return false;
    }

    /**
     * Verify a given parameter name against a list of internal parameters
     * @param parameterName
     * @return true or false if is internal parameter
     */
    public static boolean isInternalParameterName(String parameterName) {
        for (int i = 0; i < internalList.length; i++)
            if (internalList[i].equals(parameterName))
                return true;
        return false;
    }

    /**
     * @param prefix
     */
    public static void saveSolrDefaultConfig(String prefix) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = OJVMUtil.getConnection();
            stmt =
            conn.prepareStatement(StringUtils.replace(insertSolrFileStmt, "%IDX%",
                                      prefix));
            for (int i=0;i<solrConfFiles.length;i++) {
                BLOB b = BLOB.createTemporary(conn, false, BLOB.DURATION_CALL);
                b.setBytes(1,solrConfFiles[i][1].getBytes());
                stmt.setString(1, solrConfFiles[i][0]);
                stmt.setInt(2, solrConfFiles[i][1].length());
                stmt.setBlob(3, b);
                stmt.execute();
                b.freeTemporary();
            }
        } catch (SQLException sqe) {
            throw new InstantiationError(sqe.getMessage());
        } finally {
            OJVMUtil.closeDbResources(stmt, null);
        }
        
    }

    /**
     * Choose a Solr searcher using Random value ID
     * @return an string of the form host:port to connect
     */
    public String getSolrRandomSearcher() {
        String hostListStr = getParameter("Searcher", "0");
        String[] rmtSearcherHostIDs = hostListStr.split(",");
        int rndSearcherIdx = RANDOM.nextInt(rmtSearcherHostIDs.length);
        int rndSearcher = Integer.parseInt(rmtSearcherHostIDs[rndSearcherIdx]);
        //logger.info("getSolrRandomSearcher, rmtSearcherHosts.length: " + rmtSearcherHostIDs.length + " choosing: " + rndSearcher);
        return getSolrServerById(rndSearcher);
    }

    /**
     * Choose a Lucene updater using parameter ID
     * @return an string of the form host:port to connect
     */
    public String getSolrUpdater() {
        String updaterID = getParameter("Updater", "0");
        //logger.info("getSolrUpdater: " + updaterID);
        try {
            int id = Integer.parseInt(updaterID);
            return getSolrServerById(id);
        } catch (NumberFormatException e) {
            return getSolrServerById(0);
        }
    }

    /**
     * Choose a Lucene searcher using Random value ID
     * @return an string of the form host:port to connect
     */
    public String getLuceneRandomSearcher() {
        String hostListStr = getParameter("Searcher", "local");
        if ("local".equalsIgnoreCase(hostListStr))
            return "local";
        String[] rmtSearcherHostIDs = hostListStr.split(",");
        int rndSearcherIdx = RANDOM.nextInt(rmtSearcherHostIDs.length);
        int rndSearcher = Integer.parseInt(rmtSearcherHostIDs[rndSearcherIdx]);
        //logger.info("getLuceneRandomSearcher, rmtSearcherHosts.length: " + rmtSearcherHostIDs.length + " choosing: " + rndSearcher);
        return getLuceneSearcherById(rndSearcher);
    }

    /**
     * Choose a Lucene updater using parameter ID
     * @return an string of the form host:port to connect
     */
    public String getLuceneUpdater() {
        String updaterID = getParameter("Updater", "local");
        //logger.info("getLuceneUpdater: " + updaterID);
        if ("local".equalsIgnoreCase(updaterID))
            return "local";
        try {
            int id = Integer.parseInt(updaterID);
            return getLuceneUpdaterById(id);
        } catch (NumberFormatException e) {
            return getLuceneUpdaterById(0);
        }
    }

    /**
     * Choose a Lucene searcher using
     * @param id which is an index at the table LUCENE.FB_PROCESS
     * @return an string of the form host:port to connect
     */
    public String getLuceneSearcherById(int id) {
        if (luceneServerList == null) {
            // Populate here with server list
            ArrayList<String> luceneServerListTmp = new ArrayList<String>();
            PreparedStatement stmt = null;
            ResultSet rs = null;
            Connection conn = null;
            try {
                conn = OJVMUtil.getConnection();
                stmt = conn.prepareStatement(selServerDataStmt);
                stmt.setString(1, "IndexScanServ");
                rs = stmt.executeQuery();
                while (rs.next())
                    luceneServerListTmp.add(rs.getString(1));
                luceneServerList = luceneServerListTmp.toArray(new String[] {});
            } catch (SQLException e) {
                logger.error("Error trying to lookup Lucene server", e);
            } finally {
                OJVMUtil.closeDbResources(stmt, rs);
            }
            if (luceneServerList.length == 0) // Default list of Scan server
                luceneServerList = new String[] { "localhost:1099" };
            logger.trace("luceneServerList loaded from DB: " + luceneServerList);
        }
        if (id >= luceneServerList.length || id < 0) // Sanity checks
            return luceneServerList[0];
        else
            return luceneServerList[id];
    }

    /**
     * Choose a Lucene updater using
     * @param id which is an index at the table LUCENE.FB_PROCESS
     * @return an string of the form host:port to connect
     */
    public String getLuceneUpdaterById(int id) {
        if (luceneUpdaterList == null) {
            // Populate here with server list
            ArrayList<String> luceneUpdaterListTmp = new ArrayList<String>();
            PreparedStatement stmt = null;
            ResultSet rs = null;
            Connection conn = null;
            try {
                conn = OJVMUtil.getConnection();
                stmt = conn.prepareStatement(selServerDataStmt);
                stmt.setString(1, "IndexUpdateServ");
                rs = stmt.executeQuery();
                while (rs.next())
                    luceneUpdaterListTmp.add(rs.getString(1));
                luceneUpdaterList = luceneUpdaterListTmp.toArray(new String[] {});
            } catch (SQLException e) {
                logger.error("Error trying to lookup Lucene server", e);
            } finally {
                OJVMUtil.closeDbResources(stmt, rs);
            }
            if (luceneUpdaterList.length == 0) // Default list of Updater server
                luceneUpdaterList = new String[] { "localhost:1098" };
            logger.trace("luceneUpdaterList loaded from DB: " + luceneUpdaterList);
        }
        if (id >= luceneUpdaterList.length || id < 0) // Sanity checks
            return luceneUpdaterList[0];
        else
            return luceneUpdaterList[id];
    }

    /**
     * Choose a Solr updater using
     * @param id which is an index at the table LUCENE.FB_PROCESS
     * @return an string of the form host:port to connect
     */
    public String getSolrServerById(int id) {
        if (solrServerList == null) {
            // Populate here with server list
            ArrayList<String> solrServerListTmp = new ArrayList<String>();
            PreparedStatement stmt = null;
            ResultSet rs = null;
            Connection conn = null;
            try {
                conn = OJVMUtil.getConnection();
                stmt = conn.prepareStatement(selServerDataStmt);
                stmt.setString(1, "SolrServlet");
                rs = stmt.executeQuery();
                while (rs.next())
                    solrServerListTmp.add(rs.getString(1));
                solrServerList = solrServerListTmp.toArray(new String[] {});
            } catch (SQLException e) {
                logger.error("Error trying to lookup Solr server", e);
            } finally {
                OJVMUtil.closeDbResources(stmt, rs);
            }
            if (solrServerList.length == 0) // Default list of Solr server
                solrServerList = new String[] { "localhost:9099" };
            logger.trace("solrServerList loaded from DB: " + solrServerList);
        }
        if (id >= solrServerList.length || id < 0) // Sanity checks
            return solrServerList[0];
        else
            return solrServerList[id];
    }

    /**
     * Choose a Flashback searcher using
     * @param id which is an index at the table LUCENE.FB_PROCESS
     * @return an string of the form host:port to connect
     */
    public String getFlashbackSearcherById(int id) {
        if (solrFlashbackServerList == null) {
            ArrayList<String> solrFlashServerListTmp = new ArrayList<String>();
            PreparedStatement stmt = null;
            ResultSet rs = null;
            Connection conn = null;
            try {
                conn = OJVMUtil.getConnection();
                stmt = conn.prepareStatement(selFlashbackServStmt);
                rs = stmt.executeQuery();
                while (rs.next())
                    solrFlashServerListTmp.add(rs.getString(1));
                solrFlashbackServerList =
                        solrFlashServerListTmp.toArray(new String[] {});
            } catch (Exception e) {
                logger.error("Error trying to lookup Flashback server", e);
            } finally {
                OJVMUtil.closeDbResources(stmt, rs);
            }
            if (solrFlashbackServerList.length == 0) // Default list of Flashback server
                solrFlashbackServerList = new String[] { "localhost:9400" };
            logger.trace("solrFlashbackServerList loaded from DB: " + solrFlashbackServerList);
        }
        if (id >= solrFlashbackServerList.length || id < 0) // Sanity checks
            return solrFlashbackServerList[0];
        else
            return solrFlashbackServerList[id];
    }
}
