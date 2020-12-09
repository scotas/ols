SET LONG 10000
SET TIMING ON
SET ECHO ON
set serveroutput on size 1000000 

begin
  DBMS_JAVA.SET_OUTPUT(10000000);
  SolrDomainIndex.setLogLevel('INFO');
end;
/

create table test_source_big_par (
    owner VARCHAR2(128),
    name  VARCHAR2(128),
    type  VARCHAR2(12),
    line  NUMBER,
    text  VARCHAR2(4000)
) PARTITION BY RANGE (LINE) (
    PARTITION LINE_125 VALUES LESS THAN (125),
    PARTITION LINE_250 VALUES LESS THAN (250),
    PARTITION LINE_500 VALUES LESS THAN (500),
    PARTITION LINE_1000 VALUES LESS THAN (1000),
    PARTITION LINE_2000 VALUES LESS THAN (2000),
    PARTITION LINE_3000 VALUES LESS THAN (3000),
    PARTITION LINE_OTHERS VALUES LESS THAN (MAXVALUE)
);

insert into TEST_SOURCE_BIG_PAR
select owner,name,type,line,text from (select rownum as ntop_pos,q.* from
(select * from all_source) q)
where ntop_pos>0 and ntop_pos<=5000;

CREATE INDEX SOURCE_BIG_SIDX_P ON test_source_big_par(TEXT) 
INDEXTYPE IS LUCENE.SOLRINDEX 
filter by type,line
order by line desc
parameters('PopulateIndex:true;DefaultColumn:text;BatchCount:5000;CommitOnSync:false;LockMasterTable:false;IncludeMasterColumn:false;Updater:0;Searcher:0;SyncMode:OnLine;LogLevel:INFO;ExtraCols:substr(text,1,256) "text",line "line_tin", type "type_sn",substr(text,1,256) "title";HighlightColumn:title;MltColumn:title;LobStorageParameters:STORAGE (BUFFER_POOL KEEP) CACHE READS FILESYSTEM_LIKE_LOGGING') LOCAL;


begin
   SolrDomainIndex.optimize('SOURCE_BIG_SIDX_P');
   commit;
end;

alter index SOURCE_BIG_SIDX_P rebuild partition LINE_125;

select count(line) from test_source_big_par
  where scontains(text,'varchar2 AND line_tin:[3000 TO *]')>0 and line>=3000;

select sc,ln,sh from (select rownum as ntop_pos,q.* from
(select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc,line ln, shighlight(1) sh
from test_source_big_par where scontains(text,'rownum:[1 TO 10] AND function',1)>0 order by sscore(1) desc) q)
where ntop_pos>=1 and ntop_pos<11;

select /*+ DOMAIN_INDEX_SORT */ sscore(1) sc,line, shighlight(1) 
from TEST_SOURCE_BIG_PAR where scontains(text,'rownum:[1 TO 20] AND function',1)>0 order by sscore(1) desc;

select count(*) from TEST_SOURCE_BIG_PAR;
select SOLRDOMAININDEX.countHits('SOURCE_BIG_SIDX_P','*:*') from DUAL;

SELECT table_name, partition_name, high_value, num_rows
FROM   user_tab_partitions
WHERE  table_name = 'TEST_SOURCE_BIG_PAR'
ORDER BY 1,2;

ALTER TABLE TEST_SOURCE_BIG_PAR SPLIT PARTITION LINE_2000 AT (1500)
INTO (PARTITION LINE_1500,
      PARTITION LINE_2000);
alter index SOURCE_BIG_SIDX_P rebuild partition LINE_1500;
alter index SOURCE_BIG_SIDX_P rebuild partition LINE_2000;

insert into TEST_SOURCE_BIG_PAR
select owner,name,type,line,text from (select rownum as ntop_pos,q.* from
(select * from all_source) q)
where ntop_pos>5000 and ntop_pos<=10000;

ALTER TABLE TEST_SOURCE_BIG_PAR MERGE PARTITIONS LINE_1500, LINE_2000
INTO PARTITION LINE_2000;
alter index SOURCE_BIG_SIDX_P rebuild partition LINE_2000;

insert into TEST_SOURCE_BIG_PAR
select owner,name,type,line,text from (select rownum as ntop_pos,q.* from
(select * from all_source) q)
where ntop_pos>10000 and ntop_pos<=20000;

drop INDEX SOURCE_BIG_SIDX_P force;
drop table TEST_SOURCE_BIG_PAR;
