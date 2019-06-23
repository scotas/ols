rem Compiled valid classes under retrotranslator schema in batch to reduce memory usage
declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS' AND status = 'VALID'
            AND dbms_java.longname(object_name) like 'net/sf/retrotranslator/%') loop
    res := dbms_java.compile_class(c.cName);
  end loop;
end;
/

