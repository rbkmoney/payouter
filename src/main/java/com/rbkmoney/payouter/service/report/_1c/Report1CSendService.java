package com.rbkmoney.payouter.service.report._1c;

import com.rbkmoney.payouter.service.report.ReportSendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @since 06.02.17
 **/
@Service
public class Report1CSendService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Report1CService report1CService;

    @Autowired
    private ReportSendService reportSendService;



}
