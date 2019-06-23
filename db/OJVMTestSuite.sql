rem Run JUnit suites into the OJVM
set define ?
define lucene_version=?1;
drop table lucene_regresion_tests;

create table lucene_regresion_tests (
   method_or_class    CHAR(1),
   class_name         VARCHAR2(400),
   elapsed_time       NUMBER(6,3),
   result_status      VARCHAR2(32),
   result_count       NUMBER,
   failure_count      NUMBER,
   error_count        NUMBER,
   error_msg          CLOB
   )
/

select dbms_java.set_property('lucene.version','?lucene_version') from dual;
commit;

insert into lucene_regresion_tests values ('c','org.apache.lucene.store.TestWindowsMMap',0.0,'-',0,0,0,'');                
insert into lucene_regresion_tests values ('c','org.apache.lucene.store.TestFileSwitchDirectory',0.0,'-',0,0,0,'');        
-- requires tempDir permission
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.store.TestDirectory',0.0,'-',0,0,0,'');                  
insert into lucene_regresion_tests values ('c','org.apache.lucene.store.TestBufferedIndexInput',0.0,'-',0,0,0,'');         
-- requires file access: java.io.File.<init>(File.java)
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.store.TestLockFactory',0.0,'-',0,0,0,'');                
insert into lucene_regresion_tests values ('c','org.apache.lucene.store.TestRAMDirectory',0.0,'-',0,0,0,'');               
insert into lucene_regresion_tests values ('c','org.apache.lucene.store.TestHugeRamFile',0.0,'-',0,0,0,'');                
insert into lucene_regresion_tests values ('c','org.apache.lucene.store.TestLock',0.0,'-',0,0,0,'');                       

insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestComplexExplanations',0.0,'-',0,0,0,'');       
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestCustomSearcherSort',0.0,'-',0,0,0,'');        
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestSloppyPhraseQuery',0.0,'-',0,0,0,'');         
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestMultiSearcherRanking',0.0,'-',0,0,0,'');      
-- TODO: oracle.aurora.zephyr.util.JITCompileException: class java.util.ConcurrentModificationException
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestStressSort',0.0,'-',0,0,0,'');                
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestTimeLimitingCollector',0.0,'-',0,0,0,'');     
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestPositiveScoresOnlyCollector',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestNot',0.0,'-',0,0,0,'');                        
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestMatchAllDocsQuery',0.0,'-',0,0,0,'');          
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestTopDocsCollector',0.0,'-',0,0,0,'');           
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestCachingWrapperFilter',0.0,'-',0,0,0,'');       
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestMultiTermConstantScore',0.0,'-',0,0,0,'');     
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestSearchHitsWithDeletions',0.0,'-',0,0,0,'');    
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestNumericRangeQuery32',0.0,'-',0,0,0,'');        
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestDocIdSet',0.0,'-',0,0,0,'');                   
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestPhraseQuery',0.0,'-',0,0,0,'');                
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestTopScoreDocCollector',0.0,'-',0,0,0,'');       
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestSimilarity',0.0,'-',0,0,0,'');                 
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestDateFilter',0.0,'-',0,0,0,''); 
-- seem to be this JDK bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5005426
-- console output junit.framework.AssertionFailedError: java.lang.IllegalStateException: Current state = FLUSHED, new state = FLUSHED
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestThreadSafe',0.0,'-',0,0,0,'');                 
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestBooleanMinShouldMatch',0.0,'-',0,0,0,'');      
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestTimeLimitedCollector',0.0,'-',0,0,0,'');       
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestNumericRangeQuery64',0.0,'-',0,0,0,'');        
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestBoolean2',0.0,'-',0,0,0,'');                   
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestMultiValuedNumericRangeQuery',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestParallelMultiSearcher',0.0,'-',0,0,0,'');       
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestScorerPerf',0.0,'-',0,0,0,'');                  
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestSpanQueryFilter',0.0,'-',0,0,0,'');             
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestSetNorm',0.0,'-',0,0,0,'');                     
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestFieldCacheRangeFilter',0.0,'-',0,0,0,'');       
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestPrefixFilter',0.0,'-',0,0,0,'');                
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestComplexExplanationsOfNonMatches',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestBooleanPrefixQuery',0.0,'-',0,0,0,'');             
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestDisjunctionMaxQuery',0.0,'-',0,0,0,'');            
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestFilteredQuery',0.0,'-',0,0,0,'');                  

insert into lucene_regresion_tests values ('c','org.apache.lucene.search.payloads.TestBoostingTermQuery',0.0,'-',0,0,0,'');     
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.payloads.TestPayloadNearQuery',0.0,'-',0,0,0,'');      
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.payloads.TestPayloadTermQuery',0.0,'-',0,0,0,'');      

insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestMultiSearcher',0.0,'-',0,0,0,'');                  
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestSimpleExplanationsOfNonMatches',0.0,'-',0,0,0,''); 
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestMultiPhraseQuery',0.0,'-',0,0,0,'');               
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestQueryTermVector',0.0,'-',0,0,0,'');                
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestBooleanOr',0.0,'-',0,0,0,'');                      
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestTermRangeFilter',0.0,'-',0,0,0,'');                
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestTermRangeQuery',0.0,'-',0,0,0,'');                 
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestBooleanQuery',0.0,'-',0,0,0,'');                   
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestElevationComparator',0.0,'-',0,0,0,'');            
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestDateSort',0.0,'-',0,0,0,'');                       
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestBooleanScorer',0.0,'-',0,0,0,'');                  
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestScoreCachingWrappingScorer',0.0,'-',0,0,0,'');     
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestFuzzyQuery',0.0,'-',0,0,0,'');                     
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestPrefixQuery',0.0,'-',0,0,0,'');                    
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestSimpleExplanations',0.0,'-',0,0,0,'');             
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestMultiThreadTermVectors',0.0,'-',0,0,0,'');         
-- TODO: oracle.aurora.zephyr.util.JITCompileException: class java.util.ConcurrentModificationException
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestSort',0.0,'-',0,0,0,'');                           
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestQueryWrapperFilter',0.0,'-',0,0,0,'');             
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestExplanations',0.0,'-',0,0,0,'');                   
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestFieldCacheTermsFilter',0.0,'-',0,0,0,'');          
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestPositionIncrement',0.0,'-',0,0,0,'');              
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestPhrasePrefixQuery',0.0,'-',0,0,0,'');              
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestTermVectors',0.0,'-',0,0,0,'');                    

insert into lucene_regresion_tests values ('c','org.apache.lucene.search.function.TestFieldScoreQuery',0.0,'-',0,0,0,'');       
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.function.TestDocValues',0.0,'-',0,0,0,'');             
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.function.TestOrdValues',0.0,'-',0,0,0,'');             
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.function.TestCustomScoreQuery',0.0,'-',0,0,0,'');      
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.function.TestValueSource',0.0,'-',0,0,0,'');           

insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestFilteredSearch',0.0,'-',0,0,0,'');                 
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestFieldCache',0.0,'-',0,0,0,'');                     
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestWildcard',0.0,'-',0,0,0,'');                       
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestTermScorer',0.0,'-',0,0,0,'');                     
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.TestDocBoost',0.0,'-',0,0,0,'');                       

insert into lucene_regresion_tests values ('c','org.apache.lucene.search.spans.TestNearSpansOrdered',0.0,'-',0,0,0,'');         
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.spans.TestSpansAdvanced2',0.0,'-',0,0,0,'');           
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.spans.TestSpansAdvanced',0.0,'-',0,0,0,'');            
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.spans.TestSpans',0.0,'-',0,0,0,'');                    
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.spans.TestPayloadSpans',0.0,'-',0,0,0,'');             
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.spans.TestSpanExplanations',0.0,'-',0,0,0,'');         
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.spans.TestFieldMaskingSpanQuery',0.0,'-',0,0,0,'');    
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.spans.TestSpanExplanationsOfNonMatches',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.search.spans.TestBasics',0.0,'-',0,0,0,'');                      

insert into lucene_regresion_tests values ('c','org.apache.lucene.TestDemo',0.0,'-',0,0,0,'');                                     
insert into lucene_regresion_tests values ('c','org.apache.lucene.TestSearch',0.0,'-',0,0,0,'');                                   

insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestSortedVIntList',0.0,'-',0,0,0,'');                      
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestBitVector',0.0,'-',0,0,0,'');                           
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestAttributeSource',0.0,'-',0,0,0,'');                     
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestOpenBitSet',0.0,'-',0,0,0,'');                          

insert into lucene_regresion_tests values ('c','org.apache.lucene.util.cache.TestSimpleLRUCache',0.0,'-',0,0,0,'');                

insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestFieldCacheSanityChecker',0.0,'-',0,0,0,'');             
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestNumericUtils',0.0,'-',0,0,0,'');                        
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestStringIntern',0.0,'-',0,0,0,'');                        
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestRamUsageEstimator',0.0,'-',0,0,0,'');                   
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestIndexableBinaryStringTools',0.0,'-',0,0,0,'');          
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestStringHelper',0.0,'-',0,0,0,'');                        
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestPriorityQueue',0.0,'-',0,0,0,'');                       
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestCloseableThreadLocal',0.0,'-',0,0,0,'');                
insert into lucene_regresion_tests values ('c','org.apache.lucene.util.TestSmallFloat',0.0,'-',0,0,0,'');                          

