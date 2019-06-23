rem usage notes:
rem sqlplus lucene/lucene@orcl @db/enable-searcher.sql
WHENEVER SQLERROR EXIT FAILURE

DECLARE
  v_version VARCHAR2(4000);
BEGIN
  execute immediate 'truncate table LUCENE.BG_PROCESS';
  commit;
  -- Start the slave search process (DBMS_SCHEDULER)
  for i in (select JOB_NAME from user_scheduler_jobs) loop
     begin
        DBMS_SCHEDULER.ENABLE('LUCENE.'||i.JOB_NAME);
     EXCEPTION WHEN OTHERS THEN
        null;
     end;
  end loop;

EXCEPTION WHEN OTHERS THEN
  null;
END; 
/

commit;

select JOB_NAME,ENABLED,AUTO_DROP,RESTARTABLE,STATE,RUN_COUNT,FAILURE_COUNT from user_scheduler_jobs;

