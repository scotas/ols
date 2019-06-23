
---------------------------------------------------------------------
--    LUCENE auto complete implementation  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('autocomplete_term_ot','lautocomplete');
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

CREATE TYPE autocomplete_term_ot AUTHID CURRENT_USER AS OBJECT
(
key INTEGER,
STATIC FUNCTION ODCITableStart(sctx OUT autocomplete_term_ot, 
                               index_name varchar2,
                               term_name  varchar2, /* default value DefaultColumn parameter */
                               term_value varchar2, /* default value __ALL__ */
			                         num_terms  NUMBER    /* default 10 */) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.search.autocomplete.ojvm.AutoComplete.ODCITableStart(
              oracle.sql.STRUCT[],
              java.lang.String,
              java.lang.String,
              java.lang.String,
	            java.math.BigDecimal) return java.math.BigDecimal',

STATIC FUNCTION ODCITableStart(sctx OUT autocomplete_term_ot, 
                               index_name varchar2,
                               term_name  varchar2, /* default value DefaultColumn parameter */
                               term_value varchar2, /* default value __ALL__ */
			       num_terms  NUMBER,    /* default 10 */
			       fmode  VARCHAR2  /*default value NORMAL */ ) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.search.autocomplete.ojvm.AutoComplete.ODCITableStart(
              oracle.sql.STRUCT[],
              java.lang.String,
              java.lang.String,
              java.lang.String,
	            java.math.BigDecimal, java.lang.String) return java.math.BigDecimal',
  
MEMBER FUNCTION ODCITableFetch(self IN OUT autocomplete_term_ot, nrows IN NUMBER, outSet OUT term_info_set) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.search.autocomplete.ojvm.AutoComplete.ODCITableFetch(
              java.math.BigDecimal, 
              oracle.sql.ARRAY[]) return java.math.BigDecimal',

MEMBER FUNCTION ODCITableClose(self IN autocomplete_term_ot) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.lucene.search.autocomplete.ojvm.AutoComplete.ODCITableClose() return java.math.BigDecimal',

STATIC FUNCTION ODCIGetInterfaces (
                     p_interfaces OUT SYS.ODCIObjectList
                     ) RETURN NUMBER,

     STATIC FUNCTION ODCIStatsTableFunction (
                     p_function    IN  SYS.ODCIFuncInfo,
                     p_stats       OUT SYS.ODCITabFuncStats,
                     p_args        IN  SYS.ODCIArgDescList,
                     p_index_name  IN  VARCHAR2,
		                 p_term_name   IN  VARCHAR2,
		                 p_term_value  IN  VARCHAR2,
		                 p_num_terms   IN  NUMBER
                    ) RETURN NUMBER		
);
/
show errors

create function lautocomplete(index_name VARCHAR2,
                              term_name  VARCHAR2 DEFAULT NULL,
                              term_value VARCHAR2 DEFAULT '__ALL__',
                              num_terms  NUMBER DEFAULT 10,
                              fmode VARCHAR2 DEFAULT 'NORMAL' )  RETURN term_info_set
PIPELINED USING autocomplete_term_ot;
/

-- GRANTS
grant execute on lautocomplete to public;
grant execute on autocomplete_term_ot to public;

create public synonym lautocomplete for lucene.lautocomplete;
create public synonym autocomplete_term_ot for lucene.autocomplete_term_ot;

exit
