package com.rbkmoney.payouter.endpoint;

import com.rbkmoney.damsel.claim_management.ClaimCommitterSrv;
import com.rbkmoney.woody.thrift.impl.http.THServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet("/claim-committer")
public class ClaimCommitterServlet extends GenericServlet {

    private Servlet thriftServlet;

    private final ClaimCommitterSrv.Iface claimCommitterService;

    @Autowired
    public ClaimCommitterServlet(ClaimCommitterSrv.Iface claimCommitterService) {
        this.claimCommitterService = claimCommitterService;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .build(ClaimCommitterSrv.Iface.class, claimCommitterService);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }

}
