package com.rbkmoney.payouter.endpoint;

import com.rbkmoney.damsel.payout_processing.PayoutManagementSrv;
import com.rbkmoney.payouter.meta.UserIdentityIdExtensionKit;
import com.rbkmoney.payouter.meta.UserIdentityRealmExtensionKit;
import com.rbkmoney.woody.thrift.impl.http.THServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import java.util.Arrays;

@WebServlet("/payout/management")
public class PayoutManagementServlet extends AbstractEndpointServlet {

    private final PayoutManagementSrv.Iface requestHandler;

    @Autowired
    public PayoutManagementServlet(PayoutManagementSrv.Iface requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    protected Servlet servletHandler(THServiceBuilder builder) {
        return builder
                .withMetaExtensions(
                        Arrays.asList(
                                UserIdentityIdExtensionKit.INSTANCE,
                                UserIdentityRealmExtensionKit.INSTANCE)
                )
                .build(PayoutManagementSrv.Iface.class, requestHandler);
    }
}
