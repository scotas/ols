
---------------------------------------------------------------------
--    Solr Index Method  Implemented as Trusted Callouts  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('SolrIndex');
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

-- drop indextype SolrIndex;

-- CREATE INDEXTYPE
create indextype SolrIndex
for
scontains(varchar2, varchar2),
scontains(varchar2, varchar2, varchar2),
scontains(CLOB, varchar2),
scontains(CLOB, varchar2, varchar2),
scontains(sys.XMLType, varchar2),
scontains(sys.XMLType, varchar2, varchar2)
using SolrDomainIndex without column data
with array dml
with order by sscore(number)
with rebuild online
with local range partition
with composite index;

show errors

-- GRANTS
grant execute on  SolrIndex to public;

create public synonym SolrIndex for lucene.SolrIndex;

declare
  result boolean;
begin
    -- XDB directory for exporting Indexes
    if (not LuceneDomainIndex.ResourceExists('/public/solr')) then
        result := dbms_xdb.createFolder('/public/solr');
    end if;
end;
/
commit;
