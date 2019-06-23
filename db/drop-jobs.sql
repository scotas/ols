  rem usage notes:
rem sqlplus sys/change_on_install@orcl @db/drop-searcher-job.sql
begin
  for i in (select JOB_NAME from dba_scheduler_jobs where owner='LUCENE') loop
     begin
       DBMS_SCHEDULER.STOP_JOB('LUCENE.'||i.JOB_NAME,force=>true);
     EXCEPTION WHEN OTHERS THEN
        null;
     end;
  end loop;
  commit;
  -- wait 30 seconds to DBMS SCHEDULER finally shutdown the slave process
  DBMS_LOCK.SLEEP(30);
EXCEPTION WHEN OTHERS THEN
  null;
end;
/

begin
  -- Drop a Cron like process (DBMS_SCHEDULER)
  for i in (select JOB_NAME from dba_scheduler_jobs where owner='LUCENE') loop
     begin
       DBMS_SCHEDULER.DROP_JOB(job_name => 'LUCENE.'||i.JOB_NAME, force => true);
     EXCEPTION WHEN OTHERS THEN
        null;
     end;
  end loop;
EXCEPTION WHEN OTHERS THEN
  null;
end;
/
commit;

exit
