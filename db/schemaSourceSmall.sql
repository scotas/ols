create table test_source_small as 
(select NAME,TYPE,LINE,TEXT from user_source)
;


create index source_small_lidx on test_source_small(text)
indextype is lucene.LuceneIndex
parameters('SyncMode:Deferred;LogLevel:INFO;FormatCols:line(0000);ExtraCols:line "line",name "name";Analyzer:org.apache.lucene.analysis.core.StopAnalyzer;MergeFactor:500')
;
