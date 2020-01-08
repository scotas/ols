-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('SSYNC_PARTITION','SOPTIMIZE_PARTITION','SREBUILD_PARTITION');
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

-- Parallel index functions
CREATE OR REPLACE FUNCTION SSYNC_PARTITION(P_ID NUMBER, IDX_OWNER VARCHAR2, IDX_NAME VARCHAR2)  RETURN NUMBER authid current_user AS 
  PRAGMA AUTONOMOUS_TRANSACTION;
  P_NAME VARCHAR2(30);
BEGIN
  SELECT PARTITION_NAME INTO P_NAME FROM ALL_IND_PARTITIONS  WHERE INDEX_OWNER=IDX_OWNER AND INDEX_NAME=IDX_NAME AND PARTITION_POSITION=P_ID;
  SOLRDOMAININDEX.SYNC(IDX_OWNER,IDX_NAME,P_NAME);
  COMMIT;
  RETURN 1;
END;
/

CREATE OR REPLACE FUNCTION SOPTIMIZE_PARTITION(P_ID NUMBER, IDX_OWNER VARCHAR2, IDX_NAME VARCHAR2)  RETURN NUMBER authid current_user AS
  PRAGMA AUTONOMOUS_TRANSACTION;
  P_NAME VARCHAR2(30);
BEGIN
  SELECT PARTITION_NAME INTO P_NAME FROM ALL_IND_PARTITIONS  WHERE INDEX_OWNER=IDX_OWNER AND INDEX_NAME=IDX_NAME AND PARTITION_POSITION=P_ID;
  SOLRDOMAININDEX.OPTIMIZE(IDX_OWNER,IDX_NAME,P_NAME);
  COMMIT;
  RETURN 1;
END;
/

CREATE OR REPLACE FUNCTION SREBUILD_PARTITION(P_ID NUMBER, IDX_OWNER VARCHAR2, IDX_NAME VARCHAR2)  RETURN NUMBER authid current_user AS
  PRAGMA AUTONOMOUS_TRANSACTION;
  P_NAME VARCHAR2(30);
BEGIN
  SELECT PARTITION_NAME INTO P_NAME FROM ALL_IND_PARTITIONS  WHERE INDEX_OWNER=IDX_OWNER AND INDEX_NAME=IDX_NAME AND PARTITION_POSITION=P_ID;
  SOLRDOMAININDEX.REBUILD(IDX_OWNER,IDX_NAME,P_NAME);
  COMMIT;
  RETURN 1;
END;
/

GRANT EXECUTE ON SSYNC_PARTITION TO PUBLIC;

GRANT EXECUTE ON SOPTIMIZE_PARTITION TO PUBLIC;

GRANT EXECUTE ON SREBUILD_PARTITION TO PUBLIC;

CREATE PUBLIC SYNONYM SSYNC_PARTITION FOR LUCENE.SSYNC_PARTITION;

CREATE PUBLIC SYNONYM SOPTIMIZE_PARTITION FOR LUCENE.SOPTIMIZE_PARTITION;

CREATE PUBLIC SYNONYM SREBUILD_PARTITION FOR LUCENE.SREBUILD_PARTITION;

---------------------------------------------------------------------
--    Solr Index Method  Implemented as Trusted Callouts  --
---------------------------------------------------------------------

