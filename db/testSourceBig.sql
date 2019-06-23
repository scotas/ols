set long 10000 lines 140 pages 50 timing on echo on
set serveroutput on size 1000000 

begin
  DBMS_JAVA.SET_OUTPUT(10000000);
  SolrDomainIndex.setLogLevel('ALL');
end;
/


-- drop table test_source_big;
-- Demo indexing a big table (594k rows)
create table test_source_big as (
select owner,name,type,line,text from (select rownum as ntop_pos,q.* from
(select * from all_source) q)
where ntop_pos>0 and ntop_pos<=5000
);
-- MergeFactor,MaxBufferedDocs and MaxMergeDocs reduce IO over the BLOB storage
-- but you can get a java.lang.OutOfMemoryError with bigger values.
-- drop index source_big_lidx force;
create index source_big_lidx on test_source_big(text) 
INDEXTYPE IS LUCENE.LUCENEINDEX 
parameters('SyncMode:Deferred;LogLevel:ALL;AutoTuneMemory:true;PerFieldAnalyzer:line(org.apache.lucene.analysis.core.KeywordAnalyzer),type(org.apache.lucene.analysis.core.KeywordAnalyzer),TEXT(org.apache.lucene.analysis.core.StopAnalyzer);FormatCols:line(00000);ExtraCols:line "line", type "type";LobStorageParameters:STORAGE (BUFFER_POOL KEEP) CACHE READS');

CREATE INDEX SOURCE_BIG_SIDX ON TEST_SOURCE_BIG(TEXT) 
INDEXTYPE IS LUCENE.SOLRINDEX 
filter by type,line
order by line desc
parameters('DefaultColumn:text;BatchCount:5000;CommitOnSync:false;LockMasterTable:false;IncludeMasterColumn:false;Updater:0;Searcher:0;SyncMode:OnLine;LogLevel:WARNING;ExtraCols:substr(text,1,256) "text",line "line_tin", type "type_sn",substr(text,1,256) "title";HighlightColumn:title;MltColumn:title;LobStorageParameters:STORAGE (BUFFER_POOL KEEP) CACHE READS FILESYSTEM_LIKE_LOGGING');

/*
create index source_small_lidx on test_source_small(text)
indextype is lucene.LuceneIndex
filter by type,line
order by line desc,type asc
parameters('SyncMode:Deferred;LogLevel:ALL;PerFieldAnalyzer:line(org.apache.lucene.analysis.core.KeywordAnalyzer),name(org.apache.lucene.analysis.core.KeywordAnalyzer),type(org.apache.lucene.analysis.core.KeywordAnalyzer),text(org.apache.lucene.analysis.core.StopAnalyzer);ExtraCols:line "line",name "name",type "type",text "text";FormatCols:line(00000);AutoTuneMemory:true;DefaultColumn:text;IncludeMasterColumn:false;LobStorageParameters:STORAGE (BUFFER_POOL KEEP) CACHE READS'); 

  -- modifying LOB storage parameters
   alter table SCOTT.SOURCE_BIG_SIDX$t modify lob(data) (STORAGE (BUFFER_POOL KEEP) CACHE READS FILESYSTEM_LIKE_LOGGING);

  -- adding resources for Solr
  insert into source_big_sidx$T (
  select path(1),extractValue(res,'/Resource/ModificationDate'),extractValue(res,'/Resource/ContentSize'),extractValue(res,'/Resource/XMLLob'),'N' from resource_view
      where under_path(res,'/public/solr/LUCENE.SOURCE_SMALL_SIDX/conf/',1)>0);

  
  -- updating
  
  update source_big_sidx$T set 
     data=xdburitype('/public/SCOTT/SOURCE_BIG_SIDX/conf/schema.xml').getBlob(),
     file_size=dbms_lob.getLength(xdburitype('/public/SCOTT/SOURCE_BIG_SIDX/conf/schema.xml').getBlob()),
     last_modified=sysdate
     where name='schema.xml';

*/

