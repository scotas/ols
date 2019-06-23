create table t1 (f1 VARCHAR2(10), f2 VARCHAR2(4000), CONSTRAINT t1_json_chk1 CHECK (f2 IS JSON))
;
insert into t1 values ('1', '{"id":1,"name":"ravi"}');
insert into t1 values ('3', '{"id":3,"name":"murthy"}');

create index it1 on t1(f2) indextype is lucene.SolrIndex 
  parameters('PopulateIndex:false;CommitOnSync:true;SoftCommit:true;LockMasterTable:false;LogLevel:ALL;IncludeMasterColumn:false;ExtraCols:f1 "f1_ti",JSON_VALUE(f2,''$.id'') "id_s",JSON_VALUE(f2,''$.name'') "name_tg"')
;
