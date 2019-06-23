
---------------------------------------------------------------------
--    LUCENE Index Method  Implemented as Trusted Callouts  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('LuceneDomainIndexFreqTerms');
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
create type LuceneDomainIndexFreqTerms authid current_user as object
(
  scanctx integer,

  STATIC FUNCTION TextFreqTerms(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set,

  STATIC FUNCTION TextFreqTerms(Text IN VARCHAR2, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set AS LANGUAGE JAVA NAME
        'com.scotas.lucene.misc.ojvm.HighFreqTerms(
                java.lang.String, java.lang.String, java.lang.String,
                oracle.ODCI.ODCIIndexCtx, com.scotas.lucene.indexer.LuceneDomainIndex[], 
		            java.math.BigDecimal) return oracle.sql.ARRAY',

  STATIC FUNCTION TextFreqTerms(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set,

  STATIC FUNCTION TextFreqTerms(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set,
                
  STATIC FUNCTION TextFreqTerms(Text IN XMLTYPE, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set,

  STATIC FUNCTION TextFreqTerms(Text IN XMLTYPE, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set
);
/
show errors



CREATE OR REPLACE OPERATOR LFreqTerms BINDING 
  (NUMBER) RETURN term_info_set
    ANCILLARY TO LContains(VARCHAR2, VARCHAR2),
                 LContains(VARCHAR2, VARCHAR2, VARCHAR2),
                 LContains(CLOB, VARCHAR2),
                 LContains(CLOB, VARCHAR2, VARCHAR2),
                 LContains(sys.XMLType, VARCHAR2),
                 LContains(sys.XMLType, VARCHAR2, VARCHAR2)
    USING LuceneDomainIndexFreqTerms.TextFreqTerms;

show errors

-- GRANTS

grant execute on LuceneDomainIndexFreqTerms to public;
grant execute on LFreqTerms to public;



create public synonym LuceneDomainIndexFreqTerms for lucene.LuceneDomainIndexFreqTerms;
create public synonym LFreqTerms for lucene.LFreqTerms;


exit
