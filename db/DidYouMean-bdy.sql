CREATE OR REPLACE PACKAGE BODY didyoumean AS
    PROCEDURE indexDictionary(
        index_name   IN VARCHAR2,
        spellColumns IN VARCHAR2,
        distancealg  IN VARCHAR2) IS
    index_schema VARCHAR2(30);
    idx_name     VARCHAR2(30) := index_name;
    BEGIN
        SELECT owner
          INTO index_schema
          FROM all_indexes
         WHERE index_name = idx_name;
         indexDictionary(index_schema, index_name, spellColumns, distancealg);
    EXCEPTION
        WHEN no_data_found THEN
            raise_application_error(-20101, 'Index not found: ' || idx_name);
        WHEN too_many_rows THEN
            indexDictionary(sys_context('USERENV', 'CURRENT_SCHEMA'), index_name, spellColumns, distancealg);
    END indexDictionary;

    FUNCTION suggest
    (
        index_name  IN VARCHAR2,
        cmpval      IN VARCHAR2,
        highlight   IN VARCHAR2,
        distancealg IN VARCHAR2
    ) RETURN VARCHAR2 IS
        index_schema VARCHAR2(30);
        idx_name     VARCHAR2(30) := index_name;
    BEGIN
        SELECT owner
          INTO index_schema
          FROM all_indexes
         WHERE index_name = idx_name;
        RETURN suggestwords(index_schema, index_name, cmpval, highlight, distancealg);
    EXCEPTION
        WHEN no_data_found THEN
            raise_application_error(-20101, 'Index not found: ' || idx_name);
        WHEN too_many_rows THEN
            RETURN suggestwords(sys_context('USERENV', 'CURRENT_SCHEMA'), index_name, cmpval, highlight, distancealg);
    END suggest;

END didyoumean;
/

show errors;

exit