insert into lucene_regresion_tests values ('c','org.apache.lucene.messages.TestNLS',0.0,'-',0,0,0,'');                             

insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestTeeSinkTokenFilter',0.0,'-',0,0,0,'');              
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestCharFilter',0.0,'-',0,0,0,'');                      
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestKeywordAnalyzer',0.0,'-',0,0,0,'');                 
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestCachingTokenFilter',0.0,'-',0,0,0,'');              
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestToken',0.0,'-',0,0,0,'');                           

insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.tokenattributes.TestSimpleAttributeImpls',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.tokenattributes.TestTermAttributeImpl',0.0,'-',0,0,0,'');   

insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestASCIIFoldingFilter',0.0,'-',0,0,0,'');                  
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestStopFilter',0.0,'-',0,0,0,'');                          
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestPerFieldAnalzyerWrapper',0.0,'-',0,0,0,'');             
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestNumericTokenStream',0.0,'-',0,0,0,'');                  
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestStopAnalyzer',0.0,'-',0,0,0,'');                        
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestStandardAnalyzer',0.0,'-',0,0,0,'');                    
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestAnalyzers',0.0,'-',0,0,0,'');                           
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestCharArraySet',0.0,'-',0,0,0,'');                        
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestLengthFilter',0.0,'-',0,0,0,'');                        
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestISOLatin1AccentFilter',0.0,'-',0,0,0,'');               
insert into lucene_regresion_tests values ('c','org.apache.lucene.analysis.TestMappingCharFilter',0.0,'-',0,0,0,'');                   

insert into lucene_regresion_tests values ('c','org.apache.lucene.TestMergeSchedulerExternal',0.0,'-',0,0,0,'');                       

