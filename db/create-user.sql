rem usage notes:
rem sqlplus sys/change_on_install@orcl @db/create-user.sql table_space_name
rem run on the server machine, because it use dbms_java.loadjava
set long 10000 lines 140 pages 50 timing on echo off
set serveroutput on size 1000000 
whenever SQLERROR EXIT FAILURE
define tbl_name=&1;
define tmp_name=&2;

select owner,index_name,table_owner,table_name from all_indexes where ITYP_OWNER='LUCENE' AND (ITYP_NAME='LUCENEINDEX' OR ITYP_NAME='SOLRINDEX');

-- drop LUCENE users only if exists and there is no domain indexes associated
declare
  vCount NUMBER;
  plsql_block VARCHAR2(4000) := 'drop user LUCENE cascade';
begin
  select count(*) into vCount from all_indexes where ITYP_OWNER='LUCENE' AND (ITYP_NAME='LUCENEINDEX' OR ITYP_NAME='SOLRINDEX');
  if (vCount>0) then
    raise_application_error
        (-20101, 'There are Lucene or Solr Domain Index created, drop them first. Aborting installation....');
  else
    select count(*) into vCount from dba_users where username='LUCENE';
    if (vCount>0) then
      -- drop lucene users
      execute immediate plsql_block;
    end if;
  end if;
end;
/

whenever SQLERROR CONTINUE

create user LUCENE identified by LUCENE
default tablespace &tbl_name
temporary tablespace &tmp_name
quota unlimited on &tbl_name;

grant connect,resource to LUCENE;
grant create public synonym, drop public synonym to LUCENE;
grant create any trigger, drop any trigger to LUCENE;
grant create library to LUCENE;
grant create any directory to LUCENE;
grant create any operator,  create indextype, create table to LUCENE;
grant select any table to LUCENE;
GRANT EXECUTE ON dbms_aq TO LUCENE;
GRANT EXECUTE ON dbms_aqadm TO LUCENE;
GRANT EXECUTE ON dbms_lob TO LUCENE;
GRANT EXECUTE ON dbms_lock TO LUCENE;
GRANT EXECUTE ON dbms_system TO LUCENE;
GRANT EXECUTE ON dbms_flashback TO LUCENE;
grant select on v_$session to LUCENE;
grant select on v_$sqlarea to LUCENE;
-- requires to use DBMS_XPLAN
grant select on V_$SQL_PLAN_STATISTICS_ALL to LUCENE;
grant select on V_$SQL_PLAN to LUCENE;
grant select on V_$SESSION to LUCENE;
grant select on V_$SQL_PLAN_STATISTICS_ALL to LUCENE;
grant select on V_$SQL to LUCENE;
-- debugging priv
grant DEBUG CONNECT SESSION, DEBUG ANY PROCEDURE to LUCENE;
grant jmxserver to LUCENE;
-- Auto trace facilities
grant SELECT_CATALOG_ROLE to LUCENE;
grant alter session to LUCENE;
-- NCOMP required roles on 10g
GRANT JAVASYSPRIV TO LUCENE;
-- Resource Manager Plan
grant SCHEDULER_ADMIN to LUCENE;
grant MANAGE SCHEDULER to LUCENE;

-- JUnit required priv
begin
  dbms_java.grant_permission('LUCENE','SYS:java.io.FilePermission', '/junit.properties', 'read' );
  dbms_java.grant_permission('LUCENE','SYS:java.lang.RuntimePermission', 'getClassLoader', '' );
  dbms_java.grant_permission('LUCENE','SYS:java.lang.RuntimePermission', 'accessDeclaredMembers', '' );
  -- Lucene Tests
  dbms_java.grant_permission('LUCENE','SYS:java.lang.reflect.ReflectPermission', 'suppressAccessChecks', '' );
  dbms_java.grant_permission('LUCENE','SYS:java.util.PropertyPermission', 'org.apache.lucene.store.FSDirectoryLockFactoryClass', 'write' );
  dbms_java.grant_permission('LUCENE','SYS:java.util.PropertyPermission', 'lucene.version', 'read,write');
  -- add this grant also to user who want to use Lucene Domain Index
  dbms_java.grant_permission('LUCENE','SYS:java.util.logging.LoggingPermission', 'control', '' );
  -- Solr Servlet
  dbms_java.grant_permission('LUCENE', 'SYS:java.lang.RuntimePermission', 'shutdownHooks', '' );
  -- add new protocol handler for ols:/ URL
  dbms_java.grant_permission( 'LUCENE', 'SYS:java.util.PropertyPermission', 'java.protocol.handler.pkgs', 'write' );
  dbms_java.grant_permission( 'LUCENE', 'SYS:java.lang.RuntimePermission', 'setContextClassLoader', '' );
  dbms_java.grant_permission( 'LUCENE', 'SYS:java.lang.RuntimePermission', 'setFactory', '' );
  dbms_java.grant_permission( 'LUCENE', 'SYS:java.net.NetPermission', 'specifyStreamHandler', '' );
  -- AQ Role for managing QUEUE in others schemas 12cR2 required
  DBMS_AQADM.GRANT_SYSTEM_PRIVILEGE('MANAGE_ANY','LUCENE',FALSE);
  commit;
end;
/

declare
  vHostName varchar2(4000);
begin
  SELECT SYS_CONTEXT('USERENV','SERVER_HOST') INTO vHostName FROM dual;
  -- Parallel Index Searcher, RMI grants
  dbms_java.grant_permission( 'LUCENE', 'SYS:java.net.SocketPermission', 'localhost:1024-', 'accept,connect,listen,resolve');
  dbms_java.grant_permission( 'LUCENE', 'SYS:java.net.SocketPermission', vHostName||':1024-', 'accept,connect,listen,resolve');
  dbms_java.grant_permission( 'LUCENE', 'SYS:java.lang.RuntimePermission', 'setContextClassLoader', '' );
  
  commit;
end;
/

begin
  -- Misc Solr permission
  dbms_java.grant_permission('LUCENE', 'SYS:javax.management.MBeanServerPermission', 'findMBeanServer', '' );
  dbms_java.grant_permission('LUCENE', 'SYS:javax.management.MBeanTrustPermission', 'register', '' );
  commit;
exception when others then
  null; -- ignore errors, 10g DB
end;
/

declare
  v_version VARCHAR2(4000);
  plsql_block VARCHAR2(4000) := 'BEGIN dbms_java.set_native_compiler_option(''optimizerLoopPagedConversion'',''false'');END;';
begin
  select banner into v_version from v$version where rownum=1;
  if (instr(v_version,'Release 11.1')>0) then
    -- 11g Enterprise Edition Release 11.1.0.7.0, internal bug 7675125
    -- fixed in 11g Enterprise Edition Release 11.2 - Production
    execute immediate plsql_block;
    commit;
  end if;
end;
/
exit
