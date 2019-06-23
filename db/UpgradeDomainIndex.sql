rem usage notes:
rem sqlplus LUCENE/LUCENE@orcl @db/UpgradeDomainIndex.sql
rem run on the server machine, because it use dbms_java.loadjava
set long 10000 lines 140 pages 50 timing on echo off
set serveroutput on size 1000000 
whenever SQLERROR EXIT FAILURE

select owner,index_name,table_owner,table_name from all_indexes where ITYP_OWNER='LUCENE' AND (ITYP_NAME='LUCENEINDEX' OR ITYP_NAME='SOLRINDEX');

-- drop indextype only if exists and there is no domain indexes associated
declare
  vCount NUMBER;
  TYPE obj_arr IS TABLE OF VARCHAR2(300);
  DRP_STMT VARCHAR2(4000) := 'drop ';
  obj_list obj_arr := obj_arr('public synonym SolrIndex','public synonym LuceneIndex',
                              'indextype SolrIndex', 'indextype LuceneIndex');
begin
  select count(*) into vCount from all_indexes where ITYP_OWNER='LUCENE' AND (ITYP_NAME='LUCENEINDEX' OR ITYP_NAME='SOLRINDEX');
  if (vCount>0) then
    raise_application_error
        (-20101, 'There are Lucene or Solr Domain Index created, drop them first. Aborting installation....');
  else
    select count(*) into vCount from dba_users where username='LUCENE';
    if (vCount>0) then
      FOR I IN OBJ_LIST.FIRST..OBJ_LIST.LAST LOOP
	begin
	  EXECUTE IMMEDIATE DRP_STMT||OBJ_LIST(I)||' force';
	EXCEPTION WHEN OTHERS THEN
	  DBMS_OUTPUT.put_line(DBMS_UTILITY.format_error_stack);
	end;
      end loop;
    end if;
  end if;
end;
/

DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(300);
  DRP_STMT VARCHAR2(4000) := 'drop ';
  obj_list obj_arr := obj_arr('public synonym LContains','public synonym LScore','public synonym LHighlight','public synonym LuceneDomainIndex',
                              'operator LContains','operator LScore','operator LHighlight','type LuceneDomainIndex',
                              'public synonym SContains','public synonym SScore','public synonym SHighlight','public synonym SMlt','public synonym SolrDomainIndex',
                              'operator SContains','operator SScore','operator SHighlight','operator SMlt','type SolrDomainIndex');
BEGIN
  FOR I IN OBJ_LIST.FIRST..OBJ_LIST.LAST LOOP
  begin
    EXECUTE IMMEDIATE DRP_STMT||OBJ_LIST(I)||' force';
  EXCEPTION WHEN OTHERS THEN
    NULL;
  end;
  end loop;
END;
/

@@LuceneDomainIndex.sql
@@LuceneDomainIndex-bdy.plb
@@CreateDomainIndexType.sql
@@SolrDomainIndex.sql
@@SolrDomainIndex-bdy.plb
@@SolrDomainIndexType.sql

alter type LuceneDomainIndexFreqTerms compile body;
alter type LuceneDomainIndexSimilarity compile body;
alter package LuceneDomainAdm compile body;

exit
