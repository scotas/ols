create table t2 (
  f4 varchar2(3) primary key,
  f5 VARCHAR2(200))
;

insert into t2 values (101,'user data1');
insert into t2 values (102,'user data2');

create table t1 (
  f1 number, 
  f2 CLOB, 
  f3 varchar2(3),
  CONSTRAINT t1_t2_fk FOREIGN KEY (f3)
      REFERENCES t2(f4) ON DELETE cascade)
;
      
insert into t1 values ('1', 'ravi',101);
insert into t1 values ('3', 'murthy',102);

create index it1 on t1(f3) indextype is lucene.LuceneIndex 
  parameters('Analyzer:org.apache.lucene.analysis.core.SimpleAnalyzer;ExtraCols:f2 "f2"')
;

alter index it1 rebuild
  parameters('ExtraCols:f2 "f2",t2.f5 "f5";ExtraTabs:t2;WhereCondition:L$MT.f3=t2.f4')
;

CREATE OR REPLACE TRIGGER LT$IT1
AFTER INSERT OR UPDATE OF f5 ON t2
FOR EACH ROW
DECLARE
  ridlist sys.ODCIRidList;
BEGIN
    SELECT ROWID 
      BULK COLLECT INTO ridlist
      FROM T1 WHERE F3=:NEW.f4;
    LuceneDomainIndex.enqueueChange(USER||'.IT1',ridlist,'update');
END
;
