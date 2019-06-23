package com.scotas.lucene.indexer;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStreamReader;

import java.math.BigDecimal;

import java.net.URL;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.text.DecimalFormat;

import java.util.Map;

import oracle.sql.BINARY_DOUBLE;
import oracle.sql.BINARY_FLOAT;
import oracle.sql.CLOB;

import oracle.sql.NUMBER;
import oracle.sql.TIMESTAMP;
import oracle.sql.TIMESTAMPTZ;
import oracle.sql.TIMESTAMPLTZ;

import oracle.xdb.XMLType;

import oracle.xml.parser.v2.SAXParser;
import oracle.xml.parser.v2.XMLParseException;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.scotas.lucene.util.StringUtils;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * This class is an example of how to implement an User Data Store function
 */
public class DefaultUserDataStore implements UserDataStore {
    protected Map formats;
    
    protected Object []cachedFormaters;
        
    protected Field []cachedFields;
      
    protected ContentHandler saxHandler = null;

    protected SAXParser saxParser = new SAXParser();
    
    protected Connection conn = null;

    public DefaultUserDataStore() {
    }

    public void setConnection(Connection conn) {
        this.conn = conn;    
    }
    
    public void setColumnFormat(Map formats) {
        this.formats = formats;
    }
    
    private void initFormaters(String col, Object value,
                               String[] extraNames,
                               Object[] extraVals) throws SQLException,
                                                        IOException {
        int numFormaters = (extraVals != null) ? extraVals.length + 1 : 1;
        this.cachedFormaters = new Object[numFormaters];
        this.cachedFields = new Field[numFormaters+1];
        this.saxHandler = new MyContentHandler();
        this.saxParser.setContentHandler(this.saxHandler);
        String format = (String)this.formats.get(col);
        if (value instanceof String || value instanceof XMLType || value instanceof CLOB)
            cachedFormaters[0] = format;
        else if (value instanceof Date || value instanceof Timestamp ||
                 value instanceof TIMESTAMP || value instanceof TIMESTAMPTZ ||
                 value instanceof TIMESTAMPLTZ)
            cachedFormaters[0] =
                    getDateResolution((format != null) ? format : "day");
        else if (value instanceof BigDecimal || value instanceof Double ||
                 value instanceof Float || value instanceof NUMBER ||
                 value instanceof BINARY_FLOAT || value instanceof BINARY_DOUBLE)
            cachedFormaters[0] =
                    new DecimalFormat((format != null) ? format : "0000000000");
        else // any other types formatter is an String
            cachedFormaters[0] = format;
        this.cachedFields[0] = new Field("rowid", "--not defined yet--", Field.Store.YES,
                          Field.Index.NOT_ANALYZED);
        this.cachedFields[1] = getField(col,value,this.cachedFormaters[0]);
        //System.out.println("Master column col: "+col+" value: "+value +" cachedFormatters: "+cachedFormaters[0]);
        //System.out.println("Cached Field for rowid: "+this.cachedFields[0].toString());
        //System.out.println("Cached Field for "+col+": "+this.cachedFields[1].toString());
        for (int i = 1; i < numFormaters; i++) {
            Object objVal = extraVals[i - 1];
            format = (String)this.formats.get(extraNames[i - 1]);
            if (objVal instanceof Date || objVal instanceof Timestamp ||
                objVal instanceof TIMESTAMP || objVal instanceof TIMESTAMPTZ ||
                objVal instanceof TIMESTAMPLTZ)
                cachedFormaters[i] =
                        getDateResolution((format != null) ? format : "day");
            else if (objVal instanceof BigDecimal ||
                     objVal instanceof Double || 
                     objVal instanceof Float ||
                     objVal instanceof NUMBER ||
                     objVal instanceof BINARY_FLOAT ||
                     objVal instanceof BINARY_DOUBLE)
                cachedFormaters[i] =
                        new DecimalFormat((format != null) ? format :
                                          "0000000000");
            else if (objVal instanceof String || objVal instanceof XMLType || objVal instanceof CLOB)
                cachedFormaters[i] = format;
            this.cachedFields[i+1] = getField(extraNames[i - 1],objVal,this.cachedFormaters[i]);
            //System.out.println("column: "+extraVals[i-1]+" value: "+objVal +" cachedFormatters: "+cachedFormaters[i]);
            //System.out.println("Cached Field for "+extraNames[i - 1]+": "+this.cachedFields[i+1].toString());
        }
    }
    
