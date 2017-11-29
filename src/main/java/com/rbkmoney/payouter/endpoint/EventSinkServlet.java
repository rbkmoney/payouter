package com.rbkmoney.payouter.endpoint;

import com.rbkmoney.damsel.payout_processing.EventSinkSrv;
import com.rbkmoney.woody.thrift.impl.http.THServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;

@WebServlet("/repo")
public class EventSinkServlet extends AbstractEndpointServlet {

    private final EventSinkSrv.Iface requestHandler;

    @Autowired
    public EventSinkServlet(EventSinkSrv.Iface requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    protected Servlet servletHandler(THServiceBuilder builder) {
        return builder.build(EventSinkSrv.Iface.class, requestHandler);
    }
}
