conn sys/change_on_install@orcl as sysdba

alter user oe identified by oe account unlock;

conn oe/oe@orcl

create index PurchaseOrder_idx on PURCHASEORDER p (value(p)) indextype is ctxsys.context;

select count(*) from PURCHASEORDER p where contains(value(p),'Madonna')>0;

select extract(p.object_value,'/PurchaseOrder/Reference').getStringVal()
from PURCHASEORDER p,resource_view r
where extractValue(r.res,'/Resource/XMLRef')=ref(p) and
under_path(r.res,'/home/OE/PurchaseOrders/2002/Apr')>0
and contains(p.object_value,'Madonna')>0;

create index PurchaseOrder_lidx on PURCHASEORDER p (value(p)) indextype is lucene.LuceneIndex parameters('Analyzer:org.apache.lucene.analysis.en.EnglishAnalyzer');

select count(*) from PURCHASEORDER p where lcontains(value(p),'Madonna')>0;

select extract(p.object_value,'/PurchaseOrder/Reference').getStringVal()
from PURCHASEORDER p,resource_view r
where extractValue(r.res,'/Resource/XMLRef')=ref(p) and
under_path(r.res,'/home/OE/PurchaseOrders/2002/Apr')>0
and lcontains(p.object_value,'Madonna')>0;

begin
  LuceneDomainIndex.optimize('OE.PURCHASEORDER_LIDX');
end;
/

select /*+ FIRST_ROWS(15) */ ntop_pos from (select rownum as ntop_pos,q.* from
(select /*+ FIRST_ROWS(10) */ * from PURCHASEORDER p where lcontains(value(p),'Madonna')>0) q)
where ntop_pos>=0 and ntop_pos<15;

select /*+ FIRST_ROWS(15) */ ntop_pos from (select rownum as ntop_pos,q.* from
(select /*+ FIRST_ROWS(10) */ * from PURCHASEORDER p where contains(value(p),'Madonna')>0) q)
where ntop_pos>=0 and ntop_pos<15;

-- XDBUri example
CREATE TABLE uri_tab ("order_id" NUMBER, "url" URIType);

INSERT INTO uri_tab VALUES
       (2354,DBURIType.createURI('/OE/ORDERS/ROW[ORDER_ID=2354]'));
INSERT INTO uri_tab VALUES
       (2355,DBURIType.createURI('/OE/ORDERS/ROW[ORDER_ID=2355]'));
INSERT INTO uri_tab VALUES
       (2356,DBURIType.createURI('/OE/ORDERS/ROW[ORDER_ID=2356]'));
INSERT INTO uri_tab VALUES
       (2357,DBURIType.createURI('/OE/ORDERS/ROW[ORDER_ID=2357]'));
INSERT INTO uri_tab VALUES
       (2358,DBURIType.createURI('/OE/ORDERS/ROW[ORDER_ID=2358]'));
INSERT INTO uri_tab VALUES
       (2359,DBURIType.createURI('/OE/ORDERS/ROW[ORDER_ID=2359]'));
INSERT INTO uri_tab VALUES
       (2360,DBURIType.createURI('/OE/ORDERS/ROW[ORDER_ID=2360]'));
INSERT INTO uri_tab VALUES
       (2361,DBURIType.createURI('/OE/ORDERS/ROW[ORDER_ID=2361]'));
INSERT INTO uri_tab VALUES
       (2362,DBURIType.createURI('/OE/ORDERS/ROW[ORDER_ID=2362]'));
INSERT INTO uri_tab VALUES
       (2363,DBURIType.createURI('/OE/ORDERS/ROW[ORDER_ID=2363]'));
commit;

SELECT "url".getXML() FROM uri_tab;

create index uri_tab_lidx on uri_tab("order_id") indextype is lucene.LuceneIndex 
parameters('Analyzer:org.apache.lucene.analysis.core.StopAnalyzer;MergeFactor:500;ExtraCols:URI_TAB.url.getXML() "order";FormatCols:order_id(0000)');

select e.url.getCLOB() FROM uri_tab e where lcontains(e.url,'online')>0;