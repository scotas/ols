
---------------------------------------------------------------------
--    Package to do Parallel Operation on Lucene Domain Index      --
---------------------------------------------------------------------
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('getJavaPoolSize','pkg_parallel_lidx','run_in_parallel');
BEGIN
  FOR I IN OBJ_LIST.FIRST..OBJ_LIST.LAST LOOP
  begin
    EXECUTE IMMEDIATE DRP_STMT||OBJ_LIST(I);
  EXCEPTION WHEN OTHERS THEN
    NULL;
  end;
  end loop;
END;
/

-- temporary table used by parallel task
CREATE GLOBAL TEMPORARY TABLE TASK_RESULTS (START_ID NUMBER, RESULT NUMBER) ON COMMIT DELETE ROWS;

-- table used by jobs process
CREATE TABLE BG_PROCESS (host_name VARCHAR2(4000), port NUMBER, bg_process_name VARCHAR2(256), job_name VARCHAR2(128), PRIMARY KEY (host_name, port));

-- table used by flashback process
CREATE TABLE FB_PROCESS (host_name VARCHAR2(4000), port NUMBER, query_time TIMESTAMP, job_name VARCHAR2(128), PRIMARY KEY (query_time));

create or replace package pkg_parallel_lidx authid current_user as
  PROCEDURE addDocToTempIdx(tmpIdx VARCHAR2, prefix VARCHAR2, inserted sys.odciridlist) AS LANGUAGE JAVA NAME
	'com.scotas.lucene.indexer.ParallelIndexOp.addDocToTempIdx(java.lang.String, java.lang.String, oracle.ODCI.ODCIRidList)';
  PROCEDURE addDocToIdx(prefix VARCHAR2, inserted sys.odciridlist) AS LANGUAGE JAVA NAME
	'com.scotas.lucene.indexer.ParallelIndexOp.addDocToIdx(java.lang.String, oracle.ODCI.ODCIRidList)';
  PROCEDURE logSQLErr(err_code NUMBER, err_msg VARCHAR2) AS LANGUAGE JAVA NAME
	'com.scotas.lucene.store.OJVMUtil.logSQLError(java.math.BigDecimal, java.lang.String)';
END pkg_parallel_lidx;
/
show errors

create or replace procedure IndexScanServ(srvPort IN VARCHAR2, logLevel IN VARCHAR2, jmxPort IN VARCHAR2, timeOut IN VARCHAR2) authid current_user as LANGUAGE JAVA NAME
	'com.scotas.lucene.indexer.remote.IndexScanServ.main(java.lang.String [])';
/

create or replace procedure IndexUpdateServ(srvPort IN VARCHAR2, logLevel IN VARCHAR2, jmxPort IN VARCHAR2, timeOut IN VARCHAR2) authid current_user as LANGUAGE JAVA NAME
	'com.scotas.lucene.indexer.remote.IndexUpdateServ.main(java.lang.String [])';
/

create or replace procedure SolrServletServInternal(
     portFlag VARCHAR2, portNumber IN VARCHAR2,
     servletFlag IN VARCHAR2, propFile IN VARCHAR2, nohup IN VARCHAR2,
     keepAlive IN VARCHAR2, errFlag IN VARCHAR2) authid current_user as LANGUAGE JAVA NAME
	'Acme.Serve.Serve.main(java.lang.String [])';
/

create or replace procedure SolrServletServ(srvPort IN VARCHAR2, 
                                            logLevel IN VARCHAR2, 
                                            jmxPort IN VARCHAR2, 
                                            timeOut IN VARCHAR2) authid current_user is
    v_hostname VARCHAR2(4000);
    v_version  VARCHAR2(4000);
    v_action   VARCHAR2(4000);
begin
  v_hostname := SYS_CONTEXT('USERENV','SERVER_HOST');
  v_action   := SYS_CONTEXT('USERENV','ACTION');
  SELECT banner INTO v_version FROM v$version WHERE rownum=1;
  if (jmxPort <> '0' AND instr(v_version,'12c')>0) then
    dbms_java.start_jmx_agent(jmxPort, 'false');
  end if;
  execute immediate 'ALTER SESSION SET tracefile_identifier = '''||v_action||'''';
  begin
     insert into LUCENE.BG_PROCESS values (v_hostname,srvPort,'SolrServlet',v_action);
     commit;
  exception when others then
    null;  -- process already registered
  end;
  -- execute immediate 'ALTER SESSION SET COMMIT_WRITE=''BATCH,NOWAIT''';
  if (logLevel = 'ALL') then
    execute immediate 'ALTER SESSION SET SQL_TRACE=TRUE';
    execute immediate 'ALTER SESSION SET EVENTS ''10046 trace name context forever, level 8''';
  end if;
  SolrServletServInternal('-p',srvPort,'-s','/solrservlet.properties','-nohup','-nka','-err');
end SolrServletServ;
/

create or replace procedure SolrFlashBackServ(srvPort IN VARCHAR2,
                                              logLevel IN VARCHAR2,
                                              jmxPort IN VARCHAR2,
                                              timeOut IN VARCHAR2,
                                              query_time IN TIMESTAMP) authid current_user is
    v_hostname VARCHAR2(4000);
    v_version  VARCHAR2(4000);
    v_action   VARCHAR2(4000);
