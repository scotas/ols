
---------------------------------------------------------------------
--    LUCENE Index Method  common types                            --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('ridlist_table_stats_ot','ridlist_table','rowid_array','lucene_error_typ',
                              'lucene_msg_typ','rt_fetch_attributes','RowInfo',
                              'agg_attributes','agg_tbl','ljoin');
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

create or replace type lucene_msg_typ as object (
  ridlist     sys.ODCIRidList,
  operation   VARCHAR2(32)
);
/
show errors

create or replace type lucene_error_typ as object (
  failed_op   lucene_msg_typ,
  prefix      VARCHAR2(32)
);
/
show errors

create or replace TYPE lucene_msg_typ_array as VARRAY(32767) of lucene_msg_typ;
/
show errors

CREATE OR REPLACE TYPE rowid_array AS TABLE OF VARCHAR2(5072);
/
show errors

CREATE OR REPLACE FUNCTION ridlist_table(ridlist sys.odciridlist) RETURN rowid_array PIPELINED AS
BEGIN
  FOR i IN 1 .. ridlist.last LOOP
    PIPE ROW(ridlist(i));
  END LOOP;
  RETURN;
END ridlist_table;
/
show errors

-- Code example extracted from http://www.oracle-developer.net/display.php?id=427
-- by Adrian Billington, June 2009
CREATE OR REPLACE TYPE ridlist_table_stats_ot AUTHID CURRENT_USER AS OBJECT (
  dummy_attribute NUMBER,
  STATIC FUNCTION ODCIGetInterfaces (p_interfaces OUT SYS.ODCIObjectList) RETURN NUMBER,
  STATIC FUNCTION ODCIStatsTableFunction (
        p_function IN  SYS.ODCIFuncInfo,
        p_stats    OUT SYS.ODCITabFuncStats,
        p_args     IN  SYS.ODCIArgDescList,
        ridlist    IN sys.odciridlist
  ) RETURN NUMBER
);
/
show errors

-- pipeline table function required types
create or replace
TYPE rt_fetch_attributes AS OBJECT
      ( typecode     NUMBER
      , v2_column    VARCHAR2(32767)
      , num_column   NUMBER
      , date_column  DATE
      , clob_column  CLOB
      , raw_column   RAW(32767)
      , raw_error    NUMBER
      , raw_length   INTEGER
      , ids_column   INTERVAL DAY TO SECOND
      , iym_column   INTERVAL YEAR TO MONTH
      , ts_column    TIMESTAMP
      , tstz_column  TIMESTAMP WITH TIME ZONE
      , tsltz_column TIMESTAMP WITH LOCAL TIME ZONE
      , cvl_offset   INTEGER
      , cvl_length   INTEGER
      );
/
show errors

create or replace
TYPE RowInfo
  AS VARRAY(32) OF rt_fetch_attributes;
/
show errors

-- aggregate function required types
create or replace
TYPE agg_attributes AS OBJECT
      ( qryText      VARCHAR2(4000)
      , hits         NUMBER
      );
/
show errors

create or replace
TYPE agg_tbl
  AS TABLE OF agg_attributes;
/
show errors

CREATE OR REPLACE function ljoin(i_tbl  in agg_tbl,
                                i_glue in varchar2 := ',')
  return varchar2
  is
    v_str varchar2(32767);
  begin
    IF i_tbl is not null THEN
      FOR i in 1 .. i_tbl.count LOOP
        v_str := v_str || i_glue || i_tbl(i).qryText || '(' || i_tbl(i).hits || ')';
      END LOOP;
    END IF;
    return substr(v_str,length(i_glue)+1);
  end;
/
show errors

begin
    DBMS_AQADM.CREATE_QUEUE_TABLE(queue_table        => 'ERRORS$QT',
                                  queue_payload_type => 'LUCENE.lucene_error_typ',
                                  sort_list          => 'ENQ_TIME',
                                  message_grouping   => DBMS_AQADM.NONE,
                                  compatible         => '10.2',
                                  multiple_consumers => FALSE);
    DBMS_AQADM.CREATE_QUEUE(queue_name         => 'ERRORS$Q',
                            queue_table        => 'ERRORS$QT',
                            queue_type         => DBMS_AQADM.NORMAL_QUEUE,
                            comment            => 'Lucene Domain Index error queue');
    DBMS_AQADM.START_QUEUE(queue_name          => 'ERRORS$Q');
exception when others then
   null;
end;
/
show errors

-- GRANTS
grant execute on lucene_msg_typ to public;
grant execute on lucene_error_typ to public;
grant execute on rowid_array to public;
grant execute on ridlist_table to public;
grant execute on ridlist_table_stats_ot to public;
-- pipeline table function required types
grant execute on rt_fetch_attributes to public;
grant execute on RowInfo to public;

-- aggregate function required types
grant execute on agg_attributes to public;
grant execute on agg_tbl to public;
grant execute on ljoin to public;

-- public synonym
create public synonym lucene_msg_typ for lucene.lucene_msg_typ;
create public synonym lucene_error_typ for lucene.lucene_error_typ;
create public synonym rowid_array for lucene.rowid_array;
create public synonym ridlist_table for lucene.ridlist_table;
create public synonym ridlist_table_stats_ot for lucene.ridlist_table_stats_ot;
-- pipeline table function required types
create public synonym rt_fetch_attributes for lucene.rt_fetch_attributes;
create public synonym RowInfo for lucene.RowInfo;
-- aggregate function required types
create public synonym agg_attributes for lucene.agg_attributes;
create public synonym agg_tbl for lucene.agg_tbl;
create public synonym ljoin for lucene.ljoin;

exit
