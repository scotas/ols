---------------------------------------------------------------------
--    LUCENE Domain Index Admin Methods                            --
---------------------------------------------------------------------
create or replace package body LuceneDomainAdm as
  procedure createQueue(prefix VARCHAR2) is
  begin
    DBMS_AQADM.CREATE_QUEUE_TABLE(queue_table        => prefix||'$QT',
                                  queue_payload_type => 'LUCENE.lucene_msg_typ',
                                  sort_list          => 'ENQ_TIME',
                                  message_grouping   => DBMS_AQADM.NONE,
                                  compatible         => '10.2',
                                  multiple_consumers => FALSE);
    DBMS_AQADM.CREATE_QUEUE(queue_name         => prefix||'$Q',
                            queue_table        => prefix||'$QT',
                            queue_type         => DBMS_AQADM.NORMAL_QUEUE,
                            comment            => 'Lucene Domain Index normal queue');
    DBMS_AQADM.START_QUEUE(queue_name          => prefix||'$Q');
  end createQueue;

  procedure dropQueue(prefix VARCHAR2) is
  begin
    begin
        LuceneDomainIndex.disableCallBack (prefix);
    exception when others then
        null;
    end;
    begin
        DBMS_AQADM.STOP_QUEUE (queue_name         => prefix||'$Q');
    exception when others then
        null;
    end;
    -- drop the table with force will automatically drop the queue;
    begin
        DBMS_AQADM.DROP_QUEUE_TABLE (queue_table  => prefix||'$QT', force=> true);
    exception when others then
        null;
    end;
  end dropQueue;

  procedure purgueQueue(prefix VARCHAR2) is
    po dbms_aqadm.aq$_purge_options_t;
  begin
   po.block := FALSE;
   DBMS_AQADM.PURGE_QUEUE_TABLE(
     queue_table     => prefix||'$QT',
     purge_condition => NULL,
     purge_options   => po);
  end purgueQueue;
end;
/
show errors

exit
