---------------------------------------------------------------------
--    Solr Flashback Domain Index Admin Methods                    --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('SolrFlashbackAdm','PeriodTable_arr');
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

CREATE OR REPLACE
TYPE PeriodTable_arr AS VARRAY(1024) OF TIMESTAMP;
/

create or replace package SolrFlashbackAdm authid definer as
   procedure DefinePeriod(httpBasePort IN NUMBER, jmxBasePort IN NUMBER, periodTable IN PeriodTable_arr);
   procedure Clean;
end;
/
show errors

-- GRANTS
grant execute on PeriodTable_arr to public;
grant execute on SolrFlashbackAdm to public;

create public synonym PeriodTable_arr for lucene.PeriodTable_arr;
create public synonym SolrFlashbackAdm for lucene.SolrFlashbackAdm;

exit
