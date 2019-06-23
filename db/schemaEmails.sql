create table emails (
 emailFrom VARCHAR2(256),
 emailTo VARCHAR2(256),
 subject VARCHAR2(4000),
 emailDate DATE,
 bodyText CLOB)
;

-- Required types to test RHighlight pipeline function
CREATE TYPE EMAILR AS OBJECT
(
 sc NUMBER,
 emailFrom VARCHAR2(256),
 emailTo VARCHAR2(256),
 subject VARCHAR2(4000),
 emailDate DATE,
 bodyText CLOB
)
;

CREATE OR REPLACE TYPE EMAILRSET AS TABLE OF EMAILR
;

create index emailbodytext on emails(bodytext) indextype is lucene.luceneindex 
parameters('ExtraCols:emailFrom "dictionary", emailDate "emailDate",subject "subject",emailFrom "emailFrom",emailTo "emailTo"')
;

alter index emailbodyText parameters('LogLevel:ALL;FormatCols:dictionary(ANALYZED_WITH_POSITIONS_OFFSETS), subject(NOT_ANALYZED),emailFrom(NOT_ANALYZED),emailTo(NOT_ANALYZED)')
;

alter index emailbodyText parameters('Formatter:org.apache.lucene.search.highlight.MyHTMLFormatter;MaxNumFragmentsRequired:3;FragmentSeparator:...;FragmentSize:50')
;

alter index emailbodytext parameters('PerFieldAnalyzer:dictionary(org.apache.lucene.analysis.core.WhitespaceAnalyzer),BODYTEXT(org.apache.lucene.analysis.core.StopAnalyzer)')
;

CREATE OR REPLACE TRIGGER L$emailbodyText
BEFORE UPDATE OF emailDate,subject ON emails
FOR EACH ROW
BEGIN
      :new.bodyText := :new.bodyText;
END
;


create index emailFromIdx on emails(emailFrom)
;

create index emailToIdx on emails(emailTo)
;

create index emailDateIdx on emails(emaildate)
;

