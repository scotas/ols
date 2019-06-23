
---------------------------------------------------------------------
--    LUCENE Facet implementation bdy                              --
---------------------------------------------------------------------

create or replace
type body facets_agg_type  is
  static function ODCIAggregateInitialize(sctx IN OUT NOCOPY facets_agg_type)
  return number
  is
  begin
    -- dbms_output.put_line('ODCIAggregateInitialize');
    sctx := facets_agg_type( null, null, null, null );
    return ODCIConst.Success;
  end;
  member function ODCIAggregateIterate(self IN OUT NOCOPY facets_agg_type,
                                       value IN varchar2 )
  return number
  is
     status  NUMBER;
     pos NUMBER;
     qry VARCHAR2(32767);
     cnt NUMBER;
  begin
    pos := instr(value,',');
    if self.elements is null then
      -- dbms_output.put_line('ODCIAggregateIterate elements is null');
      self.elements := agg_tbl();
      qry := substr(value,pos+1);
      self.index_name := substr(value,1,pos-1);
      pos := instr(self.index_name,'.');
      if (pos>0) then
         self.owner := substr(self.index_name,1,pos-1);
         self.index_name := substr(self.index_name,pos+1);
      else
         self.owner := sys_context('userenv','session_user');
      end if;
      status := HitCounter.ODCIInitialize(self.jctx,self.owner||'.'||self.index_name,qry);
      if (status <> ODCIConst.Success) then
         return status;
      end if;
    else
      qry := substr(value,pos+1);
    end if;
    status := HitCounter.ODCIIterate(jctx,qry,cnt);
    if (status = ODCIConst.Success) then
      self.elements.extend;
      self.elements(self.elements.count) := agg_attributes(qry,cnt);
    end if;
    return status;
  end;
  member function ODCIAggregateTerminate(self IN facets_agg_type,
                                         returnValue OUT NOCOPY agg_tbl,
                                         flags IN number)
  return number
  is
  begin
    -- dbms_output.put_line('ODCIAggregateTerminate elements.count: '||self.elements.count||' owner:'|| self.owner||' index_name:'|| self.index_name);
    returnValue := self.elements;
    return HitCounter.ODCITerminate(jctx);
  end;
  member function ODCIAggregateMerge(self IN OUT NOCOPY facets_agg_type,
                                     ctx2 IN facets_agg_type)
  return number
  is
  begin
    if ctx2.elements is not null then
      if self.elements is null then
        self.elements := agg_tbl();
      end if;
      -- dbms_output.put_line('ODCIAggregateMerge elements.count: '||self.elements.count||' elements2.count'||ctx2.elements.count);
      for i in 1 .. ctx2.elements.count loop
        self.elements.extend;
        self.elements(self.elements.count) := ctx2.elements(i);
      end loop;
    end if;
    return ODCIConst.Success;
  end;
end;
/
show errors

exit