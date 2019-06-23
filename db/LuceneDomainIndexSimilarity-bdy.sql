
---------------------------------------------------------------------
--    LUCENE Index Method  Implemented as Trusted Callouts  --
---------------------------------------------------------------------

create or replace
type body LuceneDomainIndexSimilarity is

  STATIC FUNCTION TextSimilarity(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextSimilarity(Text,Key,null,indexctx,sctx,scanflg);
  end TextSimilarity;

  STATIC FUNCTION TextSimilarity(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextSimilarity(Text,Key,null,indexctx,sctx,scanflg);
  end TextSimilarity;

  STATIC FUNCTION TextSimilarity(Text IN XMLType, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextSimilarity(Text,Key,null,indexctx,sctx,scanflg);
  end TextSimilarity;

end;
/
show errors

exit
