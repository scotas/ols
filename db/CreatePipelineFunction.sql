
---------------------------------------------------------------------
--    LUCENE PHighligh implementation  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('RHighlight','PHighlight','dla_ot','cla_ot','dla_pkg');
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

create or replace
TYPE dla_ot authid current_user as object
(
  atype ANYTYPE --<-- transient record type
, STATIC FUNCTION ODCITableDescribe(
                  rtype OUT NOCOPY ANYTYPE,
                  index_name VARCHAR2,
                  qry VARCHAR2,
                  cols VARCHAR2,
                  stmt  IN  VARCHAR2) RETURN NUMBER

, STATIC FUNCTION ODCITablePrepare(
                  sctx    OUT NOCOPY dla_ot,
                  tf_info IN  sys.ODCITabFuncInfo,
                  index_name VARCHAR2,
                  qry VARCHAR2,
                  cols VARCHAR2,
                  stmt  IN  VARCHAR2) RETURN NUMBER

, STATIC FUNCTION ODCITableStart(
                  sctx IN OUT NOCOPY dla_ot,
                  index_name VARCHAR2,
                  qry VARCHAR2,
                  cols VARCHAR2,
                  stmt  IN  VARCHAR2) RETURN NUMBER

, MEMBER FUNCTION ODCITableFetch(
                  SELF  IN OUT NOCOPY dla_ot,
                  nrows IN     NUMBER,
                  rws   OUT    NOCOPY anydataset
                  ) RETURN NUMBER

, MEMBER FUNCTION ODCITableClose(
                  SELF IN dla_ot
                  ) RETURN NUMBER
);
/

create or replace
TYPE cla_ot authid current_user as object
(
  atype ANYTYPE --<-- transient record type
, STATIC FUNCTION ODCITableDescribe(
                  rtype OUT NOCOPY ANYTYPE,
                  index_name VARCHAR2,
                  qry VARCHAR2,
                  cols VARCHAR2,
                  returnType IN  VARCHAR2,
                  rws    IN  SYS_REFCURSOR) RETURN NUMBER

, STATIC FUNCTION ODCITablePrepare(
                  sctx    OUT NOCOPY cla_ot,
                  tf_info IN  sys.ODCITabFuncInfo,
                  index_name VARCHAR2,
                  qry VARCHAR2,
                  cols VARCHAR2,
                  returnType IN  VARCHAR2,
                  rws    IN  SYS_REFCURSOR) RETURN NUMBER

, STATIC FUNCTION ODCITableStart(
                  sctx IN OUT NOCOPY cla_ot,
                  index_name VARCHAR2,
                  qry VARCHAR2,
                  cols VARCHAR2,
                  returnType IN  VARCHAR2,
                  rws    IN  SYS_REFCURSOR) RETURN NUMBER

, MEMBER FUNCTION ODCITableFetch(
                  SELF  IN OUT NOCOPY cla_ot,
                  nrows IN     NUMBER,
                  rws   OUT    NOCOPY anydataset
                  ) RETURN NUMBER

, MEMBER FUNCTION ODCITableClose(
                  SELF IN cla_ot
                  ) RETURN NUMBER
);
/
show errors