-- SQL> create index source_big_lidx on test_source_big(text)
--   2  indextype is lucene.LuceneIndex
--   3  parameters('AutoTuneMemory:true;PerFieldAnalyzer:line(org.apache.lucene.analysis.core.KeywordAnalyzer),type(org.apache.lucene.analysis.core.KeywordAnalyzer),TEXT(org.apache.lucene.analysis.core.SimpleAnalyzer);LogLevel:WARNING;FormatCols:line(0000);ExtraCols:line "line", type "type";MergeFactor:500;LobStorageParameters:CACHE READS FILESYSTEM_LIKE_LOGGING');
-- Index created.
-- Elapsed: 00:01:27.76
-- 11g composite index syntax
create index source_big_idx on test_source_big(text)
indextype is ctxsys.context
filter by type,line
order by line desc
parameters('SYNC (MANUAL) TRANSACTIONAL');
-- Index created.
-- Elapsed: 00:01:11.72

-- Rebuild example
-- alter index source_big_lidx rebuild
--       parameters('AutoTuneMemory:true;MergeFactor:500');

-- Rebuild online example
-- alter index source_big_lidx
--       parameters('AutoTuneMemory:true;MergeFactor:500;SyncMode:OnLine');
-- call LuceneDomainIndex.enqueueChange(USER||'.SOURCE_BIG_LIDX','xx','rebuild');
-- commit;
-- You can still using LuceneDomainIndex during rebuild process!!!!
-- Except for sync or optimize procedure which requires exclusive lock
-- at SOURCE_BIG_LIDX$T table


create index source_big_idx on test_source_big(text) 
indextype is ctxsys.context; 
-- SQL> create index source_big_idx on test_source_big(text)
--   2  indextype is ctxsys.context;
-- Index created.
-- Elapsed: 00:01:22.93

insert into test_source_big
select owner,name,type,line,text from (select rownum as ntop_pos,q.* from
(select * from all_source) q)
where ntop_pos>5000 and ntop_pos<=10000;
begin
   SolrDomainIndex.sync('SOURCE_BIG_SIDX');
   commit;
end;
/

-- Must return 9 rows
select count(TEXT) from (select rownum as ntop_pos,q.* from
(select text from test_source_big where scontains(text,'function')>0) q)
where ntop_pos>=0 and ntop_pos<10;

select sc,TEXT from (select rownum as ntop_pos,q.* from
(select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc,text from test_source_big where scontains(text,'function',1)>0 order by sscore(1) desc) q)
where ntop_pos>=0 and ntop_pos<10;

select sc,TEXT from (select rownum as ntop_pos,q.* from
(select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc,text from test_source_big where scontains(text,'function',1)>0 order by sscore(1) asc) q)
where ntop_pos>=0 and ntop_pos<10;

-- Must return 9 rows
select sscore(1),shighlight(1) from test_source_big where scontains(text,'"procedure java"~10',1)>0 order by sscore(1) desc;
select sscore(1),shighlight(1) from test_source_big where scontains(text,'"procedure java"~10',1)>0 order by sscore(1) asc;
-- Must return 22 rows
select /*+ DOMAIN_INDEX_SORT */ sscore(1) from test_source_big where scontains(text,'(logLevel OR prefix) AND "LANGUAGE JAVA"',1)>0;
select /*+ DOMAIN_INDEX_SORT */ sscore(1) from test_source_big where scontains(text,'(logLevel OR prefix) AND "LANGUAGE JAVA"',1)>0 order by sscore(1) asc;

select /*+ DOMAIN_INDEX_SORT */ sscore(1),shighlight(1) from test_source_big where scontains(text,'"procedure java"~10',1)>0 order by sscore(1) desc;
select /*+ DOMAIN_INDEX_SORT */ sscore(1),shighlight(1) from test_source_big where scontains(text,'"procedure java"~10',1)>0 order by sscore(1) asc;
select /*+ DOMAIN_INDEX_SORT */ sscore(1) from test_source_big where scontains(text,'(logLevel OR prefix) AND "LANGUAGE JAVA"',1)>0;
select /*+ DOMAIN_INDEX_SORT */ sscore(1) from test_source_big where scontains(text,'(logLevel OR prefix) AND "LANGUAGE JAVA"',1)>0 order by sscore(1) asc;

