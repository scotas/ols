
---------------------------------------------------------------------
--    LUCENE index_terms and high_freq_terms implementation  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('index_terms','index_term_ot','high_freq_terms','term_info','term_info_set','high_freq_term_ot','LFreqTerms');
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

-- drop some operators and functions depending on term_info_set structure
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop function ';
  obj_list obj_arr := obj_arr('ldidyoumean','lautocomplete','index_terms','high_freq_terms');
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

-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop type ';
  obj_list obj_arr := obj_arr('didyoumean_term_ot','LuceneDomainIndexFreqTerms','autocomplete_term_ot','high_freq_term_ot','term_info_set','term_info');
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

create or replace type term_info as object (
  term     VARCHAR2(4000),
  docFreq  NUMBER(10),
  rid      VARCHAR2(18)
);
/
show errors

CREATE OR REPLACE TYPE term_info_set AS TABLE OF term_info;
/
show errors

CREATE TYPE high_freq_term_ot AUTHID CURRENT_USER AS OBJECT
(
key INTEGER,
STATIC FUNCTION ODCITableStart(sctx OUT high_freq_term_ot, 
                               index_name VARCHAR2,
                               term_name  VARCHAR2,
                               num_terms  NUMBER) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.misc.ojvm.HighFreqTerms.ODCITableStart(
              oracle.sql.STRUCT[],
              java.lang.String,
              java.lang.String,
              java.math.BigDecimal) return java.math.BigDecimal',
  
MEMBER FUNCTION ODCITableFetch(self IN OUT high_freq_term_ot, nrows IN NUMBER, outSet OUT term_info_set) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.misc.ojvm.HighFreqTerms.ODCITableFetch(
              java.math.BigDecimal, 
              oracle.sql.ARRAY[]) return java.math.BigDecimal',

MEMBER FUNCTION ODCITableClose(self IN high_freq_term_ot) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.misc.ojvm.HighFreqTerms.ODCITableClose() return java.math.BigDecimal'
);
/
show errors

CREATE FUNCTION high_freq_terms(index_name VARCHAR2,
                       term_name  VARCHAR2,
                       num_terms  NUMBER) RETURN term_info_set
PIPELINED USING high_freq_term_ot;
/

CREATE TYPE index_term_ot AUTHID CURRENT_USER AS OBJECT
(
key INTEGER,
STATIC FUNCTION ODCITableStart(sctx OUT index_term_ot, 
                               index_name VARCHAR2,
                               term_name  VARCHAR2) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.misc.ojvm.IndexTerms.ODCITableStart(
              oracle.sql.STRUCT[],
              java.lang.String,
              java.lang.String) return java.math.BigDecimal',
  
MEMBER FUNCTION ODCITableFetch(self IN OUT index_term_ot, nrows IN NUMBER, outSet OUT term_info_set) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.misc.ojvm.IndexTerms.ODCITableFetch(
              java.math.BigDecimal, 
              oracle.sql.ARRAY[]) return java.math.BigDecimal',

MEMBER FUNCTION ODCITableClose(self IN index_term_ot) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.misc.ojvm.IndexTerms.ODCITableClose() return java.math.BigDecimal'
);
/
show errors

CREATE FUNCTION index_terms(index_name VARCHAR2,
                       term_name  VARCHAR2) RETURN term_info_set
PIPELINED USING index_term_ot;
/

-- GRANTS
grant execute on index_terms to public;
grant execute on index_term_ot to public;
grant execute on high_freq_terms to public;
grant execute on high_freq_term_ot to public;
grant execute on term_info to public;
grant execute on term_info_set to public;

create public synonym index_terms for lucene.index_terms;
create public synonym index_term_ot for lucene.index_term_ot;
create public synonym high_freq_terms for lucene.high_freq_terms;
create public synonym high_freq_term_ot for lucene.high_freq_term_ot;
create public synonym term_info for lucene.term_info;
create public synonym term_info_set for lucene.term_info_set;

exit