begin
  v_hostname := SYS_CONTEXT('USERENV','SERVER_HOST');
  v_action   := SYS_CONTEXT('USERENV','ACTION');
  SELECT banner INTO v_version FROM v$version WHERE rownum=1;
  if (jmxPort <> '0' AND instr(v_version,'12c')>0) then
    dbms_java.start_jmx_agent(jmxPort, 'false');
  end if;
  execute immediate 'ALTER SESSION SET tracefile_identifier = '''||v_action||'''';
  begin
     insert into LUCENE.FB_PROCESS values (v_hostname,srvPort,query_time,v_action);
     commit;
  exception when others then
    null;  -- process already registered
  end;
  if (logLevel = 'ALL') then
    execute immediate 'ALTER SESSION SET SQL_TRACE=TRUE';
    execute immediate 'ALTER SESSION SET EVENTS ''10046 trace name context forever, level 8''';
  end if;
  dbms_flashback.enable_at_time(query_time);
  sys.dbms_system.ksdwrt(3,SYS_CONTEXT('USERENV','ACTION')||' - dbms_flashback.enable_at_time: '||to_char(query_time,'DD-MM-YYYY HH24:MI:SS'));
  sys.dbms_system.ksdwrt(3,'Starting Solr Server on port: '||srvPort);
  SolrServletServInternal('-p',srvPort,'-s','/solrfbservlet.properties','-nohup','-nka','-err');
end SolrFlashBackServ;
/

create or replace procedure Heartbeat is
begin
  for i in (select b.host_name,b.port,b.job_name,s.state from user_scheduler_jobs s, bg_process b where s.job_name=b.job_name) loop
     begin
       if (i.state = 'RUNNING') THEN
         NULL; -- check here http connection to b.host_name:b.port
       end if;
       if (i.state = 'SUCCEEDED') THEN
         begin
           DBMS_SCHEDULER.STOP_JOB('LUCENE.'||i.JOB_NAME,force=>true);
         exception when others then
           DBMS_SCHEDULER.ENABLE('LUCENE.'||i.JOB_NAME);
         end;
       end if;
       if (i.state = 'STOPPED') THEN
         DBMS_SCHEDULER.ENABLE('LUCENE.'||i.JOB_NAME);
       end if;
     exception when others then
       null;
     end;
  end loop;
end Heartbeat;
/

begin
  -- Start a Cron like process (DBMS_SCHEDULER)
  DBMS_SCHEDULER.CREATE_JOB(
   job_name          =>  'Hearbeat',
   job_type          =>  'STORED_PROCEDURE',
   job_action        =>  'Heartbeat',
   start_date        =>  SYSDATE,
   repeat_interval   =>  'FREQ=MINUTELY;INTERVAL=5',
   enabled           => true,
   auto_drop         => false);
end;
/
commit;

create or replace FUNCTION getJavaPoolSize RETURN NUMBER authid current_user
AS LANGUAGE JAVA
NAME 'oracle.aurora.vm.OracleRuntime.getJavaPoolSize() return long';
/


CREATE OR REPLACE procedure run_in_parallel(index_schema VARCHAR2, idx_name VARCHAR2, par_degree number, op_function VARCHAR2) authid current_user is
    par_task_name VARCHAR2(128);
    l_stmt CLOB;
  begin
      par_task_name := DBMS_PARALLEL_EXECUTE.generate_task_name;
      DBMS_PARALLEL_EXECUTE.create_task (task_name => par_task_name);
      l_stmt := 'SELECT PARTITION_POSITION,PARTITION_POSITION FROM ALL_IND_PARTITIONS  WHERE INDEX_OWNER='''||INDEX_SCHEMA||''' AND INDEX_NAME='''||IDX_NAME||'''';
      DBMS_PARALLEL_EXECUTE.create_chunks_by_sql(task_name => par_task_name,
                                             sql_stmt  => l_stmt,
                                             by_rowid  => FALSE);
      L_STMT := 'insert into LUCENE.task_results values (:start_id,'||OP_FUNCTION||'(:end_id,'''||INDEX_SCHEMA||''','''||IDX_NAME||'''))';
      DBMS_PARALLEL_EXECUTE.run_task(task_name      => par_task_name,
                                 sql_stmt       => l_stmt,
                                 language_flag  => DBMS_SQL.NATIVE,
                                 parallel_level => par_degree);
      DBMS_PARALLEL_EXECUTE.drop_task(par_task_name);
  end run_in_parallel;
/  

-- GRANTS
grant execute on pkg_parallel_lidx to public;

grant execute on getJavaPoolSize to public;

grant execute on run_in_parallel to public;

grant ALL on TASK_RESULTS to public;

grant select on BG_PROCESS to public;

grant select on FB_PROCESS to public;

create public synonym pkg_parallel_lidx for lucene.pkg_parallel_lidx;

create public synonym getJavaPoolSize for lucene.getJavaPoolSize;

create public synonym run_in_parallel for lucene.run_in_parallel;

exit