create or replace
type body SolrDomainIndex is
  static function getIndexPrefix(ia SYS.ODCIIndexInfo) return VARCHAR2 is
  begin
    if (ia.IndexPartition is not null) then
      return ia.IndexSchema || '.' || ia.IndexName || '$' || ia.IndexPartition;
    else
      return ia.IndexSchema || '.' || ia.IndexName;
    end if;
  end getIndexPrefix;

  static function ODCIGetInterfaces(
    ifclist out NOCOPY sys.ODCIObjectList) return number is
  begin
    ifclist := sys.ODCIObjectList(sys.ODCIObject('SYS','ODCIINDEX2'));
    return sys.ODCIConst.Success;
  end ODCIGetInterfaces;

  STATIC FUNCTION TextContains(Text IN VARCHAR2, Key IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextContains(Text,Key,null,indexctx,sctx,scanflg);
  end TextContains;

  STATIC FUNCTION TextContains(Text IN CLOB, Key IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextContains(Text,Key,null,indexctx,sctx,scanflg);
  end TextContains;

  STATIC FUNCTION TextContains(Text IN XMLType, Key IN VARCHAR2,
                               indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextContains(Text,Key,null,indexctx,sctx,scanflg);
  end TextContains;

  STATIC FUNCTION TextScore(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextScore(Text,Key,null,indexctx,sctx,scanflg);
  end TextScore;

  STATIC FUNCTION TextScore(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextScore(Text,Key,null,indexctx,sctx,scanflg);
  end TextScore;

  STATIC FUNCTION TextScore(Text IN CLOB, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextScore('',Key,null,indexctx,sctx,scanflg);
  end TextScore;

  STATIC FUNCTION TextScore(Text IN XMLType, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextScore(Text,Key,null,indexctx,sctx,scanflg);
  end TextScore;

  STATIC FUNCTION TextScore(Text IN XMLType, Key IN VARCHAR2, Sort IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN NUMBER is
  begin
     return TextScore('',Key,null,indexctx,sctx,scanflg);
  end TextScore;

  STATIC FUNCTION TextHighlight(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2 is
  begin
     return TextHighlight(Text,Key,null,indexctx,sctx,scanflg);
  end TextHighlight;

  STATIC FUNCTION TextHighlight(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2 is
  begin
     return TextHighlight(Text,Key,null,indexctx,sctx,scanflg);
  end TextHighlight;

  STATIC FUNCTION TextHighlight(Text IN XMLType, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN VARCHAR2 is
  begin
     return TextHighlight(Text,Key,null,indexctx,sctx,scanflg);
  end TextHighlight;

  STATIC FUNCTION TextMlt(Text IN VARCHAR2, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN sys.ODCIRidList is
  begin
     return TextMlt(Text,Key,null,indexctx,sctx,scanflg);
  end TextMlt;

  STATIC FUNCTION TextMlt(Text IN CLOB, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN sys.ODCIRidList is
  begin
     return TextMlt(Text,Key,null,indexctx,sctx,scanflg);
  end TextMlt;

  STATIC FUNCTION TextMlt(Text IN XMLType, Key IN VARCHAR2,
                            indexctx IN sys.ODCIIndexCtx, sctx IN OUT NOCOPY SolrDomainIndex, scanflg IN NUMBER) RETURN sys.ODCIRidList is
  begin
     return TextMlt(Text,Key,null,indexctx,sctx,scanflg);
  end TextMlt;

  STATIC FUNCTION ODCIIndexStart(sctx IN OUT NOCOPY SolrDomainIndex,
        ia SYS.ODCIIndexInfo, op SYS.ODCIPredInfo, qi sys.ODCIQueryInfo,
        strt number, stop number,
        cmppos NUMBER, cmpval VARCHAR2, env SYS.ODCIEnv) RETURN NUMBER is
  begin
     return ODCIIndexStart(sctx,ia,op,qi,strt,stop,cmpval,null,env);
  end ODCIIndexStart;

  STATIC FUNCTION ODCIIndexStart(sctx IN OUT NOCOPY SolrDomainIndex,
        ia SYS.ODCIIndexInfo, op SYS.ODCIPredInfo, qi sys.ODCIQueryInfo,
        strt number, stop number,
        cmpval VARCHAR2, env SYS.ODCIEnv) RETURN NUMBER is
  begin
     return ODCIIndexStart(sctx,ia,op,qi,strt,stop,cmpval,null,env);
  end ODCIIndexStart;


  MEMBER FUNCTION ODCIIndexFetch(nrows NUMBER, rids OUT NOCOPY SYS.ODCIridlist, env SYS.ODCIEnv) RETURN NUMBER is
    trids   SYS.ODCIridlist;
    stmt    VARCHAR2(200) := ODCIIndexFetchInternal(NROWS,TRIDS,ENV)
               || ' L$ORIG$TBL, TABLE(:1) L$PIPE$TBL WHERE L$ORIG$TBL.rowid = L$PIPE$TBL.COLUMN_VALUE';
  begin
     EXECUTE IMMEDIATE 'SELECT /*+ cardinality(L$PIPE$TBL, '||NVL(trids.last,0)||') */ rowid FROM ' || stmt BULK COLLECT INTO rids using trids;
     return sys.ODCICONST.SUCCESS;
  exception when others then
     DBMS_OUTPUT.PUT_LINE(DBMS_UTILITY.FORMAT_ERROR_STACK);
     return sys.ODCICONST.ERROR;
  end ODCIIndexFetch;

  static procedure msgCallBack(context  IN  RAW,
                               reginfo  IN  SYS.AQ$_REG_INFO,
                               descr    IN  SYS.AQ$_DESCRIPTOR,
                               payload  IN  RAW,
                               payloadl IN  NUMBER) is
    dequeue_options     dbms_aq.dequeue_options_t;
    enqueue_options     dbms_aq.enqueue_options_t;
    message_properties  dbms_aq.message_properties_t;
    message_handle      RAW(16);
    message             LUCENE.lucene_msg_typ;
    ctxInfo             VARCHAR2(4000) := utl_raw.cast_to_varchar2(context);
    prefix              VARCHAR2(400) := substr(ctxInfo,1,instr(ctxInfo,'|')-1);
    parInfo             VARCHAR2(40) := substr(ctxInfo,instr(ctxInfo,'|')+1);
    rcount              NUMBER;
    v_code              NUMBER;
    v_errm              varchar2(4000);
    tmpIdx              VARCHAR2(4000);
    parentPrefix        VARCHAR2(4000);
    cntParallel         NUMBER;
    parDegree           NUMBER := to_number(substr(parInfo,1,instr(parInfo,':')-1));
    indexOnRam          VARCHAR2(1) := substr(parInfo,instr(parInfo,':')+1);
    lock_handle         VARCHAR2(128);
    dummy               PLS_INTEGER;
    begin
      -- start the MEP (mutually exclusive part)
      dbms_lock.allocate_unique(prefix,lock_handle);
      dummy := dbms_lock.request(lock_handle,dbms_lock.x_mode,dbms_lock.maxwait,true);
      -- get the consumer name and msg_id from the descriptor
      dequeue_options.delivery_mode := dbms_aq.persistent;
      dequeue_options.visibility    := dbms_aq.on_commit;
      dequeue_options.msgid         := descr.msg_id;
      dequeue_options.consumer_name := descr.consumer_name;
      dequeue_options.wait          := dbms_aq.no_wait;
      begin
        dbms_aq.dequeue(queue_name        =>     descr.queue_name,
                      dequeue_options     =>     dequeue_options,
                      message_properties  =>     message_properties,
                      payload             =>     message,
                      msgid               =>     message_handle);
        case
        when (message.operation = 'insert') then
           syncInternal(prefix,sys.ODCIRidList(),message.ridlist);
        when (message.operation = 'update') then
           syncInternal(prefix,sys.ODCIRidList(),message.ridlist);
        when (message.operation = 'delete') then
           syncInternal(prefix,message.ridlist,sys.ODCIRidList());
        when (message.operation = 'rebuild') then
           rebuildInternal(prefix);
        when (message.operation = 'optimize') then
           optimizeInternal(prefix);
        end case;
        -- release locks
        commit;
      exception when others then
         ROLLBACK;  -- undo changes
         v_code := SQLCODE;
         Pkg_Parallel_lidx.logSQLErr(v_code, DBMS_UTILITY.format_error_stack);
      end;
      -- v_errm := dbms_java.endsession_and_related_state;
      dummy := dbms_lock.release(lock_handle);
  end msgCallBack;

  static procedure enableCallBack(prefix VARCHAR2) is
      reginfo             sys.aq$_reg_info;
      reginfolist         sys.aq$_reg_info_list;
      tablePrefix         varchar2(400) := substr(prefix,1,instr(prefix,'|')-1);
  begin
      reginfo := sys.aq$_reg_info(tablePrefix||'$Q',
                                DBMS_AQ.NAMESPACE_AQ,
                                'plsql://SolrDomainIndex.msgCallBack',
                                utl_raw.cast_to_raw(prefix));
    reginfolist := sys.aq$_reg_info_list(reginfo);
    sys.dbms_aq.register(reginfolist, 1);
  end enableCallBack;

  static procedure disableCallBack(prefix VARCHAR2) is
      reginfo             sys.aq$_reg_info;
      reginfolist         sys.aq$_reg_info_list;
      tablePrefix         varchar2(400) := substr(prefix,1,instr(prefix,'|')-1);
  begin
      reginfo := sys.aq$_reg_info(tablePrefix||'$Q',
                                DBMS_AQ.NAMESPACE_AQ,
                                'plsql://SolrDomainIndex.msgCallBack',
                                utl_raw.cast_to_raw(prefix));
    reginfolist := sys.aq$_reg_info_list(reginfo);
    sys.dbms_aq.unregister(reginfolist, 1);
  end disableCallBack;

  static procedure enqueueChange(prefix VARCHAR2, rid VARCHAR2, operation VARCHAR2) is
  begin
      enqueueChange(prefix,sys.ODCIRidList(rid),operation);
  end enqueueChange;

  static procedure enqueueChange(prefix VARCHAR2, ridlist sys.odciridlist, operation VARCHAR2) is
      enqueue_options     DBMS_AQ.enqueue_options_t;
      message_properties  DBMS_AQ.message_properties_t;
      message_handle      RAW(16);
      message             LUCENE.lucene_msg_typ;
      v_code              NUMBER;
  begin
      message := LUCENE.lucene_msg_typ(ridlist,operation);
      enqueue_options.delivery_mode := dbms_aq.persistent;
      enqueue_options.visibility    := dbms_aq.on_commit;
      dbms_aq.enqueue(queue_name         => prefix||'$Q',
                      enqueue_options    => enqueue_options,
                      message_properties => message_properties,
                      payload            => message,
                      msgid              => message_handle);
  exception when others then
    v_code := SQLCODE;
    Pkg_Parallel_lidx.logSQLErr(v_code, DBMS_UTILITY.format_error_stack);
  end enqueueChange;

  -- Array DML version --
  static function ODCIIndexInsert(ia sys.ODCIIndexInfo, ridlist sys.ODCIRidList, env sys.ODCIEnv) return NUMBER is
  begin
     enqueueChange(getIndexPrefix(ia),ridlist,'insert');
     return sys.ODCICONST.SUCCESS;
  end ODCIIndexInsert;

  static function ODCIIndexUpdate(ia sys.ODCIIndexInfo, ridlist sys.odciridlist, env sys.ODCIEnv) return NUMBER is
  begin
     enqueueChange(getIndexPrefix(ia),ridlist,'update');
     return sys.ODCICONST.SUCCESS;
  end ODCIIndexUpdate;

  static function ODCIIndexUpdate(ia sys.ODCIIndexInfo, ridlist sys.odciridlist, oldValList sys.ODCIColArrayValList, newValList sys.ODCIColArrayValList, env sys.ODCIEnv) return NUMBER is
  begin
     enqueueChange(getIndexPrefix(ia),ridlist,'update');
     return sys.ODCICONST.SUCCESS;
  end ODCIIndexUpdate;

  static function ODCIIndexDelete(ia sys.ODCIIndexInfo, ridlist sys.odciridlist, env sys.ODCIEnv) return NUMBER is
  begin
     enqueueChange(getIndexPrefix(ia),ridlist,'delete');
     return sys.ODCICONST.SUCCESS;
  end ODCIIndexDelete;
  
  static procedure sync(index_name VARCHAR2) is
    index_schema VARCHAR2(30);
    idx_name VARCHAR2(30) := index_name;
    is_part varchar2(3);
    par_degree number;
    v_version VARCHAR2(4000);
  begin
    select banner into v_version from v$version where rownum=1;
    SELECT OWNER,PARTITIONED,DEGREE INTO INDEX_SCHEMA,IS_PART,PAR_DEGREE FROM ALL_INDEXES WHERE INDEX_NAME=IDX_NAME;
    IF (IS_PART = 'YES' AND instr(v_version,'19c')>0) THEN
      if (PAR_DEGREE > 1 AND instr(v_version,'19c')>0) then
        EXECUTE IMMEDIATE 'begin run_in_parallel('''||INDEX_SCHEMA||''','''||IDX_NAME||''','||PAR_DEGREE||',''SSYNC_PARTITION''); end;';
      else
        FOR P IN (SELECT PARTITION_NAME FROM ALL_IND_PARTITIONS  WHERE INDEX_OWNER=INDEX_SCHEMA AND INDEX_NAME=IDX_NAME) LOOP
           SYNC(INDEX_SCHEMA,INDEX_NAME,P.PARTITION_NAME);
        end loop;
      end if;
    ELSE
      SYNC(INDEX_SCHEMA,INDEX_NAME);
    end if;
    exception when no_data_found then
      RAISE_APPLICATION_ERROR
      (-20101, 'Index not found: '||idx_name);
    when too_many_rows then
      INDEX_SCHEMA := SYS_CONTEXT('USERENV','CURRENT_SCHEMA');
      SELECT PARTITIONED,DEGREE INTO IS_PART,PAR_DEGREE FROM ALL_INDEXES WHERE INDEX_NAME=IDX_NAME and OWNER=INDEX_SCHEMA;
      IF (IS_PART = 'YES') THEN
        if (PAR_DEGREE > 1 AND instr(v_version,'19c')>0) then
          EXECUTE IMMEDIATE 'begin run_in_parallel('''||INDEX_SCHEMA||''','''||IDX_NAME||''','||PAR_DEGREE||',''SSYNC_PARTITION''); end;';
        else
          FOR P IN (SELECT PARTITION_NAME FROM ALL_IND_PARTITIONS  WHERE INDEX_OWNER=INDEX_SCHEMA AND INDEX_NAME=IDX_NAME) LOOP
             SYNC(INDEX_SCHEMA,INDEX_NAME,P.PARTITION_NAME);
          end loop;
        end if;
      ELSE
        SYNC(INDEX_SCHEMA,INDEX_NAME);
      END IF;
  end sync;

  static procedure sync(owner VARCHAR2, index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL) is
    deleted                     sys.odciridlist := sys.ODCIRidList();
    inserted                    sys.odciridlist := sys.ODCIRidList();
    dequeue_options             DBMS_AQ.dequeue_options_t;
    message_properties          DBMS_AQ.message_properties_t;
    message_handle              RAW(16);
    message                     LUCENE.lucene_msg_typ;
    message_no_data             LUCENE.lucene_msg_typ;
    no_messages                 exception;
    end_of_fetch                exception;
    prefix                      VARCHAR2(255);
    lock_handle                 VARCHAR2(128);
    dummy                       PLS_INTEGER;
    BatchCount                  INTEGER;
    PRAGMA EXCEPTION_INIT (end_of_fetch, -25228);
    PRAGMA EXCEPTION_INIT (no_messages, -25235);
  begin
   IF (part_name IS NOT NULL) THEN
     prefix := owner || '.' || index_name || '$' || part_name;
   ELSE
     prefix := owner || '.' || index_name;
   END IF;
   BatchCount := NVL(GetParameter(prefix,'BatchCount'),32767);
   -- start the MEP (mutually exclusive part)
   dbms_lock.allocate_unique(prefix,lock_handle);
   dummy := dbms_lock.request(lock_handle,dbms_lock.x_mode,dbms_lock.maxwait,true);
   -- make sure that we have write access to OJVMDirectory storage during the process
   -- EXECUTE IMMEDIATE 'lock table '||prefix||'$T in exclusive mode';
   dequeue_options.delivery_mode := dbms_aq.persistent;
   dequeue_options.visibility    := dbms_aq.on_commit;
   dequeue_options.wait         := DBMS_AQ.NO_WAIT;
   dequeue_options.navigation   := DBMS_AQ.FIRST_MESSAGE;
   dequeue_options.dequeue_mode := DBMS_AQ.LOCKED;
   LOOP
       DBMS_AQ.DEQUEUE(
          queue_name         => prefix||'$Q',
          dequeue_options    => dequeue_options,
          message_properties => message_properties,
          payload            => message,
          msgid              => message_handle);
       if (message.operation = 'delete') then
         -- put ARRAY DML rids into deleted list
         for i in 1 .. message.ridlist.last loop
              deleted.extend;
              deleted(deleted.last) := message.ridlist(i);
         end loop;
       else 
            -- if is not delete op, must be insert/update
            -- put ARRAY DML rids into inserted list
            for i in 1 .. message.ridlist.last loop
              inserted.extend;
              inserted(inserted.last) := message.ridlist(i);
            end loop;
       end if;
       -- counter is bigger than BatchCount parameter send to Solr
       -- 1024 is max boolean clause parameter definied in solrconfig.xml file
       if (inserted.last>=BatchCount or deleted.last>=1024) then
         syncInternal(prefix,deleted,inserted);
         deleted := sys.ODCIRidList();
         inserted := sys.ODCIRidList();
       end if;
       dequeue_options.dequeue_mode := dbms_aq.REMOVE_NODATA;
       dequeue_options.msgid := message_handle;
       dequeue_options.deq_condition := '';
       dbms_aq.dequeue(
                  queue_name         => prefix||'$Q',
                  dequeue_options    => dequeue_options,
                  message_properties => message_properties,
                  payload            => message_no_data,
                  msgid              => message_handle);
       dequeue_options.dequeue_mode := DBMS_AQ.LOCKED;
       dequeue_options.msgid := NULL;
       dequeue_options.navigation := dbms_aq.NEXT_MESSAGE;
   END LOOP;
   dummy := dbms_lock.release(lock_handle);
   EXCEPTION
     WHEN no_messages OR end_of_fetch THEN
        if (deleted.count>0 OR inserted.count>0) then
          syncInternal(prefix,deleted,inserted);
          dummy := dbms_lock.release(lock_handle);
        end if;
  end sync;

  static procedure optimize(index_name VARCHAR2) is
    index_schema VARCHAR2(30);
    idx_name VARCHAR2(30) := index_name;
    is_part varchar2(3);
    par_degree number;
    v_version VARCHAR2(4000);
  begin
    select banner into v_version from v$version where rownum=1;
    SELECT OWNER,PARTITIONED,DEGREE INTO INDEX_SCHEMA,IS_PART,PAR_DEGREE FROM ALL_INDEXES WHERE INDEX_NAME=IDX_NAME;
    IF (IS_PART = 'YES' AND instr(v_version,'19c')>0) THEN
      EXECUTE IMMEDIATE 'begin run_in_parallel('''||INDEX_SCHEMA||''','''||IDX_NAME||''','||PAR_DEGREE||',''SOPTIMIZE_PARTITION''); end;';
    ELSE
      OPTIMIZE(INDEX_SCHEMA,INDEX_NAME);
    end if;
    exception when no_data_found then
      RAISE_APPLICATION_ERROR
      (-20101, 'Index not found: '||idx_name);
    when too_many_rows then
      INDEX_SCHEMA := SYS_CONTEXT('USERENV','CURRENT_SCHEMA');
      SELECT PARTITIONED,DEGREE INTO IS_PART,PAR_DEGREE FROM ALL_INDEXES WHERE INDEX_NAME=IDX_NAME and OWNER=INDEX_SCHEMA;
      IF (IS_PART = 'YES' AND instr(v_version,'19c')>0) THEN
        EXECUTE IMMEDIATE 'begin run_in_parallel('''||INDEX_SCHEMA||''','''||IDX_NAME||''','||PAR_DEGREE||',''SOPTIMIZE_PARTITION''); end;';
      ELSE
        OPTIMIZE(INDEX_SCHEMA,INDEX_NAME);
      END IF;
  end optimize;

  static procedure optimize(owner VARCHAR2, index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL) is
    prefix              VARCHAR2(255);
    lock_handle         VARCHAR2(128);
    sync_mode           VARCHAR2(128);
    dummy               PLS_INTEGER;
  begin
    IF (part_name IS NOT NULL) THEN
      prefix := owner || '.' || index_name || '$' || part_name;
    ELSE
      prefix := owner || '.' || index_name;
    END IF;
    -- check if is on-line
    sync_mode := NVL(GetParameter(prefix,'SyncMode'),'OnLine');
    if (sync_mode = 'OnLine') then
      enqueueChange(prefix,sys.ODCIRidList(),'optimize');
    else
      -- sync(owner,index_name,part_name); -- purge pending changes first
      -- start the MEP (mutually exclusive part)
      dbms_lock.allocate_unique(prefix,lock_handle);
      dummy := dbms_lock.request(lock_handle,dbms_lock.x_mode,dbms_lock.maxwait,true);
      optimizeInternal(prefix); -- then optimize
      dummy := dbms_lock.release(lock_handle);
    end if;
  end optimize;

  static procedure rebuild(index_name VARCHAR2) is
    index_schema VARCHAR2(30);
    idx_name VARCHAR2(30) := index_name;
    is_part varchar2(3);
    par_degree number;
    v_version VARCHAR2(4000);
  begin
    select banner into v_version from v$version where rownum=1;
    SELECT OWNER,PARTITIONED,DEGREE INTO INDEX_SCHEMA,IS_PART,PAR_DEGREE FROM ALL_INDEXES WHERE INDEX_NAME=IDX_NAME;
    IF (IS_PART = 'YES' AND instr(v_version,'19c')>0) THEN
      EXECUTE IMMEDIATE 'begin run_in_parallel('''||INDEX_SCHEMA||''','''||IDX_NAME||''','||PAR_DEGREE||',''SREBUILD_PARTITION''); end;';
    ELSE
      REBUILD(INDEX_SCHEMA,INDEX_NAME);
    end if;
    exception when no_data_found then
      RAISE_APPLICATION_ERROR
      (-20101, 'Index not found: '||idx_name);
    when too_many_rows then
      INDEX_SCHEMA := SYS_CONTEXT('USERENV','CURRENT_SCHEMA');
      SELECT PARTITIONED,DEGREE INTO IS_PART,PAR_DEGREE FROM ALL_INDEXES WHERE INDEX_NAME=IDX_NAME and OWNER=INDEX_SCHEMA;
      IF (IS_PART = 'YES' AND instr(v_version,'19c')>0) THEN
        EXECUTE IMMEDIATE 'begin run_in_parallel('''||INDEX_SCHEMA||''','''||IDX_NAME||''','||PAR_DEGREE||',''SREBUILD_PARTITION''); end;';
      ELSE
        REBUILD(INDEX_SCHEMA,INDEX_NAME);
      END IF;
  end rebuild;

  static procedure rebuild(owner VARCHAR2, index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL) is
    prefix              VARCHAR2(255);
    sync_mode           VARCHAR2(128);
  begin
    IF (part_name IS NOT NULL) THEN
     prefix := owner || '.' || index_name || '$' || part_name;
    ELSE
     prefix := owner || '.' || index_name;
    END IF;
    -- check if is on-line
    sync_mode := NVL(GetParameter(prefix,'SyncMode'),'OnLine');
    if (sync_mode = 'OnLine') then
      enqueueChange(prefix,sys.ODCIRidList(),'rebuild');
    else
      -- use Lucene's storage table as exclusive lock
      -- EXECUTE IMMEDIATE 'lock table '||prefix||'$T in exclusive mode';
      rebuildInternal(prefix); -- then rebuild
    end if;
  end rebuild;

  static procedure createTable(prefix VARCHAR2, lobStorageParam VARCHAR2) is
    v_version VARCHAR2(4000);
    current_schema VARCHAR2(30);
  begin
      select banner into v_version from v$version where rownum=1;
      if (instr(v_version,'10g')>0) then
        EXECUTE IMMEDIATE
'create table '||prefix||'$T (
    NAME          VARCHAR2(30) primary key,
    LAST_MODIFIED TIMESTAMP,
    FILE_SIZE     INTEGER,
    DATA          BLOB,
    DELETED       CHAR(1)
    ) LOB (DATA) STORE AS ('||lobStorageParam||')';
      else
        EXECUTE IMMEDIATE
'create table '||prefix||'$T (
    NAME          VARCHAR2(30) primary key,
    LAST_MODIFIED TIMESTAMP,
    FILE_SIZE     INTEGER,
    DATA          BLOB,
    DELETED       CHAR(1)
    ) LOB (DATA) STORE AS SECUREFILE ('||lobStorageParam||')';
      end if;
      select SYS_CONTEXT('USERENV','CURRENT_SCHEMA') into current_schema from dual;
      if current_schema != 'LUCENE' then      
        EXECUTE IMMEDIATE
'grant all on '||prefix||'$T to LUCENE';
      end if;
      if (instr(v_version,'Enterprise')>0) then
        EXECUTE IMMEDIATE
'create bitmap index '||prefix||'$DI on '||prefix||'$T (DELETED)';
      else
        EXECUTE IMMEDIATE
'create index '||prefix||'$DI on '||prefix||'$T (DELETED)';
      end if;
  end createTable;

  static procedure dropTable(prefix VARCHAR2) is
  begin
      EXECUTE IMMEDIATE 'DROP TABLE '||prefix||'$T FORCE';
  end dropTable;

  -- This function is not available on 10g
  static function ResourceExists(path varchar2)
    return boolean as
    result number;
  begin
    select count(*)
        into result
        from RESOURCE_VIEW
        where equals_path(res, path) = 1;
        if (result > 0) then
            return TRUE;
        else
            return FALSE;
        end if;
  end;

  static procedure createXdbDir(dir VARCHAR2) is
      result boolean;
  begin
    if (not ResourceExists(dir)) then
        result := dbms_xdb.createFolder(dir);
        dbms_xdb.setAcl(dir,'/sys/acls/all_owner_acl.xml');
        update resource_view
             set res = updateXml(res,'/Resource/Owner/text()',USER)
             where equals_path(res,dir) = 1;
    end if;
  end createXdbDir;

  static procedure xdbExport(index_name VARCHAR2) is
    index_schema VARCHAR2(30);
    idx_name VARCHAR2(30) := index_name;
    is_part varchar2(3);
  begin
    SELECT OWNER,PARTITIONED INTO INDEX_SCHEMA,IS_PART FROM ALL_INDEXES WHERE INDEX_NAME=IDX_NAME;
    IF (IS_PART = 'YES') THEN
      FOR P IN (SELECT PARTITION_NAME FROM ALL_IND_PARTITIONS  WHERE INDEX_OWNER=INDEX_SCHEMA AND INDEX_NAME=IDX_NAME) LOOP
         xdbExport(INDEX_SCHEMA,INDEX_NAME,P.PARTITION_NAME);
      end loop;
    ELSE
      xdbExport(INDEX_SCHEMA,INDEX_NAME);
    end if;
    exception when no_data_found then
      RAISE_APPLICATION_ERROR
      (-20101, 'Index not found: '||idx_name);
    when too_many_rows then
      INDEX_SCHEMA := SYS_CONTEXT('USERENV','CURRENT_SCHEMA');
      SELECT PARTITIONED INTO IS_PART FROM ALL_INDEXES WHERE INDEX_NAME=IDX_NAME and OWNER=INDEX_SCHEMA;
      IF (IS_PART = 'YES') THEN
        FOR P IN (SELECT PARTITION_NAME FROM ALL_IND_PARTITIONS  WHERE INDEX_OWNER=INDEX_SCHEMA AND INDEX_NAME=IDX_NAME) LOOP
           xdbExport(INDEX_SCHEMA,INDEX_NAME,P.PARTITION_NAME);
        END LOOP;
      ELSE
        xdbExport(INDEX_SCHEMA,INDEX_NAME);
      END IF;
  end xdbExport;

  static procedure xdbExport(owner VARCHAR2,index_name VARCHAR2, part_name IN VARCHAR2 DEFAULT NULL) is
    TYPE IdxCurTyp  IS REF CURSOR;
    v_idx_cursor    IdxCurTyp;
    v_stmt_str      VARCHAR2(2000);
    v_name          VARCHAR2(30);
    v_data          BLOB;
    v_size          NUMBER;
    result          BOOLEAN;
    prefix          VARCHAR2(255);
  begin
    IF (part_name IS NOT NULL) THEN
     prefix := owner || '.' || index_name || '$' || part_name;
    ELSE
     prefix := owner || '.' || index_name;
    END IF;
    -- Create Solr Admin Home Directory
    createXdbDir('/public/solr/'||prefix);
    createXdbDir('/public/solr/'||PREFIX||'/data/');
    v_stmt_str := 'SELECT name,data,file_size from '||prefix||'$T where deleted=''N'' and (NAME LIKE ''\_%'' ESCAPE ''\'' OR NAME LIKE ''segment%'')';
    OPEN v_idx_cursor FOR v_stmt_str;
    LOOP
	FETCH v_idx_cursor INTO v_name,v_data,v_size;
	EXIT WHEN v_idx_cursor%NOTFOUND;
        if (ResourceExists('/public/solr/'||prefix||'/data/'||v_name)) then
          dbms_xdb.deleteResource('/public/solr/'||prefix||'/data/'||v_name);
        end if;
        RESULT := dbms_xdb.createResource('/public/solr/'||prefix||'/data/'||v_name,v_data);
    END LOOP;
    CLOSE v_idx_cursor;
    createXdbDir('/public/solr/'||PREFIX||'/conf/');
    v_stmt_str := 'SELECT name,data,file_size from '||prefix||'$T where deleted=''N'' and not (name like ''\_%'' ESCAPE ''\'' or name like ''segment%'' or name = ''updateCount'' or name=''parameters'')';
    OPEN v_idx_cursor FOR v_stmt_str;
    LOOP
	FETCH v_idx_cursor INTO v_name,v_data,v_size;
	EXIT WHEN v_idx_cursor%NOTFOUND;
        if (ResourceExists('/public/solr/'||prefix||'/conf/'||v_name)) then
          dbms_xdb.deleteResource('/public/solr/'||prefix||'/conf/'||v_name);
        end if;
        result := dbms_xdb.createResource('/public/solr/'||prefix||'/conf/'||v_name,v_data);
    END LOOP;
    CLOSE v_idx_cursor;
  end xdbExport;

  STATIC FUNCTION countHits(index_name VARCHAR2, cmpval VARCHAR2) RETURN NUMBER is
    index_schema VARCHAR2(30);
    idx_name VARCHAR2(30) := index_name;
    hits     NUMBER := 0;
    is_part varchar2(3);
  BEGIN
    -- dbms_parallel do some overhead directly sum sequencially
    SELECT OWNER,PARTITIONED INTO INDEX_SCHEMA,IS_PART FROM ALL_INDEXES WHERE INDEX_NAME=IDX_NAME;
    IF (IS_PART = 'YES') THEN
      FOR P IN (SELECT PARTITION_NAME FROM ALL_IND_PARTITIONS  WHERE INDEX_OWNER=INDEX_SCHEMA AND INDEX_NAME=IDX_NAME) LOOP
         hits := hits + countHits(index_schema,index_name|| '$' ||p.partition_name,cmpval);
      end loop;
    ELSE
      hits := countHits(index_schema,index_name,cmpval);
    end if;
    return hits;
    exception when no_data_found then
      RAISE_APPLICATION_ERROR
      (-20101, 'Index not found: '||idx_name);
    when too_many_rows then
      INDEX_SCHEMA := SYS_CONTEXT('USERENV','CURRENT_SCHEMA');
      SELECT PARTITIONED INTO IS_PART FROM ALL_INDEXES WHERE INDEX_NAME=IDX_NAME and OWNER=INDEX_SCHEMA;
      IF (IS_PART = 'YES') THEN
        FOR P IN (SELECT PARTITION_NAME FROM ALL_IND_PARTITIONS  WHERE INDEX_OWNER=INDEX_SCHEMA AND INDEX_NAME=IDX_NAME) LOOP
           hits := hits + countHits(index_schema,index_name|| '$' ||p.partition_name,cmpval);
        END LOOP;
      ELSE
        hits := countHits(index_schema,index_name,cmpval);
      END IF;
      return hits;
  end countHits;
end;
/
show errors
