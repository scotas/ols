
rem sqlplus lucene/lucene@orcl @db/create-searcher-job.sql LUCENE.IndexScanServJob LUCENE.IndexScanServ 1099 WARNING 0 100

define job_name=&1;
define job_procedure=&2;
define port=&3;
define logLevel=&4;
define jmxPort=&5;
define timeOut=&6;

---------------------------------------------------------------------
--    Package to do Parallel Operation on Lucene Domain Index      --
---------------------------------------------------------------------

begin
  -- Start a Cron like process (DBMS_SCHEDULER)
  DBMS_SCHEDULER.CREATE_JOB(
   job_name          =>  '&job_name',
   job_type          =>  'PLSQL_BLOCK',
   job_action        =>  '
begin
   &job_procedure(''&port'',''&logLevel'',''&jmxPort'',''&timeOut'');
   exception when others then
      sys.dbms_system.ksdwrt(3,DBMS_UTILITY.format_error_stack);
end;',
   start_date        =>  SYSDATE,
   enabled           => false,
   auto_drop         => false);
  DBMS_SCHEDULER.SET_ATTRIBUTE_NULL (
   name           =>   '&job_name',
   attribute      =>   'MAX_FAILURES');
end;
/
commit;

exit