-- TODO: oracle.aurora.zephyr.util.JITCompileException: class java.util.ConcurrentModificationException
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexWriterMerging',0.0,'-',0,0,0,'');                     
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestWordlistLoader',0.0,'-',0,0,0,'');                         
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexReaderClone',0.0,'-',0,0,0,'');                       
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestSegmentTermDocs',0.0,'-',0,0,0,'');
-- TODO: oracle.aurora.zephyr.util.JITCompileException: class java.util.ConcurrentModificationException
-- Deadlock
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestAtomicUpdate',0.0,'-',0,0,0,'');                           
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestFieldsReader',0.0,'-',0,0,0,'');                           
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestMultiLevelSkipList',0.0,'-',0,0,0,'');                     
-- TODO: oracle.aurora.zephyr.util.JITCompileException: class java.util.ConcurrentModificationException
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestCrash',0.0,'-',0,0,0,'');                                  
-- TODO: oracle.aurora.zephyr.util.JITCompileException: class java.util.ConcurrentModificationException
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestLazyProxSkipping',0.0,'-',0,0,0,'');                       
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestFieldInfos',0.0,'-',0,0,0,'');                             
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestCheckIndex',0.0,'-',0,0,0,'');                             
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestDocumentWriter',0.0,'-',0,0,0,'');                         
-- TODO: oracle.aurora.zephyr.util.JITCompileException: class java.util.ConcurrentModificationException
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexWriterMergePolicy',0.0,'-',0,0,0,'');                 
-- requires tempDir
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestThreadedOptimize',0.0,'-',0,0,0,'');                       
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestLazyBug',0.0,'-',0,0,0,'');                                
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestAddIndexesNoOptimize',0.0,'-',0,0,0,'');                   
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestParallelTermEnum',0.0,'-',0,0,0,'');                       
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestParallelReaderEmptyIndex',0.0,'-',0,0,0,'');               
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestSegmentReader',0.0,'-',0,0,0,'');                          
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestDirectoryReader',0.0,'-',0,0,0,'');                        
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestSegmentMerger',0.0,'-',0,0,0,'');                          
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexFileDeleter',0.0,'-',0,0,0,'');                       
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexWriterLockRelease',0.0,'-',0,0,0,'');                 
-- requires tmpdir
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexReader',0.0,'-',0,0,0,'');                            
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexWriterDelete',0.0,'-',0,0,0,'');                      
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestByteSlices',0.0,'-',0,0,0,'');                             
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestStressIndexing2',0.0,'-',0,0,0,'');                        
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestTerm',0.0,'-',0,0,0,'');                                   
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestTransactionRollback',0.0,'-',0,0,0,'');                    
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestSegmentTermEnum',0.0,'-',0,0,0,'');                        
-- seem to be this JDK bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5005426
-- console output:
--   junit.framework.AssertionFailedError: java.lang.IllegalStateException: Current state = FLUSHED, new state = FLUSHED
--   at java.nio.charset.CharsetDecoder.throwIllegalStateException(CharsetDecoder.java)
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestTransactions',0.0,'-',0,0,0,'');                           
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexReaderCloneNorms',0.0,'-',0,0,0,'');                  
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestMultiReader',0.0,'-',0,0,0,'');                            
-- seem to be this JDK bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5005426
-- console output:
--   junit.framework.AssertionFailedError: java.lang.IllegalStateException: Current state = FLUSHED, new state = FLUSHED
--   at java.nio.charset.CharsetDecoder.throwIllegalStateException(CharsetDecoder.java)
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexReaderReopen',0.0,'-',0,0,0,'');                      
-- required zips file open
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestBackwardsCompatibility',0.0,'-',0,0,0,'');                 
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestCompoundFile',0.0,'-',0,0,0,'');                           
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestParallelReader',0.0,'-',0,0,0,'');                         
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestNorms',0.0,'-',0,0,0,'');                                  
-- TODO: ConcurrentMergeScheduler dependecy
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestConcurrentMergeScheduler',0.0,'-',0,0,0,'');               
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestTermdocPerf',0.0,'-',0,0,0,'');                            
-- TODO: ConcurrentMergeScheduler dependecy
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexWriterReader',0.0,'-',0,0,0,'');                      
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestDeletionPolicy',0.0,'-',0,0,0,'');                         
-- this class was not loaded
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestDoc',0.0,'-',0,0,0,'');                                    
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestTermVectorsReader',0.0,'-',0,0,0,'');                      
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestNRTReaderWithThreads',0.0,'-',0,0,0,'');                   
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexWriterExceptions',0.0,'-',0,0,0,'');                  
-- TODO: ConcurrentMergeScheduler dependecy
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestOmitTf',0.0,'-',0,0,0,'');                                 
-- TODO: check this test suite it shouldn't fail ConcurrentMergeScheduler do not throw exceptions in some test, 10g/11g
-- testExceptionDocumentsWriterInit, testExceptionDocumentsWriterInit(TestIndexWriter.java:3151)
-- testExceptionOnMergeInit, testExceptionOnMergeInit(TestIndexWriter.java:3227)
-- testRollbackExceptionHang, testRollbackExceptionHang(TestIndexWriter.java:3993)
-- oracle.aurora.zephyr.util.JITCompileException: class java.util.ConcurrentModificationException:null
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexWriter',0.0,'-',0,0,0,'');                            
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestPayloads',0.0,'-',0,0,0,'');                               
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestIndexInput',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestFilterIndexReader',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestPositionBasedTermVectorMapper',0.0,'-',0,0,0,'');
-- requires dir access to lucene.test.stress
-- insert into lucene_regresion_tests values ('c','org.apache.lucene.index.TestStressIndexing',0.0,'-',0,0,0,'');

insert into lucene_regresion_tests values ('c','org.apache.lucene.queryParser.TestMultiAnalyzer',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.queryParser.TestMultiFieldQueryParser',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.queryParser.TestQueryParser',0.0,'-',0,0,0,'');

insert into lucene_regresion_tests values ('c','org.apache.lucene.TestSearchForDuplicates',0.0,'-',0,0,0,'');

insert into lucene_regresion_tests values ('c','org.apache.lucene.document.TestNumberTools',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.document.TestDocument',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.document.TestBinaryDocument',0.0,'-',0,0,0,'');
insert into lucene_regresion_tests values ('c','org.apache.lucene.document.TestDateTools',0.0,'-',0,0,0,'');
commit;

declare
  result XMLType;
begin
  for c in (select * from lucene_regresion_tests 
             where class_name like 'org.apache.lucene.%'
             order by class_name) loop
    dbms_output.put_line('Testing class: '||c.class_name);
    result := XMLType(OJVMRunnerAsString(c.method_or_class,c.class_name));
    commit;
    update lucene_regresion_tests 
      set elapsed_time = result.extract('/junitreport/time/text()').getNumberVal(), 
          result_status = result.extract('/junitreport/result/@status').getStringVal(), 
          result_count = result.extract('/junitreport/result/@run_count').getNumberVal(),
          failure_count = result.extract('/junitreport/result/failures/@failure_count').getNumberVal(),
          error_count = result.extract('/junitreport/result/failures/@error_count').getNumberVal(),
          error_msg = result.extract('/*').getClobVal()
      where class_name = c.class_name;
    commit;
  end loop;
end;
/

select * from lucene_regresion_tests order by class_name;

