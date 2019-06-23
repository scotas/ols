
---------------------------------------------------------------------
--    LUCENE Facet implementation  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('facets_agg_type','lfacets');
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

-- wrapper package of Java methods
create or replace
PACKAGE HitCounter authid current_user AS
  FUNCTION ODCIInitialize(
                  rJctx OUT NOCOPY NUMBER,
                  idx VARCHAR2,
                  qry VARCHAR2) RETURN NUMBER
                  AS LANGUAGE JAVA
    NAME 'com.scotas.lucene.search.facets.ojvm.HitCounter.ODCIInitialize(
              java.math.BigDecimal[],
              java.lang.String,
              java.lang.String) return java.math.BigDecimal';
              
  FUNCTION ODCIIterate(
                  rJctx IN NUMBER,
                  qry VARCHAR2,
                  cnt OUT NOCOPY NUMBER) RETURN NUMBER
                  AS LANGUAGE JAVA
    NAME 'com.scotas.lucene.search.facets.ojvm.HitCounter.ODCIIterate(
              java.math.BigDecimal,
              java.lang.String,
              java.math.BigDecimal[]) return java.math.BigDecimal';
              
  FUNCTION ODCITerminate(
                  rJctx IN NUMBER) RETURN NUMBER
                  AS LANGUAGE JAVA
    NAME 'com.scotas.lucene.search.facets.ojvm.HitCounter.ODCITerminate(
              java.math.BigDecimal) return java.math.BigDecimal';
              
END HitCounter;
/
show errors

-- ODCI API aggregate function implementation
-- adapted code from the example:
-- http://unclechrisblog.blogspot.com/2008/06/aggregate-function-for-collection-types.html
create or replace
type facets_agg_type authid current_user as object (
  elements agg_tbl,
  owner VARCHAR2(32),
  index_name VARCHAR2(128),
  jctx NUMBER,
  static function
    ODCIAggregateInitialize(sctx IN OUT NOCOPY facets_agg_type )
    return number,
  member function
    ODCIAggregateIterate(self IN OUT NOCOPY facets_agg_type ,
                         value IN varchar2 )
    return number,
  member function
    ODCIAggregateTerminate(self IN facets_agg_type,
                           returnValue OUT  NOCOPY agg_tbl,
                           flags IN number)
    return number,
  member function
    ODCIAggregateMerge(self IN OUT NOCOPY facets_agg_type,
                       ctx2 IN facets_agg_type)
    return number
);
/
show errors

create or replace function lfacets(input varchar2)
return agg_tbl
parallel_enable aggregate using facets_agg_type;
/
show errors

-- index example
-- create index emp_lidx on emp(ename) indextype is lucene.LuceneIndex 
-- parameters('AutoTuneMemory:true;Analyzer:org.apache.lucene.analysis.standard.StandardAnalyzer;ExtraTabs:dept d;WhereCondition:emp.deptno=d.deptno;ExtraCols:d.dname "dname"');

-- query example 
-- select ljoin(lfacets('PAGES_LIDX_ALL,dname:('||d.dname||'),ename:('||ename||')')) from emp e, dept d where e.deptno=d.deptno group by(e.deptno)

-- GRANTS
grant execute on facets_agg_type to public;
grant execute on lfacets to public;

create public synonym facets_agg_type for lucene.facets_agg_type;
create public synonym lfacets for lucene.lfacets;

exit