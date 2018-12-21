package com.rbkmoney.payouter.endpoint;

import com.rbkmoney.woody.thrift.impl.http.THServiceBuilder;

import javax.servlet.*;
import java.io.IOException;


public abstract class AbstractEndpointServlet extends GenericServlet {

    private Servlet thriftServlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = servletHandler(new THServiceBuilder());
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }

    protected abstract Servlet servletHandler(THServiceBuilder builder);
}
