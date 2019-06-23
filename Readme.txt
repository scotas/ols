How to install OLS
--------------------------------------------------------
- 12c Binary Distribution
Edit your ~build.properties file with your Database values, for example:

    db.str=orcl
    dba.usr=sys
    dba.pwd=change_on_install

db.str is your SQLNet connect string for your target database, check first with tnsping
ORACLE_HOME environment setting is required and properly configured to an Oracle 11g database layout.
Upload, install and test your code into the database

    # ant
    # ant test-ols-tutorial

Important before installing OLS on 12c check this SGA parameters:
SQL> show parameters job_queue_processes

NAME				     TYPE	 VALUE
------------------------------------ ----------- ------------------------------
job_queue_processes		     integer	 1000
SQL> show parameters aq_tm_processes

NAME				     TYPE	 VALUE
------------------------------------ ----------- ------------------------------
aq_tm_processes 		     integer	 10
SQL> show parameters java

NAME				     TYPE	 VALUE
------------------------------------ ----------- ------------------------------
java_jit_enabled		     boolean	 TRUE
java_max_sessionspace_size	     integer	 0
java_pool_size			     big integer 304M
java_soft_sessionspace_limit	     integer	 0
