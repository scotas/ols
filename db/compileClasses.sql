rem Compiled valid classes under lucene schema in batch to reduce memory usage
declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/analysis/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/document/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/index/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/indexer/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/misc/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/queryParser/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/search/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/store/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/util/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/wikipedia/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/commons/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/tartarus/snowball/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'junit/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'Acme/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'net/sf/retrotranslator/runtime/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

declare
  res NUMBER;
begin
  for c in (SELECT dbms_java.longname(object_name) cName
            FROM user_objects
            WHERE object_type = 'JAVA CLASS'
            AND dbms_java.longname(object_name) like 'org/apache/lucene/%') loop
    begin
       res := dbms_java.compile_class(c.cName);
    exception when others then
       null;
    end;
  end loop;
end;
/

