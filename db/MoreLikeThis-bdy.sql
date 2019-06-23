---------------------------------------------------------------------
--    LUCENE More Like This Method                                 --
---------------------------------------------------------------------
CREATE OR REPLACE PACKAGE BODY MoreLike AS
    -- FUNCTION f1 returns a collection of elements (1,2,3,... x)
    FUNCTION this(index_name IN VARCHAR2,
                  x IN ROWID,
                  f IN NUMBER DEFAULT 1,
                  t IN NUMBER DEFAULT 10,
                  minTermFreq IN NUMBER DEFAULT 2,
                  MINDOCFREQ IN NUMBER DEFAULT 5) RETURN SYS.ODCIRIDLIST IS
      index_schema VARCHAR2(30);
      idx_name VARCHAR2(30) := index_name;
    BEGIN
       SELECT owner INTO index_schema FROM all_indexes where index_name=idx_name;
       return this(index_schema,index_name,x,f,t,mintermfreq,mindocfreq);
       exception when no_data_found then
         raise_application_error
           (-20101, 'Index not found: '||idx_name);
       when too_many_rows then
         return this(SYS_CONTEXT('USERENV','CURRENT_SCHEMA'),index_name,x,f,t,mintermfreq,mindocfreq);
    END this;
    FUNCTION this(owner IN VARCHAR2,
                  index_name IN VARCHAR2,
                  x IN ROWID,
                  f IN NUMBER DEFAULT 1,
                  t IN NUMBER DEFAULT 10,
                  minTermFreq IN NUMBER DEFAULT 2,
                  minDocFreq IN NUMBER DEFAULT 5) RETURN sys.odciridlist IS
    prefix        VARCHAR2(255) := owner || '.' || index_name;
    BEGIN
       RETURN getSimilar(prefix,x,f,t,minTermFreq,minDocFreq);
    END this;
END MoreLike;
/
show errors

exit
 
