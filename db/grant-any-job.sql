rem usage notes:
rem sqlplus sys/change_on_install@orcl @db/grant-any-job.sql
set long 10000 lines 140 pages 50 timing on echo off
set serveroutput on size 1000000 

grant create any job to LUCENE;
grant execute on dbms_lock to LUCENE;

exit
