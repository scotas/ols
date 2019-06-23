
---------------------------------------------------------------------
--    LUCENE Index Method  Implemented as Trusted Callouts  --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('LuceneIndex');
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

-- CREATE INDEXTYPE
create indextype LuceneIndex
for
lcontains(varchar2, varchar2),
lcontains(varchar2, varchar2, varchar2),
lcontains(CLOB, varchar2),
lcontains(CLOB, varchar2, varchar2),
lcontains(sys.XMLType, varchar2),
lcontains(sys.XMLType, varchar2, varchar2)
using LuceneDomainIndex
without column data
with order by lscore(number)
with rebuild online
with local range partition
with array dml
with composite index;

show errors

-- GRANTS
grant execute on  LuceneIndex to public;

create public synonym LuceneIndex for lucene.LuceneIndex;

declare
  result boolean;
begin
    -- XDB directory for exporting Indexes
    if (not LuceneDomainIndex.ResourceExists('/public/lucene')) then
        result := dbms_xdb.createFolder('/public/lucene');
    end if;
end;
/
commit;