create or replace
PACKAGE dla_pkg authid current_user AS

   /*
   || ---------------------------------------------------------------------------------
   ||
   || Name:        dla_pkg
   ||
   ||              ------------------------------------------------------
   ||              Code modified from the example:
   ||              http://www.oracle-developer.net/display.php?id=422
   ||              (c) Adrian Billington, www.oracle-developer.net.
   ||
   || ---------------------------------------------------------------------------------
   */

   /*
   || Record types for use across multiple DLA_OT methods.
   */
   TYPE rt_dynamic_sql IS RECORD
   ( cursor      INTEGER
   , column_cnt  PLS_INTEGER
   , description DBMS_SQL.DESC_TAB2
   , execute     INTEGER
   );

   TYPE rt_anytype_metadata IS RECORD
   ( precision PLS_INTEGER
   , scale     PLS_INTEGER
   , length    PLS_INTEGER
   , csid      PLS_INTEGER
   , csfrm     PLS_INTEGER
   , schema    VARCHAR2(30)
   , type      ANYTYPE
   , name      VARCHAR2(30)
   , version   VARCHAR2(30)
   , attr_cnt  PLS_INTEGER
   , attr_type ANYTYPE
   , attr_name VARCHAR2(128)
   , typecode  PLS_INTEGER
   );

   /*
   || State variable for use across multiple DLA_OT methods.
   */
   r_sql rt_dynamic_sql;
   /*
   || State variable for use across multiple DLA_OT methods.
   */
   ctx NUMBER;
   
  FUNCTION ODCITableStart(ctx OUT NOCOPY NUMBER,  
                          index_name VARCHAR2,
                          qry VARCHAR2,
                          cols VARCHAR2,
                          stmt VARCHAR2)
    RETURN NUMBER
    AS LANGUAGE JAVA
    NAME 'com.scotas.lucene.search.highlight.ojvm.Highlighter.ODCITableStart(
              java.math.BigDecimal[],
              java.lang.String,
              java.lang.String,
              java.lang.String,
              java.lang.String) return java.math.BigDecimal';

  FUNCTION ODCITableStart(ctx OUT NOCOPY NUMBER,  
                          index_name VARCHAR2,
                          qry VARCHAR2,
                          cols VARCHAR2,
                          cur SYS_REFCURSOR)
    RETURN NUMBER
    AS LANGUAGE JAVA
    NAME 'com.scotas.lucene.search.highlight.ojvm.Highlighter.ODCITableStart(
              java.math.BigDecimal[],
              java.lang.String,
              java.lang.String,
              java.lang.String,
              java.sql.ResultSet) return java.math.BigDecimal';

  FUNCTION ODCITableFetch(ctx IN NUMBER,
                          nrows IN     NUMBER,
                          outSet OUT NOCOPY RowInfo
                  ) RETURN NUMBER
    AS LANGUAGE JAVA
    NAME 'com.scotas.lucene.search.highlight.ojvm.Highlighter.ODCITableFetch(
              java.math.BigDecimal,
              java.math.BigDecimal,
              java.sql.Array[]) return java.math.BigDecimal';

  FUNCTION ODCITableClose(ctx IN NUMBER) RETURN NUMBER
    AS LANGUAGE JAVA
    NAME 'com.scotas.lucene.search.highlight.ojvm.Highlighter.ODCITableClose(
              java.math.BigDecimal) return java.math.BigDecimal';
   
END dla_pkg;
/
show errors

CREATE OR REPLACE FUNCTION Phighlight(
            index_name VARCHAR2,
            qry VARCHAR2,
            cols VARCHAR2,
            stmt  IN  VARCHAR2) RETURN ANYDATASET PIPELINED USING dla_ot;
/
show errors

CREATE OR REPLACE FUNCTION Rhighlight(
            index_name VARCHAR2,
            qry VARCHAR2,
            cols VARCHAR2,
            rType  IN  VARCHAR2,
            rws    IN  SYS_REFCURSOR) RETURN ANYDATASET PIPELINED PARALLEL_ENABLE(PARTITION rws BY ANY) USING cla_ot;
/
show errors

-- GRANTS
grant execute on PHighlight to public;
grant execute on Rhighlight to public;
grant execute on dla_ot to public;
grant execute on cla_ot to public;
grant execute on dla_pkg to public;

create public synonym PHighlight for lucene.PHighlight;
create public synonym Rhighlight for lucene.Rhighlight;
create public synonym dla_ot for lucene.dla_ot;
create public synonym cla_ot for lucene.cla_ot;
create public synonym dla_pkg for lucene.dla_pkg;

exit