create table t1 (
f1 number,
f2 varchar2(200),
f3 varchar2(200),
F4 number unique)
;

insert into t1 values (1, 'ravi','user data1',101);
insert into t1 values (3, 'murthy','user data3',103);

create table t2 (
f5 number,
f6 varchar2(200), 
f7 date,
CONSTRAINT t2_t1_fk FOREIGN KEY (f5)
      references T1(F4) on delete cascade)
;

insert into t2 values (101, 'extra data1',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN')-1);
insert into t2 values (101, 'extra data1-2',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN')-2);
insert into t2 values (103, 'extra data3',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN')-3);

-- sample usage of user data stores
create index it1 on t1(f2) indextype is lucene.SolrIndex 
  parameters('CommitOnSync:false;LogLevel:INFO;IncludeMasterColumn:false;LockMasterTable:false;PopulateIndex:false')
;

alter index it1 rebuild
  parameters('ExtraCols:L$MT.f2 "f2_tgn",L$MT.f3 "f3_tgn",L$MT.f4 "f4_in",cursor(select c.f6 "f6_tgn" from SCOTT.t2 c where c.f5=L$MT.f4) "t2"')
;

-- Create here this trigger to enqueue changes on parent table
CREATE OR REPLACE TRIGGER LT$IT1
AFTER UPDATE OF f6,f7 ON t2
FOR EACH ROW
DECLARE
  ridlist sys.ODCIRidList;
BEGIN
    SELECT ROWID 
      BULK COLLECT INTO ridlist
      FROM T1 WHERE F4=:NEW.f5;
    SolrDomainIndex.enqueueChange(USER||'.IT1',ridlist,'update');
end
;

-- Force to re-index all
alter index it1 rebuild
  parameters('LogLevel:ALL')
;
