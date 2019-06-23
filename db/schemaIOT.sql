create table t1 (f1 number primary key, f2 varchar2(200), f3 number(4,2)) ORGANIZATION INDEX
;
insert into t1 values (1, 'ravi', 3.46);
insert into t1 values (3, 'murthy', 15.87);

CREATE INDEX IT1 ON T1(F2) INDEXTYPE IS LUCENE.LUCENEINDEX 
  parameters('LogLevel:ALL;Analyzer:org.apache.lucene.analysis.en.EnglishAnalyzer;FormatCols:F3(00.00);ExtraCols:F3')
;