insert into emails values ('mbraun@uni-hd.de','java-user@lucene.apache.org',
'boosting instead of sorting WAS: to boost or not to boost',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN')-7,
'Hi Daniel,
>> so a doc from 1973 should get a boost of 1.1973 and a doc of 1975 should
>> get a boost of 1.1975 .
>
> The boost is stored with a limited resolution. Try boosting one doc by 10,
> the other one by 20 or something like that.
You''re right. I thought that with the float values the resolution should
be good enough!
But there is only a difference in the score with a boosting diff of 0.2
(e.g. 1.7 and 1.9).
I know that there were many questions on the list regarding scoring
better new documents.
But I want to avoid any overhead like "FunctionQuery" at query time,
and in my case I have some documents
which have same values in many fields (=>same score) and the only
difference is the year.
However  I don''t want to overboost the score so that the scoring for
other criteria is not considered.
Shortly spoken: As a result of a search I have a list of book titles and
I want  a sort by score AND by year of publication.
But for performance reasons I want to avoid this sorting at query-time
by boosting at index time.
Is that possible?
thanks,
Martin');

insert into emails values ('ab@getopt.org','java-user@lucene.apache.org',
'Re: boosting instead of sorting WAS: to boost or not to boost',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN')-6,
'Martin Braun wrote:
> Hi Daniel,
>
>
>>> so a doc from 1973 should get a boost of 1.1973 and a doc of 1975 should
>>> get a boost of 1.1975 .
>>>
>> The boost is stored with a limited resolution. Try boosting one doc by 10,
>> the other one by 20 or something like that.
>>
>
> You''re right. I thought that with the float values the resolution should
> be good enough!
> But there is only a difference in the score with a boosting diff of 0.2
> (e.g. 1.7 and 1.9).
>
> I know that there were many questions on the list regarding scoring
> better new documents.
> But I want to avoid any overhead like "FunctionQuery" at query time,
> and in my case I have some documents
> which have same values in many fields (=>same score) and the only
> difference is the year.
>
> However  I don''t want to overboost the score so that the scoring for
> other criteria is not considered.
>
> Shortly spoken: As a result of a search I have a list of book titles and
> I want  a sort by score AND by year of publication.
>
> But for performance reasons I want to avoid this sorting at query-time
> by boosting at index time.
>
> Is that possible?
>
Here''s the trick that works for me, without the issues of boost
resolution or FunctionQuery.
Add a separate field, say "days", in which you will put as many "1" as
many days elapsed since the epoch (not neccessarily since 1 Jan 1970 -
pick a date that makes sense for you). Then, if you want to prioritize
newer documents, just add "+days:1" to your query. Voila - the final
results are a sum of other score factors plus a score factor that is
higher for more recent document, containing more 1-s.
If you are dealing with large time spans, you can split this into years
and days-in-a-year, and apply query boosts, like "+years:1^10.0
+days:1^0.02". Do some experiments and find what works best for you.
--
Best regards,
Andrzej Bialecki     <><
 ___. ___ ___ ___ _ _   __________________________________
[__ || __|__/|__||\/|  Information Retrieval, Semantic Web
___|||__||  \|  ||  |  Embedded Unix, System Integration
http://www.sigram.com  Contact: info at sigram dot com');

insert into emails values ('lucenelist2005@danielnaber.de','java-user@lucene.apache.org',
'Re: boosting instead of sorting WAS: to boost or not to boost',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN')-5,
'On Thursday 21 December 2006 10:55, Martin Braun wrote:
> and in my case I have some documents
> which have same values in many fields (=>same score) and the only
> difference is the year.
Andrzej''s response sounds like a good solution, so just for completeness:
you can sort by more than one criterion, e.g. first by score, then by
date.
regards
 Daniel
--
http://www.danielnaber.de');

insert into emails values ('codeshepherd@gmail.com','java-user@lucene.apache.org',
'lucene injection',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN')-4,
'I am bothered about security problems with lucene. Is it vulnerable to
any kind of injection like mysql injection? many times the query from
user is passed to lucene for search without validating.
');

insert into emails values ('erik@ehatchersolutions.com','java-user@lucene.apache.org',
'Re: lucene injection',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN')-3,
'On Dec 21, 2006, at 4:56 AM, Deepan wrote:
> I am bothered about security problems with lucene. Is it vulnerable to
> any kind of injection like mysql injection? many times the query from
> user is passed to lucene for search without validating.
Rest easy.  There are no known security issues with Lucene, and it
has even undergone a recent static code analysis by Fortify (see the
lucene-dev e-mail list archives).  Unlike SQL, there is no
destructive behavior available through the QueryParser.
     Erik');
       
insert into emails values ('codeshepherd@gmail.com','java-user@lucene.apache.org',
'Re: lucene injection',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN')-2,
'On Thu, 2006-12-21 at 05:04 -0500, Erik Hatcher wrote:
> On Dec 21, 2006, at 4:56 AM, Deepan wrote:
> > I am bothered about security problems with lucene. Is it vulnerable to
> > any kind of injection like mysql injection? many times the query from
> > user is passed to lucene for search without validating.
>
> Rest easy.  There are no known security issues with Lucene, and it
> has even undergone a recent static code analysis by Fortify (see the
> lucene-dev e-mail list archives).  Unlike SQL, there is no
> destructive behavior available through the QueryParser.
thanks Erik,
>
>       Erik
>
>
> ---------------------------------------------------------------------
> To unsubscribe, e-mail: java-user-unsubscribe@lucene.apache.org
> For additional commands, e-mail: java-user-help@lucene.apache.org
>');

insert into emails values ('lucenelist2005@danielnaber.de','java-user@lucene.apache.org',
'Re: lucene injection',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN')-1,
'On Thursday 21 December 2006 10:56, Deepan wrote:
> I am bothered about security problems with lucene. Is it vulnerable to
> any kind of injection like mysql injection? many times the query from
> user is passed to lucene for search without validating.
This is only an issue if your index has permission information and you
modify the user''s query so that only parts of the index are visible to
him. For example, if you add "+permission:user" to the query the user
might add something like "OR permission:admin" to get access to more
documents. This is also why you should add new parts to the query
programmatically (BooleanQuery) to avoid the use of QueryParser.
Regards
 Daniel
--
http://www.danielnaber.de');

insert into emails values ('lucenelist2005@danielnaber.de','java-user@lucene.apache.org',
'Re: lucene injection',to_date('FRI, 31 AUG 2007 06:28:19 GMT','DY, dd MON YYYY HH24:MI:SS "GMT"','NLS_DATE_LANGUAGE = AMERICAN'),
'On Thursday 21 December 2006 10:56, Deepan wrote:
> I am bothered about security problems with lucene. Is it vulnerable to
> any kind of injection like mysql injection? many times the query from
> user is passed to lucene for search without validating.
This is only an issue if your index has permission information and you
modify the user''s query so that only parts of the index are visible to
him. For example, if you add "+permission:user" to the query the user
might add something like "OR permission:admin" to get access to more
documents. This is also why you should add new parts to the query
programmatically (BooleanQuery) to avoid the use of QueryParser.
Regards
 Daniel
--
http://www.danielnaber.de');
