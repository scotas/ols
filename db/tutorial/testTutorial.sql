-- Simple search
SELECT /*+ DOMAIN_INDEX_SORT */ id FROM OLS_TUTORIAL T where scontains(id,'video')>0
-- Field specific search
SELECT id FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'name_tg:video')>0
-- simple and range search combined
SELECT id,price FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'video AND price_f:[* TO 400]')>0
-- sscore example
SELECT NAME,ID,round(sscore(1),2) sc FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'video',1)>0
-- search adata matches A-DATA
SELECT /*+ DOMAIN_INDEX_SORT */ round(sscore(1),2) sc,NAME FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'adata',1)>0
-- search 1 gigabyte matches things with GB
SELECT /*+ DOMAIN_INDEX_SORT */ round(sscore(1),2) sc,NAME FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'"1 gigabyte"',1)>0
-- SQL Sort - Optimizer Cost 4
SELECT ID,NAME,PRICE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'video',1)>0 order by price desc
-- Domain Index sort, Optimizer Cost 3
SELECT /*+ DOMAIN_INDEX_SORT */ ID,NAME,PRICE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'video','price_f desc')>0
-- pre-update test
SELECT ID,NAME,PRICE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'name_tg:"Server-mod"')>0
-- test update, wait a few second for near real-time sort
UPDATE OLS_TUTORIAL SET NAME = 'Solr, the Enterprise Search Server-mod' WHERE ID='SOLR1000'
-- pre-delete test
SELECT ID,NAME,PRICE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'id_s:SP2514N')>0
-- delete by ID test
DELETE FROM OLS_TUTORIAL WHERE ID='SP2514N'
-- pre-delete test using scontains
SELECT ID,NAME,PRICE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'name_tg:DDR')>0
-- delete by Query test
DELETE FROM OLS_TUTORIAL WHERE SCONTAINS(ID,'name_tg:DDR')>0
-- Sort query example DESC
SELECT /*+ DOMAIN_INDEX_SORT */ ID,NAME,PRICE FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video','price_f desc')>0
-- Sort query example ASC/DESC
SELECT /*+ DOMAIN_INDEX_SORT */ ID,NAME,PRICE FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video','inStock_b asc,price_f desc')>0
-- Sort query example domain index filter, order by
SELECT /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(OLS_TUTORIAL TUTORIAL_SIDX) */ ID,NAME,PRICE FROM OLS_TUTORIAL T WHERE scontains(id,'video')>0 order by instock,price desc
-- domain index filter, order by, no push predicates
SELECT /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(OLS_TUTORIAL TUTORIAL_SIDX) */ id,inStock,price FROM OLS_TUTORIAL WHERE scontains(id,'price_f:[15 TO 100]','inStock_b asc,price_f desc')>0
-- domain index filter, order by, push predicates
SELECT /*+ DOMAIN_INDEX_SORT DOMAIN_INDEX_FILTER(OLS_TUTORIAL TUTORIAL_SIDX) */ id,inStock,price FROM OLS_TUTORIAL WHERE scontains(id,'*:*')>0 and price between 15 and 100 order by inStock,price desc
-- Sort query example multiple columns
SELECT /*+ DOMAIN_INDEX_SORT */ ID,NAME,PRICE,INSTOCK FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video','inStock_b asc,price_f desc')>0
-- score desc natural sort for domain index
SELECT /*+ DOMAIN_INDEX_SORT */ round(sscore(1),2) sc,ID,NAME FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video',1)>0
-- score can also be used as a field name when specifying a sort
SELECT /*+ DOMAIN_INDEX_SORT */ round(sscore(1),2) sc,ID,NAME,PRICE,INSTOCK FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video','inStock_b asc,score desc',1)>0
-- Complex functions used to sort results
SELECT /*+ DOMAIN_INDEX_SORT */ round(POPULARITY/(PRICE+1),2) PP,NAME FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video','div(popularity_i,add(price_f,1)) desc',1)>0
-- Highlighting example
SELECT nvl(shighlight(1),name) hl,id FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'video card',1)>0
-- Facets field, simple example
SELECT * from table(SELECT T.FACETS F FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.field=cat_tw')) T)
-- Facets field with query, simple example
SELECT * from table(SELECT T.FACETS F FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','name_tg:video','facet.field=cat_tw&facet.mincount=1')) T)
-- Facets field multiple example
SELECT FIELD,SJOIN(T.FACETS) F FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.field=cat_tw&facet.field=inStock_b')) T
-- Facets query example with ranges
SELECT * from table(SELECT T.QUERIES FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.query=price_f:[0+TO+100]&facet.query=price_f:[100+TO+*]')) T)
-- Facets ranges example
SELECT * from table(SELECT T.RANGES FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.range=price_f&facet.range.start=0&facet.range.end=1000&facet.range.gap=100')) T)
-- Facets dates example
SELECT * from table(SELECT T.DATES FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.date=manufacturedate_dt&facet.date.start=2004-01-01T00:00:00Z&facet.date.end=2010-01-01T00:00:00Z&facet.date.gap=%2B1YEAR')) T)
-- Facets dates and field example combined
SELECT FIELD,SJOIN(T.FACETS) F,SJOIN(T.DATES) D FROM TABLE(SFACETS(USER||'.TUTORIAL_SIDX','*:*','facet.field=cat_tw&facet.field=inStock_b&facet.date=manufacturedate_dt&facet.date.start=2004-01-01T00:00:00Z&facet.date.end=2010-01-01T00:00:00Z&facet.date.gap=%2B1YEAR')) T
-- Text analysis, search power-shot matches PowerShot
SELECT /*+ DOMAIN_INDEX_SORT */ SSCORE(1),name FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'power-shot',1)>0
-- search features:recharging matches Rechargeable due to stemming with the EnglishPorterFilter
SELECT /*+ DOMAIN_INDEX_SORT */ SSCORE(1),NAME,features FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'features:recharging',1)>0
-- search misspelled pixima matches Pixma due to use of a SynonymFilter
SELECT /*+ DOMAIN_INDEX_SORT */ SSCORE(1),NAME FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'pixima',1)>0
-- More like this example
SELECT name FROM OLS_TUTORIAL WHERE rowid IN (SELECT column_value FROM TABLE(SELECT smlt(1) FROM OLS_TUTORIAL T WHERE SCONTAINS(ID,'pixima',1)>0 and rownum=1))
-- Geo-localization sort
SELECT /*+ DOMAIN_INDEX_SORT */ id,round(SSCORE(1),2) sc FROM OLS_TUTORIAL T where scontains(id,'video','geodist(location_pn,0.0,0.0) asc',1)>0
-- Cleanup
DROP TABLE OLS_TUTORIAL
DROP TYPE CAT_LIST_ARR