---------------------------------------------------------------------
--    Solr facets implementation - body                            --
---------------------------------------------------------------------

CREATE OR REPLACE TYPE BODY FACET_TERM_OT AS 

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
                     P_FUNCTION    IN  SYS.ODCIFUNCINFO,
                     P_STATS       OUT SYS.ODCITABFUNCSTATS,
                     P_ARGS        IN  SYS.ODCIARGDESCLIST,
                     P_INDEX_NAME  IN  VARCHAR2,
		                 P_Q           IN  VARCHAR2,
		                 P_F           IN  VARCHAR2) RETURN NUMBER IS
     BEGIN
        p_stats := SYS.ODCITabFuncStats(nvl(length(p_f),1));
        RETURN ODCIConst.success;
     END ODCIStatsTableFunction;
END;
/
show errors

ASSOCIATE STATISTICS WITH FUNCTIONS sfacets USING FACET_TERM_OT;
show errors

EXIT
