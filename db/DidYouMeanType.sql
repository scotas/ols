
---------------------------------------------------------------------
--    LUCENE Did You Mean Suggestion List implementation  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('didyoumean_term_ot','ldidyoumean');
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

CREATE OR REPLACE TYPE didyoumean_term_ot AUTHID CURRENT_USER AS OBJECT
(
key INTEGER,

STATIC FUNCTION ODCITableStart(sctx OUT didyoumean_term_ot, 
                               index_name varchar2,
                               cmpval  varchar2,
		       	       numSug	NUMBER,		
                               highlight varchar2, 
			       distancealg  varchar2) RETURN NUMBER,

STATIC FUNCTION ODCITableStart(sctx OUT didyoumean_term_ot,
       			       owner	varchar2,		
                               index_name varchar2,
                               cmpval  varchar2,
		       	       numSug	NUMBER,		
                               highlight varchar2, 
			       distancealg  varchar2) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.search.spell.ojvm.SpellChecker.ODCITableStart(
              oracle.sql.STRUCT[],
	      java.lang.String,	
              java.lang.String,
              java.lang.String,
              java.math.BigDecimal,
              java.lang.String,
	      java.lang.String) return java.math.BigDecimal',

  
MEMBER FUNCTION ODCITableFetch(self IN OUT didyoumean_term_ot, nrows IN NUMBER, outSet OUT term_info_set) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.search.spell.ojvm.SpellChecker.ODCITableFetch(
              java.math.BigDecimal, 
              oracle.sql.ARRAY[]) return java.math.BigDecimal',

MEMBER FUNCTION ODCITableClose(self IN didyoumean_term_ot) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.search.spell.ojvm.SpellChecker.ODCITableClose() return java.math.BigDecimal',


STATIC FUNCTION ODCIGetInterfaces (
                     	p_interfaces OUT SYS.ODCIObjectList
                     ) RETURN NUMBER,

     STATIC FUNCTION ODCIStatsTableFunction (
                        p_function    IN  SYS.ODCIFuncInfo,
                        p_stats       OUT SYS.ODCITabFuncStats,
                        p_args        IN  SYS.ODCIArgDescList,
                     	p_index_name  IN  VARCHAR2,
		     	p_cmpval      IN  VARCHAR2,
		     	p_numSug      IN  NUMBER,
		     	p_highlight   IN  VARCHAR2,
		     	p_distancealg  IN  VARCHAR2
                    ) RETURN NUMBER		
);
/
show errors

create function ldidyoumean(index_name VARCHAR2,
                              cmpval  VARCHAR2,
                              numSug NUMBER DEFAULT 10,
                              highlight  VARCHAR2 DEFAULT null,
			      distancealg  VARCHAR2 DEFAULT 'Levenstein')  RETURN term_info_set
PIPELINED USING didyoumean_term_ot;
/

--GRANTS
grant execute on didyoumean_term_ot to public;

create public synonym ldidyoumean for lucene.ldidyoumean;
create public synonym didyoumean_term_ot for lucene.didyoumean_term_ot;

exit
