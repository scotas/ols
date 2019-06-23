
---------------------------------------------------------------------
--    Solr Index Method  Implemented as Trusted Callouts  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('SContains','SScore','SHighlight','SMlt','SolrDomainIndex');
BEGIN
  FOR I IN OBJ_LIST.FIRST..OBJ_LIST.LAST LOOP
  begin
    EXECUTE IMMEDIATE DRP_STMT||OBJ_LIST(I);
  EXCEPTION WHEN OTHERS THEN
    NULL;
  end;
  end loop;
END;
/

-- drop operator SScore;
-- drop operator SHighlight;
-- drop operator SMlt;
-- drop operator SContains;
-- drop type SolrDomainIndex;

-- CREATE INDEXTYPE IMPLEMENTATION TYPE
create type SolrDomainIndex authid current_user as object
(
  scanctx integer,
  STATIC FUNCTION getIndexPrefix(ia SYS.ODCIIndexInfo) RETURN VARCHAR2,
  
  STATIC FUNCTION getParameter(prefix VARCHAR2, paramName IN VARCHAR2) RETURN VARCHAR2 AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.Parameters.getParameterByIndex(
                java.lang.String, java.lang.String) return java.lang.String',

  STATIC FUNCTION TextContains(Text IN VARCHAR2, Key IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextContains(Text IN VARCHAR2, Key IN VARCHAR2, Sort IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.TextContains(
                java.lang.String, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.solr.odci.SolrDomainIndex[], 
		            java.math.BigDecimal) return java.math.BigDecimal',

  STATIC FUNCTION TextContains(Text IN CLOB, Key IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextContains(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.TextContains(
                oracle.sql.CLOB, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.solr.odci.SolrDomainIndex[], 
		            java.math.BigDecimal) return java.math.BigDecimal',

  STATIC FUNCTION TextContains(Text IN XMLTYPE, Key IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextContains(Text IN XMLTYPE, Key IN VARCHAR2, Sort IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.TextContains(
                oracle.xdb.XMLType, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.solr.odci.SolrDomainIndex[], 
		            java.math.BigDecimal) return java.math.BigDecimal',

  STATIC FUNCTION TextScore(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextScore(Text IN VARCHAR2, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.TextScore(
                java.lang.String, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.solr.odci.SolrDomainIndex[], 
		java.math.BigDecimal) return java.math.BigDecimal',

  STATIC FUNCTION TextScore(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextScore(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER,
                
  STATIC FUNCTION TextScore(Text IN XMLTYPE, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextScore(Text IN XMLTYPE, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextHighlight(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2,

  STATIC FUNCTION TextHighlight(Text IN VARCHAR2, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2 AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.TextHighlight(
                java.lang.String, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.solr.odci.SolrDomainIndex[], 
		            java.math.BigDecimal) return java.lang.String',

  STATIC FUNCTION TextHighlight(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2,

  STATIC FUNCTION TextHighlight(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2 AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.TextHighlight(
                oracle.sql.CLOB, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.solr.odci.SolrDomainIndex[], 
		            java.math.BigDecimal) return java.lang.String',
                
  STATIC FUNCTION TextHighlight(Text IN XMLTYPE, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2,

  STATIC FUNCTION TextHighlight(Text IN XMLTYPE, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2 AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.TextHighlight(
                oracle.xdb.XMLType, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.solr.odci.SolrDomainIndex[], 
		            java.math.BigDecimal) return java.lang.String',

  STATIC FUNCTION TextMlt(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN sys.ODCIRidList,

  STATIC FUNCTION TextMlt(Text IN VARCHAR2, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN sys.ODCIRidList AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.TextMlt(
                java.lang.String, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.solr.odci.SolrDomainIndex[], 
		            java.math.BigDecimal) return oracle.ODCI.ODCIRidList',

  STATIC FUNCTION TextMlt(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN sys.ODCIRidList,

  STATIC FUNCTION TextMlt(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN sys.ODCIRidList AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.TextMlt(
                oracle.sql.CLOB, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.solr.odci.SolrDomainIndex[], 
		            java.math.BigDecimal) return oracle.ODCI.ODCIRidList',
                
  STATIC FUNCTION TextMlt(Text IN XMLTYPE, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN sys.ODCIRidList,

  STATIC FUNCTION TextMlt(Text IN XMLTYPE, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN sys.ODCIRidList AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.TextMlt(
                oracle.xdb.XMLType, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.solr.odci.SolrDomainIndex[], 
		            java.math.BigDecimal) return oracle.ODCI.ODCIRidList',

  STATIC FUNCTION ODCIGetInterfaces(ifclist OUT NOCOPY sys.ODCIObjectList) RETURN NUMBER,
  
  STATIC FUNCTION ODCIIndexCreate(ia sys.ODCIIndexInfo, parms VARCHAR2,
                                  env sys.ODCIEnv) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.ODCIIndexCreate(oracle.ODCI.ODCIIndexInfo, java.lang.String, 
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',

  STATIC FUNCTION ODCIIndexAlter(ia sys.ODCIIndexInfo, parms IN OUT NOCOPY VARCHAR2, alter_option NUMBER, env sys.ODCIEnv) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.ODCIIndexAlter(oracle.ODCI.ODCIIndexInfo, java.lang.String[], java.math.BigDecimal,
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
                
  STATIC FUNCTION ODCIIndexDrop(ia sys.ODCIIndexInfo, env sys.ODCIEnv) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.ODCIIndexDrop(oracle.ODCI.ODCIIndexInfo, 
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
                
  STATIC FUNCTION ODCIIndexTruncate(ia SYS.ODCIIndexInfo, env SYS.ODCIEnv) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.ODCIIndexTruncate(oracle.ODCI.ODCIIndexInfo, 
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
                
  -- Array DML implementation --  
  STATIC FUNCTION ODCIIndexDelete(ia sys.ODCIIndexInfo, ridlist sys.ODCIRidList, env sys.ODCIEnv) RETURN NUMBER,
  
  STATIC FUNCTION ODCIIndexInsert(ia sys.ODCIIndexInfo, ridlist sys.ODCIRidList, env sys.ODCIEnv) RETURN NUMBER,
  
  STATIC FUNCTION ODCIIndexUpdate(ia sys.ODCIIndexInfo, ridlist sys.ODCIRidList, env sys.ODCIEnv) RETURN NUMBER,
  
  STATIC FUNCTION ODCIIndexUpdate(ia sys.ODCIIndexInfo, ridlist sys.ODCIRidList, oldValList sys.ODCIColArrayValList, newValList sys.ODCIColArrayValList, env sys.ODCIEnv) RETURN NUMBER,

  STATIC FUNCTION ODCIIndexExchangePartition(ia sys.ODCIIndexInfo, ia1 sys.ODCIIndexInfo, env sys.ODCIEnv)  RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.ODCIIndexExchangePartition(oracle.ODCI.ODCIIndexInfo,
                oracle.ODCI.ODCIIndexInfo,
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
  
  STATIC FUNCTION ODCIIndexMergePartition(ia sys.ODCIIndexInfo, 
                                          part_name1 sys.ODCIPartInfo, 
                                          part_name2 sys.ODCIPartInfo, 
                                          parms VARCHAR2, 
                                          env sys.ODCIEnv)  RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.ODCIIndexMergePartition(oracle.ODCI.ODCIIndexInfo,
                oracle.ODCI.ODCIPartInfo,
                oracle.ODCI.ODCIPartInfo,
                java.lang.String,
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
  
  STATIC FUNCTION ODCIIndexSplitPartition(ia sys.ODCIIndexInfo, 
                                          part_name1 sys.ODCIPartInfo, 
                                          part_name2 sys.ODCIPartInfo, 
                                          parms VARCHAR2, 
                                          env sys.ODCIEnv)  RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.solr.odci.SolrDomainIndex.ODCIIndexSplitPartition(oracle.ODCI.ODCIIndexInfo,
                oracle.ODCI.ODCIPartInfo,
                oracle.ODCI.ODCIPartInfo,
                java.lang.String,
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
  
  STATIC FUNCTION ODCIIndexStart(sctx IN OUT NOCOPY SolrDomainIndex,
        ia SYS.ODCIIndexInfo, op SYS.ODCIPredInfo, qi sys.ODCIQueryInfo,
        strt number, stop number,
        cmppos NUMBER, cmpval VARCHAR2, env SYS.ODCIEnv) RETURN NUMBER,

  STATIC FUNCTION ODCIIndexStart(sctx IN OUT NOCOPY SolrDomainIndex,
        ia SYS.ODCIIndexInfo, op SYS.ODCIPredInfo, qi sys.ODCIQueryInfo,
        strt number, stop number,
        cmpval VARCHAR2, env SYS.ODCIEnv) RETURN NUMBER,

  STATIC FUNCTION ODCIIndexStart(sctx IN OUT NOCOPY SolrDomainIndex,
        ia SYS.ODCIIndexInfo, op SYS.ODCIPredInfo, qi sys.ODCIQueryInfo,
        strt number, stop number,
        cmpval VARCHAR2, sortval VARCHAR2, env SYS.ODCIEnv) RETURN NUMBER AS LANGUAGE JAVA NAME
	      'com.scotas.solr.odci.SolrDomainIndex.ODCIStart(com.scotas.solr.odci.SolrDomainIndex[],
                oracle.ODCI.ODCIIndexInfo, 
		            oracle.ODCI.ODCIPredInfo, 
		            oracle.ODCI.ODCIQueryInfo,
                java.math.BigDecimal, java.math.BigDecimal, 
                java.lang.String, java.lang.String, oracle.ODCI.ODCIEnv) return java.math.BigDecimal',

  MEMBER FUNCTION ODCIIndexFetch(nrows NUMBER, rids OUT NOCOPY SYS.ODCIridlist, env SYS.ODCIEnv) RETURN NUMBER,

  MEMBER FUNCTION ODCIIndexFetchInternal(nrows NUMBER, 
        rids OUT NOCOPY SYS.ODCIridlist, 
        env SYS.ODCIEnv) RETURN VARCHAR2 as LANGUAGE JAVA NAME
	      'com.scotas.solr.odci.SolrDomainIndex.ODCIFetch(java.math.BigDecimal, 
                oracle.ODCI.ODCIRidList[], oracle.ODCI.ODCIEnv) return java.lang.String',

  MEMBER FUNCTION ODCIIndexClose(env SYS.ODCIEnv) RETURN NUMBER as LANGUAGE JAVA NAME
	      'com.scotas.solr.odci.SolrDomainIndex.ODCIClose(oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
  
  STATIC PROCEDURE sync(index_name VARCHAR2),

  STATIC PROCEDURE sync(owner VARCHAR2, index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL),

  STATIC PROCEDURE syncInternal(prefix VARCHAR2, 
        deleted sys.ODCIRidList, 
        inserted sys.ODCIRidList) as LANGUAGE JAVA NAME
	      'com.scotas.solr.odci.SolrDomainIndex.sync(java.lang.String,
                oracle.ODCI.ODCIRidList, oracle.ODCI.ODCIRidList)',

  STATIC PROCEDURE optimize(index_name VARCHAR2),

  STATIC PROCEDURE optimize(owner VARCHAR2, index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL),

  STATIC PROCEDURE optimizeInternal(prefix VARCHAR2) as LANGUAGE JAVA NAME
	      'com.scotas.solr.odci.SolrDomainIndex.optimize(java.lang.String)',

  STATIC PROCEDURE rebuild(index_name VARCHAR2),

  STATIC PROCEDURE rebuild(owner VARCHAR2, index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL),

  STATIC PROCEDURE rebuildInternal(prefix VARCHAR2) as LANGUAGE JAVA NAME
	      'com.scotas.solr.odci.SolrDomainIndex.rebuild(java.lang.String)',

  STATIC PROCEDURE msgCallBack(context  IN  RAW, 
                               reginfo  IN  SYS.AQ$_REG_INFO, 
                               descr    IN  SYS.AQ$_DESCRIPTOR, 
                               payload  IN  RAW,
                               payloadl IN  NUMBER),

  STATIC PROCEDURE enableCallBack(prefix VARCHAR2),
  STATIC PROCEDURE disableCallBack(prefix VARCHAR2),
  STATIC PROCEDURE enqueueChange(prefix VARCHAR2, rid VARCHAR2, operation VARCHAR2),
  STATIC PROCEDURE enqueueChange(prefix VARCHAR2, ridlist sys.ODCIRidList, operation VARCHAR2),
  STATIC PROCEDURE createTable(prefix VARCHAR2, lobStorageParam VARCHAR2),
  STATIC PROCEDURE dropTable(prefix VARCHAR2),
  STATIC FUNCTION  ResourceExists(path VARCHAR2) RETURN BOOLEAN,
  STATIC PROCEDURE createXdbDir(dir VARCHAR2),
  STATIC PROCEDURE xdbExport(index_name VARCHAR2),
  STATIC PROCEDURE xdbExport(owner VARCHAR2, index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL),
  STATIC PROCEDURE setLogLevel(logLevel VARCHAR2) as LANGUAGE JAVA NAME
	      'com.scotas.solr.odci.SolrDomainIndex.setLogLevel(java.lang.String)',

  STATIC PROCEDURE refreshParameterCache as LANGUAGE JAVA NAME
	      'com.scotas.lucene.indexer.Parameters.refreshCache()',

  STATIC FUNCTION countHits(index_name VARCHAR2, cmpval VARCHAR2) RETURN NUMBER,

  STATIC FUNCTION countHits(owner VARCHAR2, index_name VARCHAR2, cmpval VARCHAR2) RETURN NUMBER AS LANGUAGE JAVA NAME
	      'com.scotas.solr.odci.SolrDomainIndex.countHits( 
                java.lang.String,
                java.lang.String, 
                java.lang.String) return java.math.BigDecimal',

  PRAGMA RESTRICT_REFERENCES (getIndexPrefix, WNDS, WNPS, RNDS, RNPS),
  PRAGMA RESTRICT_REFERENCES (TextContains, WNDS, RNDS, TRUST),
  PRAGMA RESTRICT_REFERENCES (ODCIIndexStart, WNDS, RNDS, TRUST),
  PRAGMA RESTRICT_REFERENCES (ODCIIndexFetch, WNDS, RNDS, TRUST),
  PRAGMA RESTRICT_REFERENCES (ODCIIndexClose, WNDS, RNDS, TRUST),
  PRAGMA RESTRICT_REFERENCES (ODCIIndexFetchInternal, WNDS, RNDS, TRUST),
  PRAGMA RESTRICT_REFERENCES (countHits, WNDS, RNDS, TRUST),
  PRAGMA RESTRICT_REFERENCES (refreshParameterCache, WNDS, RNDS),
  PRAGMA RESTRICT_REFERENCES (getParameter, WNDS, WNPS)
);
/
show errors

CREATE OR REPLACE OPERATOR SContains
  BINDING (VARCHAR2, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT SolrDomainIndex COMPUTE ANCILLARY DATA
  without column data USING SolrDomainIndex.TextContains,
  (VARCHAR2, VARCHAR2, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT SolrDomainIndex COMPUTE ANCILLARY DATA
  without column data USING SolrDomainIndex.TextContains,
  (CLOB, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT SolrDomainIndex COMPUTE ANCILLARY DATA
  without column data USING SolrDomainIndex.TextContains,
  (CLOB, VARCHAR2, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT SolrDomainIndex COMPUTE ANCILLARY DATA
  without column data USING SolrDomainIndex.TextContains,
  (sys.XMLType, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT SolrDomainIndex COMPUTE ANCILLARY DATA
  without column data USING SolrDomainIndex.TextContains,
  (sys.XMLType, VARCHAR2, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT SolrDomainIndex COMPUTE ANCILLARY DATA
  without column data USING SolrDomainIndex.TextContains;

show errors

CREATE OR REPLACE OPERATOR SScore BINDING 
  (NUMBER) RETURN NUMBER
    ANCILLARY TO SContains(VARCHAR2, VARCHAR2),
                 SContains(VARCHAR2, VARCHAR2, VARCHAR2),
                 SContains(CLOB, VARCHAR2),
                 SContains(CLOB, VARCHAR2, VARCHAR2),
                 SContains(sys.XMLType, VARCHAR2),
                 SContains(sys.XMLType, VARCHAR2, VARCHAR2)
    without column data USING SolrDomainIndex.TextScore;

show errors

CREATE OR REPLACE OPERATOR SHighlight BINDING 
  (NUMBER) RETURN VARCHAR2
    ANCILLARY TO SContains(VARCHAR2, VARCHAR2),
                 SContains(VARCHAR2, VARCHAR2, VARCHAR2),
                 SContains(CLOB, VARCHAR2),
                 SContains(CLOB, VARCHAR2, VARCHAR2),
                 SContains(sys.XMLType, VARCHAR2),
                 SContains(sys.XMLType, VARCHAR2, VARCHAR2)
    USING SolrDomainIndex.TextHighlight;

show errors

CREATE OR REPLACE OPERATOR SMlt BINDING 
  (NUMBER) RETURN sys.ODCIRidList
    ANCILLARY TO SContains(VARCHAR2, VARCHAR2),
                 SContains(VARCHAR2, VARCHAR2, VARCHAR2),
                 SContains(CLOB, VARCHAR2),
                 SContains(CLOB, VARCHAR2, VARCHAR2),
                 SContains(sys.XMLType, VARCHAR2),
                 SContains(sys.XMLType, VARCHAR2, VARCHAR2)
    USING SolrDomainIndex.TextMlt;

show errors

-- GRANTS
grant execute on SolrDomainIndex to public;
grant execute on SContains to public;
grant execute on SScore to public;
grant execute on SHighlight to public;
grant execute on SMlt to public;

create public synonym SolrDomainIndex for lucene.SolrDomainIndex;
create public synonym SContains for lucene.SContains;
create public synonym SScore for lucene.SScore;
create public synonym SHighlight for lucene.SHighlight;
create public synonym SMlt for lucene.SMlt;
