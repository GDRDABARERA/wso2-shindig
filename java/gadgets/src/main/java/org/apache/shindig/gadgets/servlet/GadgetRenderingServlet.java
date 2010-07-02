/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.servlet;

import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.render.Renderer;
import org.apache.shindig.gadgets.render.RenderingResults;
import org.apache.shindig.gadgets.uri.IframeUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;

import com.google.inject.Inject;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for rendering Gadgets.
 */
public class GadgetRenderingServlet extends InjectedServlet {

  private static final long serialVersionUID = -5634040113214794888L;

  static final int DEFAULT_CACHE_TTL = 60 * 5;

  private static final Logger LOG = Logger.getLogger(GadgetRenderingServlet.class.getName());

  private transient Renderer renderer;
  private transient IframeUriManager iframeUriManager;
  private transient boolean initialized;

  @Inject
  public void setRenderer(Renderer renderer) {
    if (initialized) {
      throw new IllegalStateException("Servlet already initialized");
    }
    this.renderer = renderer;
  }
  
  @Inject
  public void setIframeUriManager(IframeUriManager iframeUriManager) {
    if (initialized) {
      throw new IllegalStateException("Servlet already initialized");
    }
    this.iframeUriManager = iframeUriManager;
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    initialized = true;
  }

  private void render(HttpServletRequest req, HttpServletResponse resp, UriStatus urlstatus)
      throws IOException {
    if (req.getHeader(HttpRequest.DOS_PREVENTION_HEADER) != null) {
      // Refuse to render for any request that came from us.
      // TODO: Is this necessary for any other type of request? Rendering seems to be the only one
      // that can potentially result in an infinite loop.
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    resp.setContentType("text/html");
    resp.setCharacterEncoding("UTF-8");

    GadgetContext context = new HttpGadgetContext(req);
    RenderingResults results = renderer.render(context);
    switch (results.getStatus()) {
      case OK:
        if (context.getIgnoreCache() ||
            urlstatus == UriStatus.INVALID_VERSION) {
          HttpUtil.setCachingHeaders(resp, 0);
        } else if (urlstatus == UriStatus.VALID_VERSIONED) {
          // Versioned files get cached indefinitely
          HttpUtil.setCachingHeaders(resp, true);
        } else {
          // Unversioned files get cached for 5 minutes by default, but this can be overridden
          // with a query parameter.
          int ttl = DEFAULT_CACHE_TTL;
          String ttlStr = req.getParameter(ProxyBase.REFRESH_PARAM);
          if (!StringUtils.isEmpty(ttlStr)) {
            try {
              ttl = Integer.parseInt(ttlStr);
            } catch (NumberFormatException e) {
              // Ignore malformed TTL value
              LOG.info("Bad TTL value '" + ttlStr + "' was ignored");
            }
          }
          HttpUtil.setCachingHeaders(resp, ttl, true);
        }
        resp.getWriter().print(results.getContent());
        break;
      case ERROR:
        resp.setStatus(results.getHttpStatusCode());
        resp.getWriter().print(StringEscapeUtils.escapeHtml(results.getErrorMessage()));
        break;
      case MUST_REDIRECT:
        resp.sendRedirect(results.getRedirect().toString());
        break;
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // If an If-Modified-Since header is ever provided, we always say
    // not modified. This is because when there actually is a change,
    // cache busting should occur.
    UriStatus urlstatus = getUrlStatus(req);
    if (req.getHeader("If-Modified-Since") != null &&
        !"1".equals(req.getParameter("nocache")) &&
        urlstatus == UriStatus.VALID_VERSIONED) {
      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }
    render(req, resp, urlstatus);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    render(req, resp, getUrlStatus(req));
  }
  
  private UriStatus getUrlStatus(HttpServletRequest req) {
    return iframeUriManager.validateRenderingUri(new UriBuilder(req).toUri());
  }
}
