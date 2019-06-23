
---------------------------------------------------------------------
--    LUCENE Index Method  common types bdy                        --
---------------------------------------------------------------------

CREATE OR REPLACE TYPE BODY ridlist_table_stats_ot AS
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
                     p_function IN  SYS.ODCIFuncInfo,
                     p_stats    OUT SYS.ODCITabFuncStats,
                     p_args     IN  SYS.ODCIArgDescList,
                     ridlist    IN sys.odciridlist
                     ) RETURN NUMBER IS
     BEGIN
        p_stats := SYS.ODCITabFuncStats(ridlist.last);
        RETURN ODCIConst.success;
     END ODCIStatsTableFunction;
END;
/
show errors

ASSOCIATE STATISTICS WITH FUNCTIONS ridlist_table USING ridlist_table_stats_ot;

exit
