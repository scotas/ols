rem Compiled valid classes under scotas schema in batch to reduce memory usage
declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'com/scotas/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

