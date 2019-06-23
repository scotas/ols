
---------------------------------------------------------------------
--    LUCENE Index Method  Implemented as Trusted Callouts  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('LSimilarity','LuceneDomainIndexSimilarity');
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
create or replace type LuceneDomainIndexSimilarity authid current_user as object
(
  scanctx integer,

  STATIC FUNCTION TextSimilarity(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextSimilarity(Text IN VARCHAR2, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.search.similar.ojvm.Similarity(
                java.lang.String, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		            java.math.BigDecimal) return java.math.BigDecimal',

  STATIC FUNCTION TextSimilarity(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,
                
  STATIC FUNCTION TextSimilarity(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.search.similar.ojvm.Similarity(
                oracle.sql.CLOB, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		            java.math.BigDecimal) return java.math.BigDecimal',

  STATIC FUNCTION TextSimilarity(Text IN XMLTYPE, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER,

  STATIC FUNCTION TextSimilarity(Text IN XMLTYPE, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME
        'com.scotas.lucene.search.similar.ojvm.Similarity(
                oracle.xdb.XMLType, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		            java.math.BigDecimal) return java.math.BigDecimal'

);
/
show errors



CREATE OR REPLACE OPERATOR LSimilarity BINDING 
  (NUMBER) RETURN NUMBER
    ANCILLARY TO LContains(VARCHAR2, VARCHAR2),
                 LContains(VARCHAR2, VARCHAR2, VARCHAR2),
                 LContains(CLOB, VARCHAR2),
                 LContains(CLOB, VARCHAR2, VARCHAR2),
                 LContains(sys.XMLType, VARCHAR2),
                 LContains(sys.XMLType, VARCHAR2, VARCHAR2)
    USING LuceneDomainIndexSimilarity.TextSimilarity;

show errors


-- GRANTS

grant execute on LuceneDomainIndexSimilarity to public;
grant execute on LSimilarity to public;



create public synonym LuceneDomainIndexSimilarity for lucene.LuceneDomainIndexSimilarity;
create public synonym LSimilarity for lucene.LSimilarity;


exit