    public DateTools.Resolution getDateResolution(String resolution) {
        if ("year".equalsIgnoreCase(resolution))
            return DateTools.Resolution.YEAR;
        else if ("month".equalsIgnoreCase(resolution))
            return DateTools.Resolution.MONTH;
        else if ("day".equalsIgnoreCase(resolution))
            return DateTools.Resolution.DAY;
        else if ("hour".equalsIgnoreCase(resolution))
            return DateTools.Resolution.HOUR;
        else if ("minute".equalsIgnoreCase(resolution))
            return DateTools.Resolution.MINUTE;
        else if ("second".equalsIgnoreCase(resolution))
            return DateTools.Resolution.SECOND;
        else if ("millisecond".equalsIgnoreCase(resolution))
            return DateTools.Resolution.MILLISECOND;
        else
            throw new RuntimeException(".setDateResolution: Invalid DateTools.Resolution");
    }

    public StringBuffer textExtractor(XMLType obj) throws IOException,
                                                          SQLException {
        StringBuffer result = new StringBuffer("");
        if (obj == null) // Sanity checks
            return result;
        //System.out.println("saxHandler: " + saxHandler + " result: " + result);
        ((MyContentHandler)saxHandler).setTextResult(result);
        try {
           saxParser.parse(obj.getInputStream());
        } catch (XMLParseException e) {
            e.printStackTrace();
            throw new SQLException("XMLParseException: " + e.getMessage());
        } catch (SAXException e) {
            e.printStackTrace();
            throw new SQLException("SAXException: " + e.getMessage());
        }
        ((MyContentHandler)saxHandler).setTextResult(null);
        return result;
    }

