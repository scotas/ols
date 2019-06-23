rem Register an XDB Servlet
rem execute logged as SYS or other DBA user
rem requires one argument with the schema used to test Solr, for example LUCENE
set define '$';
define sch=$1;

rem SolrServlet test
rem http://localhost:8080/solr/select?indent=on&q=video&wt=json&start=1&rows=3&fl=score
DECLARE
  configxml SYS.XMLType;
begin
  dbms_xdb.deleteServletMapping('SolrServlet');
  dbms_xdb.deleteServlet('SolrServlet');
  dbms_xdb.addServlet(name=>'SolrServlet',language=>'Java',class=>'com.scotas.solr.servlet.SolrServlet',dispname=>'Solr Select Servlet',schema=>'LUCENE');
  dbms_xdb.addServletSecRole(SERVNAME => 'SolrServlet',ROLENAME => 'authenticatedUser',ROLELINK => 'authenticatedUser');
  dbms_xdb.addServletMapping('/solr/select/*','SolrServlet');
  commit;
end;
/
commit;

rem SolrUpdateServlet test
rem http://localhost:8080/solr/update
DECLARE
  configxml SYS.XMLType;
begin
  dbms_xdb.deleteServletMapping('SolrUpdateServlet');
  dbms_xdb.deleteServlet('SolrUpdateServlet');
  dbms_xdb.addServlet(name=>'SolrUpdateServlet',language=>'Java',class=>'com.scotas.solr.servlet.SolrUpdateServlet',dispname=>'Solr Update Servlet',schema=>'LUCENE');
  dbms_xdb.addServletSecRole(SERVNAME => 'SolrUpdateServlet',ROLENAME => 'authenticatedUser',ROLELINK => 'authenticatedUser');
  dbms_xdb.addServletMapping('/solr/update/*','SolrUpdateServlet');
  commit;
end;
/
commit;

rem SolrLoggingServlet test
rem http://localhost:8080/solr/admin/logging
DECLARE
  configxml SYS.XMLType;
begin
  dbms_xdb.deleteServletMapping('SolrLoggingServlet');
  dbms_xdb.deleteServlet('SolrLoggingServlet');
  dbms_xdb.addServlet(name=>'SolrLoggingServlet',language=>'Java',class=>'org.apache.solr.servlet.LogLevelSelection',dispname=>'Solr Logging Servlet',schema=>'LUCENE');
  dbms_xdb.addServletSecRole(SERVNAME => 'SolrUpdateServlet',ROLENAME => 'authenticatedUser',ROLELINK => 'authenticatedUser');
  dbms_xdb.addServletMapping('/solr/admin/logging/*','SolrLoggingServlet');
  commit;
end;
/
commit;

