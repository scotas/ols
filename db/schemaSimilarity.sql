create table t1 (f1 number primary key, f2 varchar2(2000), f3 number(5,3))
;
insert into t1 values (1, 'Cefaleias', 1);
insert into t1 values (2, 'Cefaleia', 1);
insert into t1 values (3, 'Cefaleia em salva', 0.625);
insert into t1 values (4, 'Cefaleias de tensão', 0.625);
insert into t1 values (5, 'Cefaleias / enxaquecas', 0.625);
insert into t1 values (6, 'Desproporção céfalo-pélvica', 0.5);
insert into t1 values (7, 'Deformidade por redução cefálica congénita', 15.87);
insert into t1 values (8, 'Intoxicação por antibióticos do grupo das cefalosporinas', 0.5);

create index it1 on t1(f2) indextype is lucene.luceneindex 
  parameters('LogLevel:ALL;Analyzer:org.apache.lucene.analysis.pt.PortugueseAnalyzer;FormatCols:F3(00.000);ExtraCols:F3;RewriteScore:true;SimilarityMethod:org.apache.lucene.search.WildcardSimilarity')
;
