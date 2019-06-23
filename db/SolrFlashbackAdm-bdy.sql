---------------------------------------------------------------------
--    Solr Flashback Domain Index Admin Methods                    --
---------------------------------------------------------------------
create or replace
package body SolrFlashbackAdm as
   procedure DefinePeriod(httpBasePort IN NUMBER, jmxBasePort IN NUMBER, periodTable IN PeriodTable_arr) is
   begin
       for i in 1..periodTable.count loop
         -- TODO: Check here if there snapshot available at periodTable(i) timestamp
         --       to avoid ORA-01555: snapshot too old
         --                ORA-22924: snapshot too old
         DBMS_SCHEDULER.CREATE_JOB(
   job_name          =>  'SolrFlashbackAdm#'||i,
   job_type          =>  'PLSQL_BLOCK',
   job_action        =>  '
begin
   SolrFlashBackServ('''||(httpBasePort+i-1)||
      ''',''WARNING'','''||(case jmxBasePort when 0 then 0 else jmxBasePort+i-1 end)||
      ''',''0'',to_timestamp('''||
      to_char(periodTable(i),'DD-MM-YYYY HH24:MI:SS')||
      ''',''DD-MM-YYYY HH24:MI:SS''));
   exception when others then
      sys.dbms_system.ksdwrt(3,DBMS_UTILITY.format_error_stack);
end;',
   start_date        =>  SYSDATE,
   enabled           => true,
   auto_drop         => true);
  DBMS_SCHEDULER.SET_ATTRIBUTE_NULL (
         name           =>   'SolrFlashbackAdm#'||i,
         attribute      =>   'MAX_FAILURES');
       end loop;
   end DefinePeriod;

   procedure Clean is
   begin
     for c in (select JOB_NAME from user_scheduler_jobs where job_name like 'SOLRFLASHBACKADM#%') loop
       begin
         DBMS_SCHEDULER.DROP_JOB(job_name => c.JOB_NAME, force => true);
       EXCEPTION WHEN OTHERS THEN
         null;
       end;
     end loop;
     execute immediate 'truncate table LUCENE.FB_PROCESS';
   end Clean;
end;
/
show errors

exit