select text,smlt(1) from test_source_big where scontains(text,'rownum:[1 TO 1] AND "procedure java"~10',1)>0;
select text from test_source_big where rowid in (
    select * from table(
       select smlt(1) from test_source_big where scontains(text,'rownum:[1 TO 1] AND "procedure java"~10',1)>0));

insert into test_source_big
select owner,name,type,line,text from (select rownum as ntop_pos,q.* from
(select * from all_source) q)
where ntop_pos>10000 and ntop_pos<=20000;
begin
   SolrDomainIndex.sync('SOURCE_BIG_SIDX');
   commit;
end;
/
select count(TEXT) from (select rownum as ntop_pos,q.* from
(select text from test_source_big where scontains(text,'function')>0) q)
where ntop_pos>=0 and ntop_pos<10;

insert into test_source_big
select owner,name,type,line,text from (select rownum as ntop_pos,q.* from
(select * from all_source) q)
where ntop_pos>20000 and ntop_pos<=40000;
begin
   SolrDomainIndex.sync('SOURCE_BIG_SIDX');
   commit;
end;
/

begin
   SolrDomainIndex.optimize('SOURCE_BIG_SIDX');
   commit;
end;
/


select count(TEXT) from (select rownum as ntop_pos,q.* from
(select text from test_source_big where scontains(text,'function')>0) q)
where ntop_pos>=0 and ntop_pos<10;

-- Test execution time: 00:00:05.82
select sc,TEXT from (select rownum as ntop_pos,q.* from
(select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc, text 
from test_source_big where scontains(text,'function',1)>0) q)
where ntop_pos>=1990 and ntop_pos<2000;

select sc,TEXT from (select rownum as ntop_pos,q.* from
(select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc, text 
from test_source_big where scontains(text,'function',1)>0 order by sscore(1) asc) q)
where ntop_pos>=100 and ntop_pos<110;

-- inline pagination version using scontains "rownum:[n TO m] AND" option
select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc, shighlight(1) 
from test_source_big where scontains(text,'rownum:[100 TO 109] AND function',1)>0 order by sscore(1) asc;

