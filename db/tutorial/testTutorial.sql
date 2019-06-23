SELECT /*+ DOMAIN_INDEX_SORT */ id FROM OLS_TUTORIAL T where scontains(id,'video')>0
SELECT id FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'name_tg:video')>0
SELECT id,price FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'video AND price_f:[* TO 400]')>0
SELECT NAME,ID,round(sscore(1),2) sc FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'video',1)>0
SELECT /*+ DOMAIN_INDEX_SORT */ round(sscore(1),2) sc,NAME FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'adata',1)>0
SELECT /*+ DOMAIN_INDEX_SORT */ round(sscore(1),2) sc,NAME FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'"1 gigabyte"',1)>0
SELECT ID,NAME,PRICE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'video',1)>0 order by price desc
SELECT /*+ DOMAIN_INDEX_SORT */ ID,NAME,PRICE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'video','price_f desc')>0
SELECT ID,NAME,PRICE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'name_tg:"Server-mod"')>0
UPDATE OLS_TUTORIAL SET NAME = 'Solr, the Enterprise Search Server-mod' WHERE ID='SOLR1000'
SELECT ID,NAME,PRICE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'id_s:SP2514N')>0
DELETE FROM OLS_TUTORIAL WHERE ID='SP2514N'
SELECT ID,NAME,PRICE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'name_tg:DDR')>0
DELETE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'name_tg:DDR')>0
SELECT /*+ DOMAIN_INDEX_SORT */ ID,NAME,PRICE FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video','price_f desc')>0
SELECT /*+ DOMAIN_INDEX_SORT */ ID,NAME,PRICE FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video','inStock_b asc,price_f desc')>0
SELECT /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(OLS_TUTORIAL TUTORIAL_SIDX) */ ID,NAME,PRICE FROM OLS_TUTORIAL T WHERE scontains(id,'video')>0 order by instock,price desc
SELECT /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(OLS_TUTORIAL TUTORIAL_SIDX) */ id,inStock,price FROM OLS_TUTORIAL WHERE scontains(id,'price_f:[15 TO 100]','inStock_b asc,price_f desc')>0
select /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(OLS_TUTORIAL TUTORIAL_SIDX) */ id,inStock,price from OLS_TUTORIAL WHERE scontains(id,'*:*')>0 and price between 15 and 100 order by inStock,price desc
SELECT /*+ DOMAIN_INDEX_SORT */ ID,NAME,PRICE,INSTOCK FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video','inStock_b asc,price_f desc')>0
SELECT /*+ DOMAIN_INDEX_SORT */ round(sscore(1),2) sc,ID,NAME FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video',1)>0
SELECT /*+ DOMAIN_INDEX_SORT */ round(sscore(1),2) sc,ID,NAME,PRICE,INSTOCK FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video','inStock_b asc,score desc',1)>0
SELECT /*+ DOMAIN_INDEX_SORT */ round(POPULARITY/(PRICE+1),2) PP,NAME FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video','div(popularity_i,add(price_f,1)) desc',1)>0
SELECT nvl(shighlight(1),name) hl,id FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video card',1)>0
select * from table(SELECT T.FACETS F FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.field=cat_tw')) T)
select * from table(SELECT T.FACETS F FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','name_tg:video','facet.field=cat_tw&facet.mincount=1')) T)
SELECT FIELD,SJOIN(T.FACETS) F FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.field=cat_tw&facet.field=inStock_b')) T
select * from table(SELECT T.QUERIES FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.query=price_f:[0+TO+100]&facet.query=price_f:[100+TO+*]')) T)
select * from table(SELECT T.RANGES FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.range=price_f&facet.range.start=0&facet.range.end=1000&facet.range.gap=100')) T)
select * from table(SELECT T.DATES FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.date=manufacturedate_dt&facet.date.start=2004-01-01T00:00:00Z&facet.date.end=2010-01-01T00:00:00Z&facet.date.gap=%2B1YEAR')) T)
SELECT FIELD,SJOIN(T.FACETS) F,SJOIN(T.DATES) D FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.field=cat_tw&facet.field=inStock_b&facet.date=manufacturedate_dt&facet.date.start=2004-01-01T00:00:00Z&facet.date.end=2010-01-01T00:00:00Z&facet.date.gap=%2B1YEAR')) T
SELECT /*+ DOMAIN_INDEX_SORT */ SSCORE(1),name FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'power-shot',1)>0
SELECT /*+ DOMAIN_INDEX_SORT */ SSCORE(1),NAME,features FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'features:recharging',1)>0
SELECT /*+ DOMAIN_INDEX_SORT */ SSCORE(1),NAME FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'pixima',1)>0
select name from ols_tutorial where rowid in (select column_value from table(SELECT smlt(1) FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'pixima',1)>0 and rownum=1))
SELECT /*+ DOMAIN_INDEX_SORT */ id,round(SSCORE(1),2) sc FROM OLS_TUTORIAL T where scontains(id,'video','geodist(location_pn,0.0,0.0) asc',1)>0
