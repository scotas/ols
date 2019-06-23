---------------------------------------------------------------------
--    Solr facets implementation                                   --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('NAME_VALUE','NAME_VALUE_ARR','FACET_TERM_OT','FACET_INFO','FACET_INFO_SET','SJOIN','SFACETS');
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
  DRP_STMT VARCHAR2(4000) := 'drop function ';
  obj_list obj_arr := obj_arr('SJOIN','SFACETS');
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

CREATE OR REPLACE TYPE NAME_VALUE AS OBJECT (
  name     VARCHAR2(4000),
  value    NUMBER(10)
);
/

CREATE OR REPLACE
TYPE NAME_VALUE_ARR AS VARRAY(32767) OF NAME_VALUE;
/

CREATE OR REPLACE TYPE FACET_INFO AS OBJECT (
  FIELD      VARCHAR2(4000),
  FACETS     NAME_VALUE_ARR,
  QUERIES    NAME_VALUE_ARR,
  DATES      NAME_VALUE_ARR,
  RANGES     NAME_VALUE_ARR
);
/
show errors

CREATE OR REPLACE TYPE FACET_INFO_SET AS TABLE OF FACET_INFO;
/
SHOW ERRORS

CREATE OR REPLACE TYPE FACET_TERM_OT AUTHID CURRENT_USER AS OBJECT
(
key INTEGER,
STATIC FUNCTION ODCITableStart(sctx OUT FACET_TERM_OT, 
                               INDEX_NAME VARCHAR2,
                               Q       VARCHAR2, /* default *:* */
                               F       VARCHAR2 /* any facet Faceting Parameters encoded using URL sintax including the prefix "facet." */) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.solr.handler.component.ojvm.FacetComponent.ODCITableStart(
              oracle.sql.STRUCT[],
              java.lang.String,
              java.lang.String,
              java.lang.String) return java.math.BigDecimal',
  
MEMBER FUNCTION ODCITableFetch(self IN OUT FACET_TERM_OT, nrows IN NUMBER, outSet OUT FACET_INFO_SET) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.solr.handler.component.ojvm.FacetComponent.ODCITableFetch(
              java.math.BigDecimal, 
              oracle.sql.ARRAY[]) return java.math.BigDecimal',

MEMBER FUNCTION ODCITableClose(self IN FACET_TERM_OT) RETURN NUMBER
  AS LANGUAGE JAVA
  NAME 'com.scotas.solr.handler.component.ojvm.FacetComponent.ODCITableClose() return java.math.BigDecimal',

STATIC FUNCTION ODCIGetInterfaces (
                     p_interfaces OUT SYS.ODCIObjectList
                     ) RETURN NUMBER,

     STATIC FUNCTION ODCIStatsTableFunction (
                     P_FUNCTION    IN  SYS.ODCIFUNCINFO,
                     P_STATS       OUT SYS.ODCITABFUNCSTATS,
                     P_ARGS        IN  SYS.ODCIARGDESCLIST,
                     P_INDEX_NAME  IN  VARCHAR2,
		                 P_Q           IN  VARCHAR2,
		                 P_F           IN  VARCHAR2
                    ) RETURN NUMBER		
);
/
show errors

CREATE OR REPLACE FUNCTION SFACETS(INDEX_NAME VARCHAR2,
                        Q       VARCHAR2 DEFAULT '*:*',
                        F       VARCHAR2 DEFAULT NULL /* any facet.* Value Parameters encoded using URL sintax including the prefix "facet." */)  
                        RETURN facet_info_set
PIPELINED USING FACET_TERM_OT;
/

CREATE OR REPLACE FUNCTION SJOIN(I_TBL  IN NAME_VALUE_ARR,
                                 I_GLUE IN VARCHAR2 := ',')
  RETURN VARCHAR2
  IS
    V_STR VARCHAR2(32767);
  BEGIN
    IF I_TBL IS NOT NULL THEN
      FOR I IN 1 .. I_TBL.COUNT LOOP
        v_str := v_str || i_glue || i_tbl(i).name || '(' || i_tbl(i).value || ')';
      END LOOP;
    END IF;
    RETURN SUBSTR(V_STR,LENGTH(I_GLUE)+1);
  END;
/
show errors

-- GRANTS
GRANT EXECUTE ON SFACETS TO PUBLIC;
GRANT EXECUTE ON FACET_TERM_OT TO PUBLIC;
GRANT EXECUTE ON NAME_VALUE TO PUBLIC;
GRANT EXECUTE ON NAME_VALUE_ARR TO PUBLIC;
GRANT EXECUTE ON FACET_INFO TO PUBLIC;
GRANT EXECUTE ON FACET_INFO_SET TO PUBLIC;
GRANT EXECUTE ON SJOIN TO PUBLIC;

CREATE PUBLIC SYNONYM SFACETS FOR LUCENE.SFACETS;
CREATE PUBLIC SYNONYM FACET_TERM_OT FOR LUCENE.FACET_TERM_OT;
CREATE PUBLIC SYNONYM NAME_VALUE FOR LUCENE.NAME_VALUE;
CREATE PUBLIC SYNONYM NAME_VALUE_ARR FOR LUCENE.NAME_VALUE_ARR;
CREATE PUBLIC SYNONYM FACET_INFO FOR LUCENE.FACET_INFO;
CREATE PUBLIC SYNONYM FACET_INFO_SET FOR LUCENE.FACET_INFO_SET;
CREATE PUBLIC SYNONYM SJOIN FOR LUCENE.SJOIN;

exit
