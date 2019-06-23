-- Drops
DECLARE
  TYPE obj_arr IS TABLE OF VARCHAR2(30);
  DRP_STMT VARCHAR2(4000) := 'drop public synonym ';
  obj_list obj_arr := obj_arr('DidYouMean');
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

CREATE OR REPLACE PACKAGE didyoumean authid current_user AS
    PROCEDURE indexDictionary(
        index_name   IN VARCHAR2,
        spellColumns IN VARCHAR2 DEFAULT null,
        distancealg  IN VARCHAR2 DEFAULT 'Levenstein');

    PROCEDURE indexDictionary(
        owner        IN VARCHAR2,
        index_name   IN VARCHAR2,
        spellColumns IN VARCHAR2,
        distancealg  IN VARCHAR2) as 
           language java name
	            'com.scotas.lucene.search.spell.ojvm.SpellChecker.indexDictionary(java.lang.String, java.lang.String, java.lang.String, java.lang.String)';
 
    FUNCTION suggest
    (
        index_name  IN VARCHAR2,
        cmpval      IN VARCHAR2,
        highlight   IN VARCHAR2 DEFAULT null,
        distancealg IN VARCHAR2 DEFAULT 'Levenstein'
    ) RETURN VARCHAR2;

    FUNCTION suggestwords
    (
        owner       IN VARCHAR2,
        index_name  IN VARCHAR2,
        cmpval      IN VARCHAR2,
        highlight   IN VARCHAR2,
        distancealg IN VARCHAR2
    ) RETURN VARCHAR2 AS
        LANGUAGE JAVA name 'com.scotas.lucene.search.spell.ojvm.SpellChecker.didYouMean(
                java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String) return java.lang.String';

END didyoumean;
/
show errors

-- GRANTS
grant execute on DidYouMean to public;

create public synonym DidYouMean for lucene.DidYouMean;

exit
