/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scotas.solr.servlet;


import com.scotas.license.OlsLicenseManager;
import com.scotas.lucene.store.OJVMUtil;
import com.scotas.solr.handler.RowIDLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.common.util.XML;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;

import org.xml.sax.SAXException;


/**
 * Update Servlet only used for testing purpose using RAMDirectory
 */
public class SolrUpdateServlet extends HttpServlet {

    @SuppressWarnings("compatibility:-5965720182317798292")
    private static final long serialVersionUID = 1L;

    private boolean isValid = true;
    
    @Override
    public void init() throws ServletException {
        String validLicense = "/com/scotas/license/valid.ols";
        log("SolrUpdateServlet.init[Validating license...]");
        isValid = OlsLicenseManager.getInstance().licenseExpires(validLicense);
        log("SolrUpdateServlet.init[" + this + "] using coreContainer: " +
            PerSessionCoreContainer.instanceId());
    }

    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException,
                                                            IOException {
        final SolrCore core;
        final InputStream is = request.getInputStream();
        final ArrayList m1 = (ArrayList)new JavaBinCodec().unmarshal(is);
        final String coreName = (String)m1.get(0);
        final String coreAction = (String)m1.get(1);
        if (!isValid) {
            this.sendMsg(401, "Invalide Licenese Key found or Expired, please contact OLS sales support.",
                         response);
            return;
        }
        try {
            core = PerSessionCoreContainer.getCore(coreName);
            //log("Core: " + core.toString());
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            sendMsg(500, "ParserConfigurationException: " + e.getMessage(),
                    response);
            return;
        } catch (SAXException e) {
            e.printStackTrace();
            sendMsg(500, "SAXException: " + e.getMessage(), response);
            return;
        }
        Writer writer = new OutputStreamWriter(response.getOutputStream());
        response.setContentType(QueryResponseWriter.CONTENT_TYPE_XML_UTF8);

        SolrQueryRequest req =
            new LocalSolrQueryRequest(core, new HashMap<String, String[]>());
        if ("sync".equalsIgnoreCase(coreAction))
            try {
                ArrayList deleted = (ArrayList)m1.get(2);
                ArrayList inserted = (ArrayList)m1.get(3);
                // Old style requests do not choose a custom handler
                UpdateRequestProcessorChain processorFactory =
                    core.getUpdateProcessingChain(null);

                SolrQueryResponse rsp = new SolrQueryResponse(); // ignored
                UpdateRequestProcessor processor =
                    processorFactory.createProcessor(req, rsp);
                RowIDLoader loader =
                    new RowIDLoader(processor, coreName, (String[])deleted.toArray(new String[] { }),
                                    (String[])inserted.toArray(new String[] { }));
                loader.processSync(req, processor);
                processor.finish();
                writer.write("<result status=\"0\"></result>\n");
            } catch (Exception ex) {
                try {
                    OJVMUtil.getConnection().rollback();
                    log("Error processing update command", ex);
                    XML.writeXML(writer, "result", SolrException.toStr(ex),
                                 "status", "1");
                } catch (Exception ee) {
                    log("Error writing to output stream", ee);
                }
            } finally {
                req.close();
            }
        else if ("commit".equalsIgnoreCase(coreAction)) {
            Boolean waitFlush = (Boolean)m1.get(2); // deprecated
            Boolean waitSearcher = (Boolean)m1.get(3);
            Boolean softCommit = (Boolean)m1.get(4);
            Boolean expungeDeletes = (Boolean)m1.get(5);
            Integer maxSegments =  (Integer)m1.get(6);
            Boolean optimize = (Boolean)m1.get(7);
            CommitUpdateCommand cmd = new CommitUpdateCommand(req, optimize);
            // deprecated
            //cmd.waitFlush = waitFlush.booleanValue();
            cmd.waitSearcher = waitSearcher.booleanValue();
            cmd.softCommit = softCommit.booleanValue();
            cmd.expungeDeletes = expungeDeletes.booleanValue();
            cmd.maxOptimizeSegments = maxSegments.intValue();
            SolrQueryResponse rsp = new SolrQueryResponse(); // ignored
            UpdateRequestProcessorChain processorFactory =
                core.getUpdateProcessingChain(null);
            UpdateRequestProcessor processor =
                processorFactory.createProcessor(req, rsp);
            processor.processCommit(cmd);
            processor.finish();
            writer.write("<result status=\"0\"></result>\n");
        } else
            XML.writeXML(writer, "result", "invalid action: " + coreAction,
                         "status", "1");
        try {
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //log.info("exiting");
    }

    final void sendMsg(int rc, String msg, HttpServletResponse response) {
        try {
            // hmmm, what if this was already set to text/xml?
            try {
                response.setContentType(QueryResponseWriter.CONTENT_TYPE_TEXT_UTF8);
                // response.setCharacterEncoding("UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                response.setStatus(rc);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Writer writer = new OutputStreamWriter(response.getOutputStream());
            writer.write(msg);
            writer.flush();
        } catch (IOException e) {
            log("Error sending msg: " + e.getLocalizedMessage(), e);
        }
    }
}
