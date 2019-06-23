
---------------------------------------------------------------------
--    LUCENE PHighligh implementation  bdy                         --
---------------------------------------------------------------------

create or replace
TYPE BODY dla_ot AS
   ----------------------------------------------------------------------------
   STATIC FUNCTION ODCITableDescribe(
                   rtype OUT NOCOPY ANYTYPE,
                   index_name VARCHAR2,
                   qry VARCHAR2,
                   cols VARCHAR2,
                   stmt  IN  VARCHAR2) RETURN NUMBER IS

      r_sql   dla_pkg.rt_dynamic_sql;
      v_rtype ANYTYPE;

  BEGIN

      /*
      || Parse the SQL and describe its format and structure.
      */
      r_sql.cursor := DBMS_SQL.OPEN_CURSOR;
      DBMS_SQL.PARSE( r_sql.cursor, stmt, DBMS_SQL.NATIVE );
      DBMS_SQL.DESCRIBE_COLUMNS2( r_sql.cursor, r_sql.column_cnt, r_sql.description );
      DBMS_SQL.CLOSE_CURSOR( r_sql.cursor );

      /*
      || Create the ANYTYPE record structure from this SQL structure.
      || Replace LONG columns with CLOB...
      */
      ANYTYPE.BeginCreate( DBMS_TYPES.TYPECODE_OBJECT, v_rtype );

      FOR i IN 1 .. r_sql.column_cnt LOOP

         v_rtype.AddAttr( r_sql.description(i).col_name,
                          CASE 
                             --<>--
                             WHEN r_sql.description(i).col_type IN (1,96,11,208)
                             THEN DBMS_TYPES.TYPECODE_VARCHAR2
                             --<>--
                             WHEN r_sql.description(i).col_type = 2
                             THEN DBMS_TYPES.TYPECODE_NUMBER
                             --<LONG defined as CLOB>--
                             WHEN r_sql.description(i).col_type IN (8,112)
                             THEN DBMS_TYPES.TYPECODE_CLOB
                             --<>--
                             WHEN r_sql.description(i).col_type = 12
                             THEN DBMS_TYPES.TYPECODE_DATE
                             --<>--
                             WHEN r_sql.description(i).col_type = 23 
                             THEN DBMS_TYPES.TYPECODE_RAW
                             --<>--
                             WHEN r_sql.description(i).col_type = 180
                             THEN DBMS_TYPES.TYPECODE_TIMESTAMP
                             --<>--
                             WHEN r_sql.description(i).col_type = 181
                             THEN DBMS_TYPES.TYPECODE_TIMESTAMP_TZ
                             --<>--
                             WHEN r_sql.description(i).col_type = 182
                             THEN DBMS_TYPES.TYPECODE_INTERVAL_YM
                             --<>--
                             WHEN r_sql.description(i).col_type = 183
                             THEN DBMS_TYPES.TYPECODE_INTERVAL_DS
                             --<>--
                             WHEN r_sql.description(i).col_type = 231
                             THEN DBMS_TYPES.TYPECODE_TIMESTAMP_LTZ
                             --<>--
                          END,
                          r_sql.description(i).col_precision,
                          r_sql.description(i).col_scale,
                          CASE r_sql.description(i).col_type
                             WHEN 11 
                             THEN 32
                             ELSE r_sql.description(i).col_max_len
                          END,
                          r_sql.description(i).col_charsetid,
                          r_sql.description(i).col_charsetform );
      END LOOP;

      v_rtype.EndCreate;

      /*
      || Now we can use this transient record structure to create a table type
      || of the same. This will create a set of types on the database for use 
      || by the pipelined function...
      */
      ANYTYPE.BeginCreate( DBMS_TYPES.TYPECODE_TABLE, rtype );
      rtype.SetInfo( NULL, NULL, NULL, NULL, NULL, v_rtype,
                     DBMS_TYPES.TYPECODE_OBJECT, 0 );
      rtype.EndCreate();

      RETURN ODCIConst.Success;

   END;

   ----------------------------------------------------------------------------
   STATIC FUNCTION ODCITablePrepare(
                   sctx    OUT NOCOPY dla_ot,
                   tf_info IN  sys.ODCITabFuncInfo,
                   index_name VARCHAR2,
                   qry VARCHAR2,
                   cols VARCHAR2,
                   stmt  IN  VARCHAR2) RETURN NUMBER IS

      r_meta dla_pkg.rt_anytype_metadata;

  BEGIN

      /*
      || We prepare the dataset that our pipelined function will return by
      || describing the ANYTYPE that contains the transient record structure...
      */
      r_meta.typecode := tf_info.rettype.GetAttrElemInfo( 
                            1, r_meta.precision, r_meta.scale, r_meta.length,
                            r_meta.csid, r_meta.csfrm, r_meta.type, r_meta.name 
                            );

      /*
      || Using this, we initialise the scan context for use in this and
      || subsequent executions of the same dynamic SQL cursor...
      */
      sctx := dla_ot(r_meta.type);

      RETURN ODCIConst.Success;

   END;

   ----------------------------------------------------------------------------
   STATIC FUNCTION ODCITableStart(
                   sctx IN OUT NOCOPY dla_ot,
                   index_name VARCHAR2,
                   qry VARCHAR2,
                   cols VARCHAR2,
                   stmt  IN  VARCHAR2) RETURN NUMBER IS

      r_meta dla_pkg.rt_anytype_metadata;

  BEGIN

      /*
      || We now describe the cursor again and use this and the described
      || ANYTYPE structure to define and execute the SQL statement...
      */
      RETURN dla_pkg.ODCITableStart(dla_pkg.ctx,index_name,qry,cols,stmt);

   END;

   ----------------------------------------------------------------------------
   MEMBER FUNCTION ODCITableFetch(
                   SELF   IN OUT NOCOPY dla_ot,
                   nrows  IN     NUMBER,
                   rws    OUT    NOCOPY ANYDATASET
                   ) RETURN NUMBER IS

      r_fetch rt_fetch_attributes;
      colVals RowInfo;
      retVal  NUMBER;
      jdbcTypeCode NUMBER;
   BEGIN
      retVal := dla_pkg.ODCITableFetch(dla_pkg.ctx,nrows,colVals);
      
      IF colVals is not null THEN

         /*
         || First we describe our current ANYTYPE instance (SELF.A) to determine
         || the number and types of the attributes...
         */

         /*
         || We can now begin to piece together our returning dataset. We create an
         || instance of ANYDATASET and then fetch the attributes off the DBMS_SQL
         || cursor using the metadata from the ANYTYPE. LONGs are converted to CLOBs...
         */
         ANYDATASET.BeginCreate( DBMS_TYPES.TYPECODE_OBJECT, SELF.atype, rws );
         rws.AddInstance();
         rws.PieceWise();

         FOR i IN 1 .. colVals.COUNT LOOP
            r_fetch := colVals(i);
            
            jdbcTypeCode := r_fetch.typeCode;
            
            CASE
               --<JDBC String>--
               WHEN jdbcTypeCode = 12
               THEN
                  rws.SetVarchar2( r_fetch.v2_column );
               --<JDBC BigDecimal>--
               WHEN jdbcTypeCode = 2
               THEN
                  rws.SetNumber( r_fetch.num_column );
               --<JDBC Date>--
               WHEN jdbcTypeCode = 91
               THEN
                  rws.SetDate( r_fetch.date_column );
               --<JDBC Timestamp>--
               WHEN jdbcTypeCode = 93
               THEN
                  rws.SetTimestamp( r_fetch.ts_column );
               --<JDBC TimestampTZ>--
               WHEN jdbcTypeCode = -101
               THEN
                  rws.SetTimestampTZ( r_fetch.tstz_column );
               --<JDBC TimestampLTZ>--
               WHEN jdbcTypeCode = -102
               THEN
                  rws.SetTimestampLTZ( r_fetch.tsltz_column );
               --<JDBC CLOB>--
               WHEN jdbcTypeCode = 2005
               THEN
                  --<>--
                  rws.SetClob( r_fetch.clob_column );
               --<>--
            END CASE;
         END LOOP;
   
         /*
         || Our ANYDATASET instance is complete. We end our create session...
         */    
         rws.EndCreate();

      END IF;

      RETURN retVal;

   END;

   ----------------------------------------------------------------------------
   MEMBER FUNCTION ODCITableClose(
                   SELF IN dla_ot
                   ) RETURN NUMBER IS
   BEGIN
      RETURN dla_pkg.ODCITableClose(dla_pkg.ctx);
   END;

