rem 
rem 
Rem Copyright (c) 1988, 2004, Oracle. All rights reserved.  
Rem NAME
REM    UTLXPLAN.SQL
Rem  FUNCTION
Rem  NOTES
Rem  MODIFIED
Rem     bdagevil   05/08/04  - add other_xml column 
Rem     bdagevil   06/18/03  - rename hint alias to object_alias
Rem     ddas       06/03/03  - increase size of hint alias column
Rem     bdagevil   02/13/03  - add plan_id and depth column
Rem     ddas       01/17/03  - add query_block and hint_alias columns
Rem     ddas       11/04/02  - revert timestamp column to DATE (PL/SQL problem)
Rem     ddas       10/28/02  - change type of timestamp column to TIMESTAMP
Rem     ddas       10/03/02  - add estimated_time column
Rem     mzait      04/16/02  - add row vector to the plan table
Rem     mzait      10/26/01  - add keys and filter predicates to the plan table
Rem     ddas       05/05/00  - increase length of options column
Rem     ddas       04/17/00  - add CPU, I/O cost, temp_space columns
Rem     mzait      02/19/98 -  add distribution method column
Rem     ddas       05/17/96 -  change search_columns to number
Rem     achaudhr   07/23/95 -  PTI: Add columns partition_{start, stop, id}
Rem     glumpkin   08/25/94 -  new optimizer fields
Rem     jcohen     11/05/93 -  merge changes from branch 1.1.710.1 - 9/24
Rem     jcohen     09/24/93 -  #163783 add optimizer column
Rem     glumpkin   10/25/92 -  Renamed from XPLAINPL.SQL 
Rem     jcohen     05/22/92 -  #79645 - set node width to 128 (M_XDBI in gendef)
Rem     rlim       04/29/91 -  change char to varchar2 
Rem     Peeler     10/19/88 - Creation
Rem
Rem This is the format for the table that is used by the EXPLAIN PLAN
Rem statement.  The explain statement requires the presence of this 
Rem table in order to store the descriptions of the row sources.
drop table PLAN_TABLE;

create table PLAN_TABLE (
        statement_id       varchar2(30),
        plan_id            number,
        timestamp          date,
        remarks            varchar2(4000),
        operation          varchar2(30),
        options            varchar2(255),
        object_node        varchar2(128),
        object_owner       varchar2(30),
        object_name        varchar2(30),
        object_alias       varchar2(65),
        object_instance    numeric,
        object_type        varchar2(30),
        optimizer          varchar2(255),
        search_columns     number,
        id                 numeric,
        parent_id          numeric,
        depth              numeric,
        position           numeric,
        cost               numeric,
        cardinality        numeric,
        bytes              numeric,
        other_tag          varchar2(255),
        partition_start    varchar2(255),
        partition_stop     varchar2(255),
        partition_id       numeric,
        other              long,
        distribution       varchar2(30),
        cpu_cost           numeric,
        io_cost            numeric,
        temp_space         numeric,
        access_predicates  varchar2(4000),
        filter_predicates  varchar2(4000),
        projection         varchar2(4000),
        time               numeric,
        qblock_name        varchar2(30),
        other_xml          clob
);
