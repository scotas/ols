  rem usage notes:
rem sqlplus sys/change_on_install@orcl @db/create-lucene-role.sql
rem run on the server machine, because it use dbms_java.loadjava
set long 10000 lines 140 pages 50 timing on echo off
set serveroutput on size 1000000
-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop role ';
  obj_list obj_arr := obj_arr('LUCENEUSER');
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

create role LUCENEUSER;

-- required for parallel processing DBMS_PARALLEL_EXECUTE.run_task
grant create job to LUCENEUSER;

declare
  vHostName varchar2(4000);
begin
  SELECT SYS_CONTEXT('USERENV','SERVER_HOST') INTO vHostName FROM dual;
  dbms_java.grant_permission( 'LUCENEUSER','SYS:java.lang.RuntimePermission', 'getClassLoader', '' );
  dbms_java.grant_permission( 'LUCENEUSER','SYS:java.util.logging.LoggingPermission', 'control', '' );
  dbms_java.grant_permission( 'LUCENEUSER','SYS:java.lang.RuntimePermission', 'accessDeclaredMembers', '' );
  dbms_java.grant_permission( 'LUCENEUSER', 'SYS:java.net.SocketPermission', 'localhost:1024-', 'resolve,connect' );
  dbms_java.grant_permission( 'LUCENEUSER','SYS:java.net.SocketPermission', vHostName||':1024-', 'resolve,connect' );
  dbms_java.grant_permission( 'LUCENEUSER', 'SYS:java.net.NetPermission','setDefaultAuthenticator', '' );
  commit;
end;
/

exit
