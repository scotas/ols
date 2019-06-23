rem usage notes:
rem sqlplus sys/change_on_install@orcl @db/disable-searcher.sql
WHENEVER SQLERROR EXIT FAILURE

BEGIN
  -- Stop the slave search process (DBMS_SCHEDULER)
  for i in (select JOB_NAME from dba_scheduler_jobs where owner='LUCENE') loop
     begin
        DBMS_SCHEDULER.STOP_JOB('LUCENE.'||i.JOB_NAME,force=>true);
     EXCEPTION WHEN OTHERS THEN
        null;
     end;
  end loop;
  execute immediate 'truncate table LUCENE.BG_PROCESS';
exception when others then 
  null;
END; 
/

commit;

