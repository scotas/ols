create table t1 (f1 VARCHAR2(10), f2 XMLType)
;
insert into t1 values ('1', XMLType('<emp id="1"><name>ravi</name></emp>'));
insert into t1 values ('3', XMLType('<emp id="3"><name>murthy</name></emp>'));

create index it1 on t1(f2) indextype is lucene.SolrIndex 
  parameters('PopulateIndex:false;CommitOnSync:true;SoftCommit:true;LockMasterTable:false;LogLevel:ALL;IncludeMasterColumn:false;ExtraCols:F1 "f1_ti",extractValue(F2,''/emp/name/text()'') "name_tg",extractValue(F2,''/emp/@id'') "id_s"')
;
