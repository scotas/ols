
---------------------------------------------------------------------
--    LUCENE auto complete implementation - body                   --
---------------------------------------------------------------------

CREATE TYPE BODY autocomplete_term_ot AS 

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
		                 p_term_name   IN  VARCHAR2,
		                 p_term_value  IN  VARCHAR2,
		                 p_num_terms   IN  NUMBER
                     ) RETURN NUMBER IS
     BEGIN
        p_stats := SYS.ODCITabFuncStats(p_num_terms);
        RETURN ODCIConst.success;
     END ODCIStatsTableFunction;
END;
/
show errors

ASSOCIATE STATISTICS WITH FUNCTIONS lautocomplete USING autocomplete_term_ot;
show errors

exit
