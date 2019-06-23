rem Compiled valid classes under lucene schema in batch to reduce memory usage
declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS' AND status = 'VALID'
            AND dbms_java.longname(object_name) like 'oracle/ODCI/%') loop
    res := dbms_java.compile_class(c.cName);
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS' AND status = 'VALID'
            AND dbms_java.longname(object_name) like 'oracle/xdb/%') loop
    res := dbms_java.compile_class(c.cName);
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS' AND status = 'VALID'
            AND dbms_java.longname(object_name) like 'oracle/jdbc/%') loop
    res := dbms_java.compile_class(c.cName);
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS' AND status = 'VALID'
            AND dbms_java.longname(object_name) like 'oracle/sql/%') loop
    res := dbms_java.compile_class(c.cName);
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS' AND status = 'VALID'
            AND dbms_java.longname(object_name) like 'oracle/jpub/%') loop
    res := dbms_java.compile_class(c.cName);
  end loop;
end;
/
