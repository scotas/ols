---------------------------------------------------------------------
--    LUCENE More Like This Method                                 --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('MoreLike');
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
 
CREATE OR REPLACE PACKAGE MoreLike AS
  FUNCTION this(index_name IN VARCHAR2,
                x IN ROWID,
                f IN NUMBER DEFAULT 1,
                t IN NUMBER DEFAULT 10,
                minTermFreq IN NUMBER DEFAULT 2,
                minDocFreq IN NUMBER DEFAULT 5) RETURN sys.odciridlist;
  FUNCTION this(owner IN VARCHAR2,
                index_name IN VARCHAR2,
                x IN ROWID,
                f IN NUMBER DEFAULT 1,
                t IN NUMBER DEFAULT 10,
                minTermFreq IN NUMBER DEFAULT 2,
                minDocFreq IN NUMBER DEFAULT 5) RETURN sys.odciridlist;
  FUNCTION getSimilar(prefix VARCHAR2, x ROWID, f NUMBER, t NUMBER, minTermFreq NUMBER, minDocFreq NUMBER) RETURN sys.odciridlist AS LANGUAGE JAVA NAME
	'com.scotas.lucene.search.similar.ojvm.MoreLike.getSimilar(java.lang.String, java.lang.String,
                java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal) return oracle.ODCI.ODCIRidList';
END MoreLike;
/
show errors

-- GRANTS
grant execute on MoreLike to public;

create public synonym MoreLike for lucene.MoreLike;

exit