    public static String readStream(BufferedReader reader) {
        StringBuffer buffer = new StringBuffer();
        try {
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    buffer.append(line);
                } else {
                    break;
                }
            }
        } catch (IOException iox) {
            iox.printStackTrace();
        }
        return buffer.toString();
    }

    public String valueExtractor(Object value,
                                 Object formatter) throws SQLException,
                                                          IOException {
        String valueStr = null;
        if (value != null) { // Sanity checks
            //System.out.println(".valueExtractor class: " +
            //                   value.getClass().getName() + " value: '"+ value + "' formatter: "+formatter);
            if (value instanceof String && formatter == null)
                valueStr = (String)value;
            else if (value instanceof CLOB)
                valueStr =
                        readStream(new BufferedReader(((CLOB)value).characterStreamValue()));
            else if (value instanceof URL) {
                valueStr =
                        readStream(new BufferedReader(new InputStreamReader(((URL)value).openStream())));
            } else if (value instanceof XMLType)
                if (formatter != null) {
                    XMLType obj =
                        ((XMLType)value).extract((String)formatter, null);
                    valueStr = (obj != null) ? textExtractor(obj).toString() : "";
                } else
                    valueStr = textExtractor((XMLType)value).toString();
            else if (value instanceof Date)
                valueStr =
                        DateTools.dateToString((Date)value, (DateTools.Resolution)formatter);
            else if (value instanceof Timestamp)
                valueStr =
                        DateTools.timeToString(((Timestamp)value).getTime(), (DateTools.Resolution)formatter);
            else if (value instanceof TIMESTAMP)
                valueStr =
                        DateTools.timeToString(((TIMESTAMP)value).timestampValue().getTime(),
                                               (DateTools.Resolution)formatter);
            else if (value instanceof TIMESTAMPTZ)
                valueStr =
                        DateTools.timeToString(((TIMESTAMPTZ)value).timestampValue(this.conn).getTime(),
                                               (DateTools.Resolution)formatter);
            else if (value instanceof TIMESTAMPLTZ)
                valueStr =
                        DateTools.timeToString(((TIMESTAMPLTZ)value).timestampValue().getTime(),
                                               (DateTools.Resolution)formatter);
            else if (value instanceof BigDecimal)
                valueStr =
                        ((DecimalFormat)formatter).format(((BigDecimal)value).doubleValue());
            else if (value instanceof Double)
                valueStr =
                        ((DecimalFormat)formatter).format(((Double)value).doubleValue());
            else if (value instanceof Float)
                valueStr =
                        ((DecimalFormat)formatter).format(((Float)value).doubleValue());
            else if (value instanceof NUMBER)
                valueStr =
                        ((DecimalFormat)formatter).format(((NUMBER)value).doubleValue());
            else if (value instanceof BINARY_FLOAT)
                valueStr =
                        ((DecimalFormat)formatter).format(((BINARY_FLOAT)value).doubleValue());
            else if (value instanceof BINARY_DOUBLE)
                valueStr =
                        ((DecimalFormat)formatter).format(((BINARY_DOUBLE)value).doubleValue());
            else if (value instanceof String) { // formatters != null
                String fmtString = (String)formatter;
                if (fmtString.startsWith("ANALYZED") || fmtString.startsWith("NOT_ANALYZED"))
                  valueStr = (String)value;
                else
                  valueStr = StringUtils.leftPad((String)value, fmtString.length(),
                                            fmtString.charAt(0));
            } else // any other types will use toString() method
                valueStr = value.toString();
        } else {
          // Values can't be null in fields
          valueStr = "";
        }
        //System.out.println(".valueExtractor string: "+valueStr);
        return valueStr;
    }

  public Field getField(String name, Object objVal,
                        Object formatter) throws SQLException, IOException {
    Field fld = null;
    //System.out.println(".getField: name=" + name + " val:=" + objVal +
    //                   " formatter=" + formatter);
    if (formatter instanceof String) {
      if (((String)formatter).startsWith("NOT_ANALYZED")) {
        fld =
            new Field(name, valueExtractor(objVal, null), (((String)formatter).endsWith("_STORED")) ?
                                                          Field.Store.YES :
                                                          Field.Store.NO,
                      Field.Index.NOT_ANALYZED);
      } else if (((String)formatter).startsWith("ANALYZED")) {
        if (((String)formatter).endsWith("_WITH_OFFSETS"))
          fld =
              new Field(name, valueExtractor(objVal, null), Field.Store.YES, Field.Index.ANALYZED,
                        Field.TermVector.WITH_OFFSETS);
        else if (((String)formatter).endsWith("_WITH_POSITIONS"))
          fld =
              new Field(name, valueExtractor(objVal, null), Field.Store.YES, Field.Index.ANALYZED,
                        Field.TermVector.WITH_POSITIONS);
        else if (((String)formatter).endsWith("_WITH_POSITIONS_OFFSETS"))
          fld =
              new Field(name, valueExtractor(objVal, null), Field.Store.YES, Field.Index.ANALYZED,
                        Field.TermVector.WITH_POSITIONS_OFFSETS);
        else if (((String)formatter).endsWith("_WITH_VECTORS"))
          fld =
              new Field(name, valueExtractor(objVal, null), Field.Store.YES, Field.Index.ANALYZED,
                        Field.TermVector.YES);
        else
          fld =
              new Field(name, valueExtractor(objVal, null), Field.Store.YES, Field.Index.ANALYZED,
                        Field.TermVector.NO);
      } else {
        fld =
            new Field(name, valueExtractor(objVal, formatter), Field.Store.NO,
                      Field.Index.ANALYZED);
      }
    } else if (objVal instanceof Timestamp || objVal instanceof Date ||
               objVal instanceof TIMESTAMP || objVal instanceof TIMESTAMPTZ ||
               objVal instanceof TIMESTAMPLTZ ||
               objVal instanceof BigDecimal || objVal instanceof Float ||
               objVal instanceof Double || objVal instanceof NUMBER ||
               objVal instanceof BINARY_FLOAT ||
               objVal instanceof BINARY_DOUBLE)
      fld =
          new Field(name, valueExtractor(objVal, formatter), Field.Store.NO, Field.Index.NOT_ANALYZED);
    else
      fld =
          new Field(name, valueExtractor(objVal, formatter), Field.Store.NO, Field.Index.ANALYZED);
    return fld;
  }

  public Document getDocument(String rowid, String col, Object value,
                                String[] extraNames,
                                Object[] extraVals) throws SQLException,
                                                           IOException {
        if (this.saxHandler == null)
            initFormaters(col, value, extraNames, extraVals);
        Document doc = new Document();
        this.cachedFields[0].setStringValue(rowid);
        doc.add(this.cachedFields[0]);
        if (value != null) {// Sanity checks
            this.cachedFields[1].setStringValue(valueExtractor(value, this.cachedFormaters[0]));
            doc.add(this.cachedFields[1]);
        }
        int numCols = extraNames.length;
        for (int i = 0; i < numCols; i++) {
            Object objVal = extraVals[i];
            if (objVal != null) {// Sanity checks
                this.cachedFields[i+2].setStringValue(valueExtractor(objVal, this.cachedFormaters[i + 1]));
                doc.add(this.cachedFields[i+2]);
            }
        }
        //System.out.println(".getDocument: doc="+doc);
        return doc;
    }

    public static class MyContentHandler extends DefaultHandler {
        StringBuffer textResult = null;

        public void startElement(String uri, String localName, String qName,
                                 Attributes atts) {
            int i = atts.getLength();
            for (int j = 0; j < i; j++) {
                textResult.append(atts.getValue(j)).append(' ');
                //System.out.println(".adding: "+atts.getValue(j));
            }
        }

        public void endElement(String uri, String localName, String qName) {
        }

        public void characters(char[] chars, int start, int length) {
            String strPart = new String(chars, start, length);
            textResult.append(strPart).append(' ');
            //System.out.println(".adding: "+strPart);
        }

        public void setTextResult(StringBuffer buff) {
            this.textResult = buff;
        }
    }
}