END;
/
show errors

create or replace
TYPE BODY cla_ot AS
   ----------------------------------------------------------------------------
   STATIC FUNCTION ODCITableDescribe(
                   rtype OUT NOCOPY ANYTYPE,
                   index_name VARCHAR2,
                   qry VARCHAR2,
                   cols VARCHAR2,
                   returnType IN  VARCHAR2,
                   rws    IN  SYS_REFCURSOR) RETURN NUMBER IS
      v_owner           VARCHAR2(30);
      v_type_name       VARCHAR2(30);
      dot_pos           INTEGER;
      v_elem_type_name  VARCHAR2(30);
      v_elem_type_owner VARCHAR2(30);
   BEGIN
       dot_pos := instr(returnType,'.');
       if dot_pos > 0 then
          v_owner := substr(returnType,1,dot_pos-1);
          v_type_name  := substr(returnType,dot_pos+1, length(returnType)-dot_pos);
       else
          v_owner := sys_context('userenv','session_user');
          v_type_name  := returnType;
       end if;
       select elem_type_name, elem_type_owner
         into v_elem_type_name,v_elem_type_owner
         from all_coll_types where owner=v_owner and type_name=v_type_name and coll_type='TABLE';
       rtype:=AnyType.GetPersistent(v_owner,v_type_name);
       RETURN ODCIConst.Success;
   END;

   ----------------------------------------------------------------------------
   STATIC FUNCTION ODCITablePrepare(
                   sctx    OUT NOCOPY cla_ot,
                   tf_info IN  sys.ODCITabFuncInfo,
                   index_name VARCHAR2,
                   qry VARCHAR2,
                   cols VARCHAR2,
                   returnType IN  VARCHAR2,
                   rws    IN  SYS_REFCURSOR) RETURN NUMBER IS

      r_meta dla_pkg.rt_anytype_metadata;
   BEGIN

      /*
      || We prepare the dataset that our pipelined function will return by
      || describing the ANYTYPE that contains the transient record structure...
      */
      r_meta.typecode := tf_info.rettype.GetAttrElemInfo( 
                            1, r_meta.precision, r_meta.scale, r_meta.length,
                            r_meta.csid, r_meta.csfrm, r_meta.type, r_meta.name 
                            );

      /*
      || Using this, we initialise the scan context for use in this and
      || subsequent executions of the same dynamic SQL cursor...
      */
      sctx := cla_ot(r_meta.type);

      RETURN ODCIConst.Success;
   END;

   ----------------------------------------------------------------------------
   STATIC FUNCTION ODCITableStart(
                   sctx IN OUT NOCOPY cla_ot,
                   index_name VARCHAR2,
                   qry VARCHAR2,
                   cols VARCHAR2,
                   returnType IN  VARCHAR2,
                   rws    IN  SYS_REFCURSOR) RETURN NUMBER IS
  BEGIN

      /*
      || We now describe the cursor again and use this and the described
      || ANYTYPE structure to define and execute the SQL statement...
      */
      RETURN dla_pkg.ODCITableStart(dla_pkg.ctx,index_name,qry,cols,rws);

   END;

   ----------------------------------------------------------------------------
   MEMBER FUNCTION ODCITableFetch(
                   SELF   IN OUT NOCOPY cla_ot,
                   nrows  IN     NUMBER,
                   rws    OUT    NOCOPY ANYDATASET
                   ) RETURN NUMBER IS

      r_fetch rt_fetch_attributes;
      colVals RowInfo;
      retVal  NUMBER;
      jdbcTypeCode NUMBER;
   BEGIN
      retVal := dla_pkg.ODCITableFetch(dla_pkg.ctx,nrows,colVals);
      
      IF colVals is not null THEN

         /*
         || First we describe our current ANYTYPE instance (SELF.A) to determine
         || the number and types of the attributes...
         */

         /*
         || We can now begin to piece together our returning dataset. We create an
         || instance of ANYDATASET and then fetch the attributes off the DBMS_SQL
         || cursor using the metadata from the ANYTYPE. LONGs are converted to CLOBs...
         */
         ANYDATASET.BeginCreate( DBMS_TYPES.TYPECODE_OBJECT, SELF.atype, rws );
         rws.AddInstance();
         rws.PieceWise();

         FOR i IN 1 .. colVals.COUNT LOOP
            r_fetch := colVals(i);
            
            jdbcTypeCode := r_fetch.typeCode;
            
            CASE
               --<JDBC String>--
               WHEN jdbcTypeCode = 12
               THEN
                  rws.SetVarchar2( r_fetch.v2_column );
               --<JDBC BigDecimal>--
               WHEN jdbcTypeCode = 2
               THEN
                  rws.SetNumber( r_fetch.num_column );
               --<JDBC Date>--
               WHEN jdbcTypeCode = 91
               THEN
                  rws.SetDate( r_fetch.date_column );
               --<JDBC Timestamp>--
               WHEN jdbcTypeCode = 93
               THEN
                  rws.SetTimestamp( r_fetch.ts_column );
               --<JDBC TimestampTZ>--
               WHEN jdbcTypeCode = -101
               THEN
                  rws.SetTimestampTZ( r_fetch.tstz_column );
               --<JDBC TimestampLTZ>--
               WHEN jdbcTypeCode = -102
               THEN
                  rws.SetTimestampLTZ( r_fetch.tsltz_column );
               --<JDBC CLOB>--
               WHEN jdbcTypeCode = 2005
               THEN
                  --<>--
                  rws.SetClob( r_fetch.clob_column );
               --<>--
            END CASE;
         END LOOP;
   
         /*
         || Our ANYDATASET instance is complete. We end our create session...
         */    
         rws.EndCreate();

      END IF;

      RETURN retVal;

   END;

   ----------------------------------------------------------------------------
   MEMBER FUNCTION ODCITableClose(
                   SELF IN cla_ot
                   ) RETURN NUMBER IS
   BEGIN
      RETURN dla_pkg.ODCITableClose(dla_pkg.ctx);
   END;
END;
/
show errors

exit