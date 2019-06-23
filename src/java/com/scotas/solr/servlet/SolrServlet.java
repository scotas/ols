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


import com.scotas.license.OlsLicense;
import com.scotas.license.OlsLicenseManager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.BinaryQueryResponseWriter;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;

import org.xml.sax.SAXException;


/**
 */

final public class SolrServlet extends HttpServlet {

    @SuppressWarnings("compatibility")
    private static final long serialVersionUID = 1L;

    private boolean isValid = true;
    
    public void init() throws ServletException {
        String validLicense = "/com/scotas/license/valid.ols";
        log("SolrServlet.init[Trying validlicense...]");
        OlsLicenseManager lcs = OlsLicenseManager.getInstance();
        isValid = lcs.licenseExpires(validLicense);
        log("SolrServlet.init[valid: " + isValid + "]");
        if (isValid) {
            OlsLicense il = (OlsLicense)lcs.getLicense(validLicense);
            log("License signed for   : <" + il.getName() + ">");
            log("License signed email : <" + il.getEmail() + ">");
            log("License valid until  : <" + il.getExpiration() + ">");
        }
        log("SolrServlet.init[" + this + "] using coreContainer: " +
            PerSessionCoreContainer.instanceId());
    }

    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException,
                                                            IOException {
        doGet(request, response);
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException,
                                                           IOException {

        final SolrCore core;
        final String coreName = request.getParameter("core");
        final String coreAction = request.getParameter("core.action");
        if (!isValid) {
            this.sendMsg(401, "Invalide Licenese Key found or Expired, please contact OLS sales support.",
                         response);
            return;
        }
        if (coreAction != null) {
            if ("reload".equalsIgnoreCase(coreAction))
                this.doTrace(request, response);
            else if ("delete".equalsIgnoreCase(coreAction))
                this.doDelete(request, response);
            else if ("register".equalsIgnoreCase(coreAction))
                this.doPut(request, response);
            else
                this.sendMsg(403, "Invalid core.action name: " + coreAction,
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
        SolrServletRequest solrReq = new SolrServletRequest(core, request);
        SolrQueryResponse solrRsp = new SolrQueryResponse();
        //Writer writer = new OutputStreamWriter(response.getOutputStream());
        OutputStream writer = response.getOutputStream();
        try {
            String reqParam = solrReq.getParams().get(CommonParams.QT);
            SolrRequestHandler handler = core.getRequestHandler(reqParam);
            if (handler == null) {
                SolrException s =
                    new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                                      "Unknown Request Handler '" +
                                      solrReq.getParams().get(CommonParams.QT) +
                                      "'");
                log("Unknown Request Handler '" +
                    solrReq.getParams().get(CommonParams.QT) + "' :" + solrReq,
                    s);
                throw s;
            }
            core.execute(handler, solrReq, solrRsp);
            if (solrRsp.getException() == null) {
                QueryResponseWriter responseWriter =
                    core.getQueryResponseWriter(solrReq);
                response.setContentType(responseWriter.getContentType(solrReq,
                                                                      solrRsp));
                //responseWriter.write(writer, solrReq, solrRsp);
                BinaryQueryResponseWriter binWriter =
                    (BinaryQueryResponseWriter)responseWriter;
                binWriter.write(writer, solrReq, solrRsp);
            } else {
                Exception e = solrRsp.getException();
                int rc = 500;
                if (e instanceof SolrException) {
                    rc = ((SolrException)e).code();
                }
                sendMsg(rc, SolrException.toStr(e), response);
            }
        } catch (SolrException e) {
            log("Error excecuting request: " + e.getLocalizedMessage(), e);
            sendMsg(e.code(), SolrException.toStr(e), response);
        } catch (Throwable e) {
            log("Error excecuting request: " + e.getLocalizedMessage(), e);
            sendMsg(500, SolrException.toStr(e), response);
        } finally {
            // This releases the IndexReader associated with the request
            solrReq.close();
            //core.close();
            writer.flush();
        }
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


    public void destroy() {
        log("SolrServlet.destroy()");
        super.destroy();
    }

    @Override
    protected void doDelete(HttpServletRequest request,
                            HttpServletResponse response) throws ServletException,
                                                                 IOException {
        String coreName = request.getParameter("core");
        PerSessionCoreContainer.remove(coreName);
        sendMsg(200, "Core removed: " + coreName, response);
    }

    @Override
    protected void doTrace(HttpServletRequest request,
                           HttpServletResponse response) throws ServletException,
                                                                IOException {
        String coreName = request.getParameter("core");
        try {
            PerSessionCoreContainer.reload(coreName);
            sendMsg(200, "Core reloaded: " + coreName, response);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            sendMsg(500, "ParserConfigurationException: " + e.getMessage(),
                    response);
        } catch (SAXException e) {
            e.printStackTrace();
            sendMsg(500, "SAXException: " + e.getMessage(), response);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException,
                                                              IOException {
        String coreName = request.getParameter("core");
        try {
            PerSessionCoreContainer.getCore(coreName);
            sendMsg(200, "Core loaded: " + coreName, response);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            sendMsg(500, "ParserConfigurationException: " + e.getMessage(),
                    response);
        } catch (SAXException e) {
            e.printStackTrace();
            sendMsg(500, "SAXException: " + e.getMessage(), response);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        log("SolrServlet.finalize()");
        super.finalize();
    }
}
