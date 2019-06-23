create table t1 (
tvnot varchar2(255),
termvector varchar2(255),
tvoffset varchar2(255),
tvposition varchar2(255),
tvpositionoffset varchar2(255))
;

insert into t1 (tvnot,termvector,tvoffset,tvposition,tvpositionoffset) values (
'one two two three three three',
'one two two three three three',
'one two two three three three',
'one two two three three three',
'one two two three three three'
)
;

insert into t1 (tvnot,termvector,tvoffset,tvposition,tvpositionoffset) values (
'one two two three three three',
'one two two three three three',
'one two two three three three',
'one two two three three three',
'one two two three three three'
)
;

insert into t1 (tvnot,termvector,tvoffset,tvposition,tvpositionoffset) values (
'one two two three three three',
'one two two three three three',
'one two two three three three',
'one two two three three three',
'one two two three three three'
)
;

insert into t1 (tvnot,termvector,tvoffset,tvposition,tvpositionoffset) values (
'two two three three three four four four four',
'two two three three three four four four four',
'two two three three three four four four four',
'two two three three three four four four four',
'two two three three three four four four four'
)
;

create index it1 on t1(termvector) indextype is lucene.LuceneIndex
parameters('LogLevel:ALL;ExtraCols:TVNOT,TVOFFSET,TVPOSITION,TVPOSITIONOFFSET;FormatCols:TVNOT(ANALYZED),TERMVECTOR(ANALYZED_WITH_VECTORS),TVOFFSET(ANALYZED_WITH_OFFSETS),TVPOSITION(ANALYZED_WITH_POSITIONS),TVPOSITIONOFFSET(ANALYZED_WITH_POSITIONS_OFFSETS)')
;
