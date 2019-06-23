rem usage notes:
rem sqlplus sys/change_on_install@orcl @db/create-sys-triggers.sql
WHENEVER SQLERROR EXIT FAILURE

CREATE OR REPLACE TRIGGER start_lucene_bg_srv 
  AFTER STARTUP ON DATABASE
DECLARE
  v_version VARCHAR2(4000);
BEGIN
  -- Start the slave search process (DBMS_SCHEDULER)
  for i in (select JOB_NAME from dba_scheduler_jobs where owner='LUCENE') loop
     begin
        DBMS_SCHEDULER.ENABLE('LUCENE.'||i.JOB_NAME);
     EXCEPTION WHEN OTHERS THEN
        sys.dbms_system.ksdwrt(3,DBMS_UTILITY.format_error_stack);
     end;
  end loop;

EXCEPTION WHEN OTHERS THEN
  sys.dbms_system.ksdwrt(3,DBMS_UTILITY.format_error_stack);
END; 
/

CREATE OR REPLACE TRIGGER stop_lucene_bg_srv 
  BEFORE SHUTDOWN ON DATABASE
BEGIN
  -- Stop the slave search process (DBMS_SCHEDULER)
  for i in (select JOB_NAME from dba_scheduler_jobs where owner='LUCENE') loop
     begin
        DBMS_SCHEDULER.STOP_JOB('LUCENE.'||i.JOB_NAME,force=>true,commit_semantics=>'ABSORB_ERRORS');
     EXCEPTION WHEN OTHERS THEN
        sys.dbms_system.ksdwrt(3,DBMS_UTILITY.format_error_stack);
     end;
  end loop;
EXCEPTION WHEN OTHERS THEN
  sys.dbms_system.ksdwrt(3,DBMS_UTILITY.format_error_stack);
END; 
/

exit
