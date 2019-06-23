
---------------------------------------------------------------------
--    LUCENE Index Method  Implemented as Trusted Callouts  --
---------------------------------------------------------------------

create or replace
type body LuceneDomainIndexFreqTerms is

  STATIC FUNCTION TextFreqTerms(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set is
  begin
     return TextFreqTerms(Text,Key,null,indexctx,sctx,scanflg);
  end TextFreqTerms;

  STATIC FUNCTION TextFreqTerms(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set is
  begin
     return TextFreqTerms(Text,Key,null,indexctx,sctx,scanflg);
  end TextFreqTerms;

  STATIC FUNCTION TextFreqTerms(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set is
  begin
     return TextFreqTerms('',Key,null,indexctx,sctx,scanflg);
  end TextFreqTerms;

  STATIC FUNCTION TextFreqTerms(Text IN XMLType, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set is
  begin
     return TextFreqTerms(Text,Key,null,indexctx,sctx,scanflg);
  end TextFreqTerms;

  STATIC FUNCTION TextFreqTerms(Text IN XMLType, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY LuceneDomainIndex, scanflg IN NUMBER) RETURN term_info_set is
  begin
     return TextFreqTerms('',Key,null,indexctx,sctx,scanflg);
  end TextFreqTerms;
end;
/
show errors

exit
