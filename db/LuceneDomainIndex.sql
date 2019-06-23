
---------------------------------------------------------------------
--    LUCENE Index Method  Implemented as Trusted Callouts  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('LContains','LScore','LHighlight','LuceneDomainIndex');
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

-- CREATE INDEXTYPE IMPLEMENTATION TYPE
create type LuceneDomainIndex authid current_user as object
(
  scanctx integer,
  STATIC FUNCTION getIndexPrefix(ia SYS.ODCIIndexInfo) RETURN VARCHAR2,
  
  STATIC FUNCTION getParameter(prefix VARCHAR2, paramName IN VARCHAR2) RETURN VARCHAR2 AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.Parameters.getParameterByIndex(
                java.lang.String, java.lang.String) return java.lang.String',

  STATIC FUNCTION TextContains(Text IN VARCHAR2, Key IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextContains(Text IN VARCHAR2, Key IN VARCHAR2, Sort IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.TextContains(
                java.lang.String, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		            java.math.BigDecimal) return java.math.BigDecimal',

  STATIC FUNCTION TextContains(Text IN CLOB, Key IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextContains(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.TextContains(
                oracle.sql.CLOB, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		            java.math.BigDecimal) return java.math.BigDecimal',

  STATIC FUNCTION TextContains(Text IN XMLTYPE, Key IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextContains(Text IN XMLTYPE, Key IN VARCHAR2, Sort IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.TextContains(
                oracle.xdb.XMLType, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		            java.math.BigDecimal) return java.math.BigDecimal',

  STATIC FUNCTION TextScore(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextScore(Text IN VARCHAR2, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.TextScore(
                java.lang.String, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		java.math.BigDecimal) return java.math.BigDecimal',

  STATIC FUNCTION TextScore(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextScore(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,
                
  STATIC FUNCTION TextScore(Text IN XMLTYPE, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextScore(Text IN XMLTYPE, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextHighlight(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2,

  STATIC FUNCTION TextHighlight(Text IN VARCHAR2, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2 AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.TextHighlight(
                java.lang.String, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		            java.math.BigDecimal) return java.lang.String',

  STATIC FUNCTION TextHighlight(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2,

  STATIC FUNCTION TextHighlight(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2 AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.TextHighlight(
                oracle.sql.CLOB, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		            java.math.BigDecimal) return java.lang.String',
                
  STATIC FUNCTION TextHighlight(Text IN XMLTYPE, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2,

  STATIC FUNCTION TextHighlight(Text IN XMLTYPE, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2 AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.TextHighlight(
                oracle.xdb.XMLType, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		            java.math.BigDecimal) return java.lang.String',

  STATIC FUNCTION ODCIGetInterfaces(ifclist OUT NOCOPY sys.ODCIObjectList) RETURN NUMBER,
  
  STATIC FUNCTION ODCIIndexCreate(ia sys.ODCIIndexInfo, parms VARCHAR2,
                                  env sys.ODCIEnv) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.ODCIIndexCreate(oracle.ODCI.ODCIIndexInfo, java.lang.String, 
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',

  STATIC FUNCTION ODCIIndexAlter(ia sys.ODCIIndexInfo, parms IN OUT NOCOPY VARCHAR2, alter_option NUMBER, env sys.ODCIEnv) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.ODCIIndexAlter(oracle.ODCI.ODCIIndexInfo, java.lang.String[], java.math.BigDecimal,
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
                
  STATIC FUNCTION ODCIIndexDrop(ia sys.ODCIIndexInfo, env sys.ODCIEnv) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.ODCIIndexDrop(oracle.ODCI.ODCIIndexInfo, 
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
                
  STATIC FUNCTION ODCIIndexTruncate(ia SYS.ODCIIndexInfo, env SYS.ODCIEnv) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.ODCIIndexTruncate(oracle.ODCI.ODCIIndexInfo, 
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
                
  -- Array DML implementation --
  STATIC FUNCTION ODCIIndexDelete(ia sys.ODCIIndexInfo, ridlist sys.ODCIRidList, env sys.ODCIEnv) RETURN NUMBER,
  
  STATIC FUNCTION ODCIIndexInsert(ia sys.ODCIIndexInfo, ridlist sys.ODCIRidList, env sys.ODCIEnv) RETURN NUMBER,
  
  STATIC FUNCTION ODCIIndexUpdate(ia sys.ODCIIndexInfo, ridlist sys.ODCIRidList, env sys.ODCIEnv) RETURN NUMBER,
  
  STATIC FUNCTION ODCIIndexUpdate(ia sys.ODCIIndexInfo, ridlist sys.ODCIRidList, oldValList sys.ODCIColArrayValList, newValList sys.ODCIColArrayValList, env sys.ODCIEnv) RETURN NUMBER,
  
  STATIC FUNCTION ODCIIndexExchangePartition(ia sys.ODCIIndexInfo, ia1 sys.ODCIIndexInfo, env sys.ODCIEnv)  RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.ODCIIndexExchangePartition(oracle.ODCI.ODCIIndexInfo,
                oracle.ODCI.ODCIIndexInfo,
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
  
  STATIC FUNCTION ODCIIndexMergePartition(ia sys.ODCIIndexInfo, 
                                          part_name1 sys.ODCIPartInfo, 
                                          part_name2 sys.ODCIPartInfo, 
                                          parms VARCHAR2, 
                                          env sys.ODCIEnv)  RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.ODCIIndexMergePartition(oracle.ODCI.ODCIIndexInfo,
                oracle.ODCI.ODCIPartInfo,
                oracle.ODCI.ODCIPartInfo,
                java.lang.String,
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
  
  STATIC FUNCTION ODCIIndexSplitPartition(ia sys.ODCIIndexInfo, 
                                          part_name1 sys.ODCIPartInfo, 
                                          part_name2 sys.ODCIPartInfo, 
                                          parms VARCHAR2, 
                                          env sys.ODCIEnv)  RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.indexer.LuceneDomainIndex.ODCIIndexSplitPartition(oracle.ODCI.ODCIIndexInfo,
                oracle.ODCI.ODCIPartInfo,
                oracle.ODCI.ODCIPartInfo,
                java.lang.String,
		            oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
  
  STATIC FUNCTION ODCIIndexStart(sctx IN OUT NOCOPY LuceneDomainIndex,
        ia SYS.ODCIIndexInfo, op SYS.ODCIPredInfo, qi sys.ODCIQueryInfo,
        strt number, stop number,
        cmppos NUMBER, cmpval VARCHAR2, env SYS.ODCIEnv) RETURN NUMBER,

  STATIC FUNCTION ODCIIndexStart(sctx IN OUT NOCOPY LuceneDomainIndex,
        ia SYS.ODCIIndexInfo, op SYS.ODCIPredInfo, qi sys.ODCIQueryInfo,
        strt number, stop number,
        cmpval VARCHAR2, env SYS.ODCIEnv) RETURN NUMBER,

  STATIC FUNCTION ODCIIndexStart(sctx IN OUT NOCOPY LuceneDomainIndex,
        ia SYS.ODCIIndexInfo, op SYS.ODCIPredInfo, qi sys.ODCIQueryInfo,
        strt number, stop number,
        cmpval VARCHAR2, sortval VARCHAR2, env SYS.ODCIEnv) RETURN NUMBER AS LANGUAGE JAVA NAME
	      'com.scotas.lucene.indexer.LuceneDomainIndex.ODCIStart(com.scotas.lucene.indexer.LuceneDomainIndex[],
                oracle.ODCI.ODCIIndexInfo, 
		            oracle.ODCI.ODCIPredInfo, 
		            oracle.ODCI.ODCIQueryInfo,
                java.math.BigDecimal, java.math.BigDecimal, 
                java.lang.String, java.lang.String, oracle.ODCI.ODCIEnv) return java.math.BigDecimal',

  MEMBER FUNCTION ODCIIndexFetch(nrows NUMBER, rids OUT NOCOPY SYS.ODCIridlist, env SYS.ODCIEnv) RETURN NUMBER,

  MEMBER FUNCTION ODCIIndexFetchInternal(nrows NUMBER, 
        rids OUT NOCOPY SYS.ODCIridlist, 
        env SYS.ODCIEnv) RETURN VARCHAR2 as LANGUAGE JAVA NAME
	      'com.scotas.lucene.indexer.LuceneDomainIndex.ODCIFetch(java.math.BigDecimal, 
                oracle.ODCI.ODCIRidList[], oracle.ODCI.ODCIEnv) return java.lang.String',

  MEMBER FUNCTION ODCIIndexClose(env SYS.ODCIEnv) RETURN NUMBER as LANGUAGE JAVA NAME
	      'com.scotas.lucene.indexer.LuceneDomainIndex.ODCIClose(oracle.ODCI.ODCIEnv) return java.math.BigDecimal',
  
  STATIC PROCEDURE sync(index_name VARCHAR2),

  STATIC PROCEDURE sync(owner VARCHAR2, index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL),

  STATIC PROCEDURE syncInternal(prefix VARCHAR2, 
        deleted sys.ODCIRidList, 
        inserted sys.ODCIRidList) as LANGUAGE JAVA NAME
	      'com.scotas.lucene.indexer.LuceneDomainIndex.sync(java.lang.String,
                oracle.ODCI.ODCIRidList, oracle.ODCI.ODCIRidList)',

  STATIC PROCEDURE optimize(index_name VARCHAR2),

  STATIC PROCEDURE optimize(owner VARCHAR2, index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL),

  STATIC PROCEDURE optimizeInternal(prefix VARCHAR2) as LANGUAGE JAVA NAME
	      'com.scotas.lucene.indexer.LuceneDomainIndex.optimize(java.lang.String)',

  STATIC PROCEDURE rebuild(index_name VARCHAR2),

  STATIC PROCEDURE rebuild(owner VARCHAR2, index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL),

  STATIC PROCEDURE rebuildInternal(prefix VARCHAR2) as LANGUAGE JAVA NAME
	      'com.scotas.lucene.indexer.LuceneDomainIndex.rebuild(java.lang.String)',

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
	      'com.scotas.lucene.indexer.LuceneDomainIndex.setLogLevel(java.lang.String)',

  STATIC PROCEDURE refreshParameterCache as LANGUAGE JAVA NAME
	      'com.scotas.lucene.indexer.Parameters.refreshCache()',

  STATIC FUNCTION countHits(index_name VARCHAR2, cmpval VARCHAR2) RETURN NUMBER,

  STATIC FUNCTION countHits(owner VARCHAR2, index_name VARCHAR2, cmpval VARCHAR2) RETURN NUMBER AS LANGUAGE JAVA NAME
	      'com.scotas.lucene.indexer.LuceneDomainIndex.countHits( 
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

CREATE OR REPLACE OPERATOR LContains
  BINDING (VARCHAR2, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT LuceneDomainIndex COMPUTE ANCILLARY DATA
  without column data USING LuceneDomainIndex.TextContains,
  (VARCHAR2, VARCHAR2, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT LuceneDomainIndex COMPUTE ANCILLARY DATA
  without column data USING LuceneDomainIndex.TextContains,
  (CLOB, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT LuceneDomainIndex COMPUTE ANCILLARY DATA
  without column data USING LuceneDomainIndex.TextContains,
  (CLOB, VARCHAR2, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT LuceneDomainIndex COMPUTE ANCILLARY DATA
  without column data USING LuceneDomainIndex.TextContains,
  (sys.XMLType, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT LuceneDomainIndex COMPUTE ANCILLARY DATA
  without column data USING LuceneDomainIndex.TextContains,
  (sys.XMLType, VARCHAR2, VARCHAR2) RETURN NUMBER
  WITH INDEX CONTEXT, SCAN CONTEXT LuceneDomainIndex COMPUTE ANCILLARY DATA
  without column data USING LuceneDomainIndex.TextContains;

show errors

CREATE OR REPLACE OPERATOR LScore BINDING 
  (NUMBER) RETURN NUMBER
    ANCILLARY TO LContains(VARCHAR2, VARCHAR2),
                 LContains(VARCHAR2, VARCHAR2, VARCHAR2),
                 LContains(CLOB, VARCHAR2),
                 LContains(CLOB, VARCHAR2, VARCHAR2),
                 LContains(sys.XMLType, VARCHAR2),
                 LContains(sys.XMLType, VARCHAR2, VARCHAR2)
    without column data USING LuceneDomainIndex.TextScore;

show errors

CREATE OR REPLACE OPERATOR LHighlight BINDING 
  (NUMBER) RETURN VARCHAR2
    ANCILLARY TO LContains(VARCHAR2, VARCHAR2),
                 LContains(VARCHAR2, VARCHAR2, VARCHAR2),
                 LContains(CLOB, VARCHAR2),
                 LContains(CLOB, VARCHAR2, VARCHAR2),
                 LContains(sys.XMLType, VARCHAR2),
                 LContains(sys.XMLType, VARCHAR2, VARCHAR2)
    USING LuceneDomainIndex.TextHighlight;

show errors

-- GRANTS
grant execute on LuceneDomainIndex to public;
grant execute on LContains to public;
grant execute on LScore to public;
grant execute on LHighlight to public;

create public synonym LuceneDomainIndex for lucene.LuceneDomainIndex;
create public synonym LContains for lucene.LContains;
create public synonym LScore for lucene.LScore;
create public synonym LHighlight for lucene.LHighlight;
