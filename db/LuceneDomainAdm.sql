---------------------------------------------------------------------
--    LUCENE Domain Index Admin Methods                            --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('LuceneDomainAdm');
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

create or replace package LuceneDomainAdm authid definer as
  procedure createQueue(prefix VARCHAR2);
  procedure dropQueue(prefix VARCHAR2);
  procedure purgueQueue(prefix VARCHAR2);
end;
/
show errors

-- GRANTS
grant execute on LuceneDomainAdm to public;

create public synonym LuceneDomainAdm for lucene.LuceneDomainAdm;

exit
