rem output the content of an explain plan command
set long 10000 lines 140 pages 50 timing on echo on
set serveroutput on size 1000000 

select plan_table_output from table(dbms_xplan.display('plan_table',null,'serial'));
-- as SYS to see adaptive excecution plans
-- select plan_table_output from table(dbms_xplan.display_cursor(format=>'allstats +adaptive'));

-- COLUMN SQL_TEXT FORMAT A60
-- COLUMN OP FORMAT 99
-- COLUMN EXEC FORMAT 9999
-- SELECT a.SQL_TEXT SQL_TEXT,a.USERS_OPENING OP,a.EXECUTIONS EXEC FROM sys.v_$sqlarea a,sys.all_users b 
-- WHERE a.parsing_user_id = b.user_id and b.username = 'LUCENE' and a.USERS_OPENING>0
-- order by a.executions desc;