-- Test execution time: 00:00:00.38
-- Question: Why this query is faster than the above???, they have same execution plan :(
-- Answer: Context switching from SQL to Java for geetting score of 2000 rows is too high
-- Workaround: use inline pagination syntax, see example above
select TEXT from (select rownum as ntop_pos,q.* from
(select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc,text 
from test_source_big where scontains(text,'function',1)>0) q)
where ntop_pos>=100 and ntop_pos<110;

select TEXT from (select rownum as ntop_pos,q.* from
(select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc,text 
from test_source_big where scontains(text,'function',1)>0 order by sscore(1) asc) q)
where ntop_pos>=100 and ntop_pos<110;

select count(line) from test_source_big
  where scontains(text,'varchar2')>0 and line>=2600;
  
select count(line) from test_source_big
  where scontains(text,'varchar2 AND line_tin:[2600 TO *]')>0;
-- Query equivalent using 11g+ Composite index filter by functionality
select /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(test_source_big source_big_sidx) */ count(*) from test_source_big
  where scontains(text,'varchar2')>0 and line between 2600 and 9000;

-- Query equivalent using CountHits
select SolrDomainIndex.countHits('SOURCE_BIG_SIDX','varchar2 AND line_tin:[2600 TO *]') from dual;

-- Query equivalent using 11g+ Composite index filter by functionality
-- select /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(test_source_big source_big_idx) */ count(*) from test_source_big
-- where contains(text,'varchar2')>0 and line between 2600 and 9000;

select sc,TEXT from (select rownum as ntop_pos,q.* from
(select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc, text 
from test_source_big where scontains(text,'type',1)>0 order by sscore(1) asc) q)
where ntop_pos>=2000 and ntop_pos<=2010;

-- inline pagination version using lcontains "rownum:[n TO m] AND" option
select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc, text 
from test_source_big where scontains(text,'rownum:[2000 TO 2010] AND type',1)>0 order by sscore(1) asc;

-- countHits example
declare 
  hits NUMBER;
  fromRow NUMBER;
  toRow NUMBER;
  sc    NUMBER;
  text  VARCHAR2(4000);
  CURSOR c1 (fromRow NUMBER, toRow NUMBER) IS select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc,text
             from test_source_big where scontains(text,'rownum:['||fromRow||' TO '||toRow||'] AND function OR procedure OR package',1)>0 
             order by sscore(1) ASC;
begin
  hits := SolrDomainIndex.countHits('SOURCE_BIG_SIDX','function OR procedure OR package');
  fromRow := round(hits*0.75);
  toRow := fromRow+10;
  dbms_output.put_line('Count Hits: '||hits);
  dbms_output.put_line('Score list from rownum: '||fromRow||' to: '||toRow);
  for j in 1..10 loop
    OPEN c1(fromRow+(j*10),toRow+((j+1)*10)); -- open the cursor before fetching
    LOOP
       -- Fetches 2 columns into variables
       FETCH c1 INTO sc, text;
       EXIT WHEN c1%NOTFOUND;
       null;
    END LOOP;
    CLOSE c1;
  end loop;
end;
/

-- Oracle Context example to compare performance
declare 
  hits NUMBER;
  fromRow NUMBER;
  toRow NUMBER;
  sc    NUMBER;
  text  VARCHAR2(4000);
  CURSOR c1 (fromRow NUMBER, toRow NUMBER) IS select sc,TEXT from (select rownum as ntop_pos,q.* from
               (select /*+ DOMAIN_INDEX_SORT */ score(1) sc, text 
                from test_source_big where contains(text,'function OR procedure OR package',1)>0 order by score(1) asc) q)
                where ntop_pos>=fromRow and ntop_pos<=toRow;
begin
  hits := ctx_query.count_hits(index_name => 'source_big_idx', text_query => 'function OR procedure OR package', exact => TRUE);
  fromRow := round(hits*0.75);
  toRow := fromRow+10;
  dbms_output.put_line('Count Hits: '||hits);
  dbms_output.put_line('Score list from rownum: '||fromRow||' to: '||toRow);
  for j in 1..10 loop
    OPEN c1(fromRow+(j*10),toRow+((j+1)*10)); -- open the cursor before fetching
    LOOP
       -- Fetches 2 columns into variables
       FETCH c1 INTO sc, text;
       EXIT WHEN c1%NOTFOUND;
       null;
    END LOOP;
    CLOSE c1;
  end loop;
end;
/

-- traditional count(*) example, oops I got ORA-03113: end-of-file on communication channel on 11g
-- Apply patch: 6445561 - ORA-00600 [26599] [62] DUE TO INCORRECT PERSISTENCE OF BY INVOKER PIN
declare 
  hits NUMBER;
  fromRow NUMBER;
  toRow NUMBER;
  sc    NUMBER;
  text  VARCHAR2(4000);
  CURSOR c1 (fromRow NUMBER, toRow NUMBER) IS select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc,text
             from test_source_big where scontains(text,'rownum:['||fromRow||' TO '||toRow||'] AND function OR procedure OR package',1)>0 
             order by sscore(1) ASC;
begin
  select count(text) into hits 
     from test_source_big where scontains(text,'function OR procedure OR package',1)>0 
     order by sscore(1) ASC;
  fromRow := round(hits*0.75);
  toRow := fromRow+10;
  dbms_output.put_line('Count Hits: '||hits);
  dbms_output.put_line('Score list from rownum: '||fromRow||' to: '||toRow);
  for j in 1..10 loop
    OPEN c1(fromRow+(j*10),toRow+((j+1)*10)); -- open the cursor before fetching
    LOOP
       -- Fetches 2 columns into variables
       FETCH c1 INTO sc, text;
       EXIT WHEN c1%NOTFOUND;
       null;
    END LOOP;
    CLOSE c1;
  end loop;
end;
/

/*
composite domain index comparison
-- 11g composite index syntax
-- SQL> create index source_big_idx on test_source_big(text)
--   2  indextype is ctxsys.context
--   3  filter by line
--   4  order by line;
-- Index created.
-- Elapsed: 00:00:53.82

*/
select text,line from
(select rownum as ntop_pos,q.* from
  (select /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(test_source_big source_big_idx) */ text, line from test_source_big
   where contains(text,'function OR procedure OR package',1)>0 and line between 0 and 3000 order by line DESC) q)
 where ntop_pos>=1 and ntop_pos<=10;

select /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(test_source_big source_big_sidx) */ text,line from test_source_big
where scontains(text,'rownum:[1 TO 10] AND line_tin:[0000 TO 3000] AND (function OR procedure OR package)','line_tin desc',1)>0;
-- equivalent query using inline order by
select /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(test_source_big source_big_sidx) */ text,line from test_source_big
where scontains(text,'rownum:[1 TO 10] AND line_tin:[0000 TO 3000] AND (function OR procedure OR package)',1)>0 order by line desc;
-- equivalent query using inline filter by and order by
select /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(test_source_big source_big_sidx) */ text,line from test_source_big
where scontains(text,'rownum:[1 TO 10] AND (function OR procedure OR package)',1)>0 and line between 0 and 3000 order by line desc;

set define off
SELECT FIELD,SJOIN(T.FACETS) F FROM TABLE(SFACETS(USER||'.SOURCE_BIG_SIDX','*:*','facet.field=type_sn&facet.method=enum')) T;

select text from test_source_big where rowid in
(SELECT column_value from table(SELECT /*+ FIRST_ROWS */ smlt(1) FROM test_source_big where scontains(text,'rownum:[1 TO 1] AND procedure',1)>0 and rownum=1)); 


create table source_categories (
 cat_code    number(4),
 cat_name    varchar2(256),
 cat_parent  number(4),
 CONSTRAINT PK_SOURCE_CATEGORIES PRIMARY KEY (cat_code),
 CONSTRAINT FK_CAT_PARENT FOREIGN KEY (cat_parent)
	  REFERENCES source_categories (cat_code)
);
/

begin
  insert into source_categories values (1,'TEXT:procedure',null);
  insert into source_categories values (2,'TEXT:function',null);
  insert into source_categories values (3,'TEXT:trigger',null);
  insert into source_categories values (4,'TEXT:package',null);
  insert into source_categories values (5,'TEXT:(object type)',null);

  insert into source_categories values (6,'TEXT:java',1);
  insert into source_categories values (7,'TEXT:(pl sql)',1);
  insert into source_categories values (8,'TEXT:wrapped',1);

  insert into source_categories values (9,'TEXT:java',2);
  insert into source_categories values (10,'TEXT:(pl sql)',2);
  insert into source_categories values (11,'TEXT:wrapped',2);

  insert into source_categories values (12,'TEXT:java',3);
  insert into source_categories values (13,'TEXT:(pl sql)',3);
  insert into source_categories values (14,'TEXT:wrapped',3);

  insert into source_categories values (15,'TEXT:java',4);
  insert into source_categories values (16,'TEXT:(pl sql)',4);
  insert into source_categories values (17,'TEXT:wrapped',4);

  insert into source_categories values (18,'TEXT:java',5);
  insert into source_categories values (19,'TEXT:(pl sql)',5);
  insert into source_categories values (20,'TEXT:wrapped',5);

  insert into source_categories values (21,'line:[0001 TO 1000]',1);
  insert into source_categories values (22,'line:[1001 TO 2000]',1);
  insert into source_categories values (23,'line:[2001 TO 3000]',1);
  insert into source_categories values (24,'line:[3001 TO 4000]',1);
  insert into source_categories values (25,'line:[4001 TO 5000]',1);

  insert into source_categories values (26,'line:[0001 TO 1000]',2);
  insert into source_categories values (27,'line:[1001 TO 2000]',2);
  insert into source_categories values (28,'line:[2001 TO 3000]',2);
  insert into source_categories values (29,'line:[3001 TO 4000]',2);
  insert into source_categories values (30,'line:[4001 TO 5000]',2);

  insert into source_categories values (31,'line:[0001 TO 1000]',3);
  insert into source_categories values (32,'line:[1001 TO 2000]',3);
  insert into source_categories values (33,'line:[2001 TO 3000]',3);
  insert into source_categories values (34,'line:[3001 TO 4000]',3);
  insert into source_categories values (35,'line:[4001 TO 5000]',3);

  insert into source_categories values (36,'line:[0001 TO 1000]',4);
  insert into source_categories values (37,'line:[1001 TO 2000]',4);
  insert into source_categories values (38,'line:[2001 TO 3000]',4);
  insert into source_categories values (39,'line:[3001 TO 4000]',4);
  insert into source_categories values (40,'line:[4001 TO 5000]',4);

  insert into source_categories values (41,'line:[0001 TO 1000]',5);
  insert into source_categories values (42,'line:[1001 TO 2000]',5);
  insert into source_categories values (43,'line:[2001 TO 3000]',5);
  insert into source_categories values (44,'line:[3001 TO 4000]',5);
  insert into source_categories values (45,'line:[4001 TO 5000]',5);
end;

SELECT cat_code, cat_name, cat_parent, level
   FROM source_categories
   start with cat_parent is null
   CONNECT BY PRIOR cat_code = cat_parent;

select ljoin(lfacets('SOURCE_BIG_LIDX,'||
   case level when 1 then cat_name
   ELSE  PRIOR cat_name||','|| cat_name
   END
   )), cat_code,level
   FROM source_categories
   start with cat_parent is null
   CONNECT BY PRIOR cat_code = cat_parent
   group by cat_code,level;

-- requires create materialized view grant
CREATE MATERIALIZED VIEW source_facets
AS
select ljoin(lfacets('SOURCE_BIG_LIDX,'||
   case level when 1 then cat_name
   ELSE  PRIOR cat_name||','|| cat_name
   END
   )), cat_code,level
   FROM source_categories
   start with cat_parent is null
   CONNECT BY PRIOR cat_code = cat_parent
   group by cat_code,level;

-- Examples high_freq_terms pipeline table function
select * from table(high_freq_terms('SOURCE_BIG_LIDX','TEXT',10));

select * from table(high_freq_terms('SOURCE_BIG_LIDX',null,10));

select * from table(high_freq_terms('SOURCE_BIG_LIDX','line',10));

-- Examples index_terms pipeline table function
select * from table(index_terms('SOURCE_BIG_LIDX','TEXT')) order by docFreq desc;

select * from table(index_terms('SOURCE_BIG_LIDX','TEXT'));

select * from table(index_terms('SOURCE_BIG_LIDX',null)) where rownum<10;

select * from table(index_terms('SOURCE_BIG_LIDX','line'));

select * from (select * from table(index_terms('SOURCE_BIG_LIDX',null)) order by docfreq desc) where rownum<=10;

-- verify that all rows where indexed, this number should be equal to:
-- select count(*) from test_source_big;
select count(*) from table(index_terms('SOURCE_BIG_LIDX','rowid')) where substr(term,1,length('rowid'))='rowid';
begin 
  LuceneDomainIndex.optimize('SOURCE_BIG_LIDX');
  commit;
end;
alter index SOURCE_BIG_LIDX rebuild parameters('LogLevel:INFO;IndexOnRam:false;ParallelDegree:2;BatchCount:3000;MergeFactor:500');
alter index SOURCE_BIG_LIDX rebuild parameters('LogLevel:INFO;IndexOnRam:true;ParallelDegree:2;BatchCount:3000;MergeFactor:500');

-- export/import example
create table SOURCE_BIG_SIDX$T$BK as (select * from SOURCE_BIG_SIDX$T);
/*
mochoa@pocho:~/tmp$ exp scott/tiger@orcl
Export: Release 11.2.0.3.0 - Production on Mon Jan 21 15:11:15 2013
Copyright (c) 1982, 2011, Oracle and/or its affiliates.  All rights reserved.

Connected to: Oracle Database 11g Enterprise Edition Release 11.2.0.3.0 - Production
With the Partitioning, OLAP, Data Mining and Real Application Testing options
Enter array fetch buffer size: 4096 > 
Export file: expdat.dmp > SOURCE_BIG_SIDX_BK.dmp
(2)U(sers), or (3)T(ables): (2)U > 3
Export table data (yes/no): yes > 
Compress extents (yes/no): yes > 
Export done in US7ASCII character set and AL16UTF16 NCHAR character set
server uses AL32UTF8 character set (possible charset conversion)
About to export specified tables via Conventional Path ...
Table(T) or Partition(T:P) to be exported: (RETURN to quit) > SOURCE_BIG_SIDX$T$BK
. . exporting table           SOURCE_BIG_SIDX$T$BK        194 rows exported
Table(T) or Partition(T:P) to be exported: (RETURN to quit) > 
Export terminated successfully without warnings.

*/

select count(*) from test_source_big where scontains(text,'function')>0;
-- 13136 rows
drop index SOURCE_BIG_SIDX;

CREATE INDEX SOURCE_BIG_SIDX ON TEST_SOURCE_BIG(TEXT) 
INDEXTYPE IS LUCENE.SOLRINDEX 
parameters('PopulateIndex:false;BatchCount:5000;DefaultColumn:text_tn;CommitOnSync:false;LockMasterTable:false;IncludeMasterColumn:false;Updater:0;Searcher:0;SyncMode:OnLine;LogLevel:WARNING;ExtraCols:text "text_tn",line "line_tin", type "type_sn",substr(text,1,256) "title";HighlightColumn:title;MltColumn:title;LobStorageParameters:STORAGE (BUFFER_POOL KEEP) CACHE READS FILESYSTEM_LIKE_LOGGING');

drop table SOURCE_BIG_SIDX$T$BK;
/*
mochoa@pocho:~/tmp$ imp scott/tiger@orcl
Import: Release 11.2.0.3.0 - Production on Mon Jan 21 15:21:25 2013
Copyright (c) 1982, 2011, Oracle and/or its affiliates.  All rights reserved.

Connected to: Oracle Database 11g Enterprise Edition Release 11.2.0.3.0 - Production
With the Partitioning, OLAP, Data Mining and Real Application Testing options
Import data only (yes/no): no > 
Import file: expdat.dmp > SOURCE_BIG_SIDX_BK.dmp
Enter insert buffer size (minimum is 8192) 30720> 
Export file created by EXPORT:V11.02.00 via conventional path
import done in US7ASCII character set and AL16UTF16 NCHAR character set
import server uses AL32UTF8 character set (possible charset conversion)
List contents of import file only (yes/no): no > 
Ignore create error due to object existence (yes/no): no > yes
Import grants (yes/no): yes > no
Import table data (yes/no): yes > 
Import entire export file (yes/no): no > yes
. importing SCOTT's objects into SCOTT
. importing SCOTT's objects into SCOTT
. . importing table         "SOURCE_BIG_SIDX$T$BK"        194 rows imported
Import terminated successfully without warnings.
*/

select count(*) from test_source_big where scontains(text,'function')>0;
-- 0 rows

truncate table SOURCE_BIG_SIDX$T;

insert into SOURCE_BIG_SIDX$T (select * from SOURCE_BIG_SIDX$T$BK);

commit;

alter index SOURCE_BIG_SIDX parameters('LogLevel:WARNING');

select count(*) from test_source_big where scontains(text,'function')>0;
-- 13136 rows


