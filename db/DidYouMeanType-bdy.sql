
---------------------------------------------------------------------
--    LUCENE Did You Mean Suggestion List implementation - body    --
---------------------------------------------------------------------

CREATE TYPE BODY didyoumean_term_ot AS

      STATIC FUNCTION ODCITableStart(sctx OUT didyoumean_term_ot, 
                               index_name varchar2,
                               cmpval  varchar2,
		       	       numSug	NUMBER,		
                               highlight varchar2, 
			       distancealg  varchar2) RETURN NUMBER IS
		 index_schema VARCHAR2(30);
	         idx_name VARCHAR2(30) := index_name;
   	BEGIN
                SELECT owner INTO index_schema FROM all_indexes where index_name=idx_name;
		RETURN didyoumean_term_ot.ODCITableStart(sctx,index_schema,index_name,cmpval,numSug,highlight,distancealg);
		EXCEPTION when no_data_found THEN
	      		RAISE_APPLICATION_ERROR
		        (-20101, 'Index not found: '||idx_name);
	        when too_many_rows THEN
	        RETURN didyoumean_term_ot.ODCITableStart(sctx,SYS_CONTEXT('USERENV','CURRENT_SCHEMA'),index_name,cmpval,numSug,highlight,distancealg);
	END;


     STATIC FUNCTION ODCIGetInterfaces (
                     p_interfaces OUT SYS.ODCIObjectList
                     ) RETURN NUMBER IS
     BEGIN
        p_interfaces := SYS.ODCIObjectList(
                           SYS.ODCIObject ('SYS', 'ODCISTATS2')
                           );
        RETURN ODCIConst.success;
     END ODCIGetInterfaces;

     STATIC FUNCTION ODCIStatsTableFunction (
	                 p_function    IN  SYS.ODCIFuncInfo,
        	         p_stats       OUT SYS.ODCITabFuncStats,
                	 p_args        IN  SYS.ODCIArgDescList,
			 p_index_name  IN  VARCHAR2,
		         p_cmpval   IN  VARCHAR2,
		         p_numSug  IN  NUMBER,
		         p_highlight   IN  VARCHAR2,
			 p_distancealg  IN  VARCHAR2
                     ) RETURN NUMBER IS
     BEGIN
        p_stats := SYS.ODCITabFuncStats(p_numSug);
        RETURN ODCIConst.success;
     END ODCIStatsTableFunction;
END;
/
show errors

ASSOCIATE STATISTICS WITH FUNCTIONS ldidyoumean USING didyoumean_term_ot;
show errors

exit

