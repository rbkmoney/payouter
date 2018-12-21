package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.message_sender.*;
import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.enums.ReportStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.ReportException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.ReportSendService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportSendServiceImpl implements ReportSendService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ReportDao reportDao;

    private MessageSenderSrv.Iface dudoserClient;

    @Value("${service.dudoser.mail.from}")
    private String mailFrom;

    @Value("#{'${service.dudoser.mail.to}'.split(',')}")
    private List<String> mailTo;

    public ReportSendServiceImpl(ReportDao reportDao, MessageSenderSrv.Iface dudoserClient) {
        this.reportDao = reportDao;
        this.dudoserClient = dudoserClient;
    }

    @Override
    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void sendUnsentReports() {
        List<Report> reports = reportDao.getForSend();
        reports.forEach(report -> sendReport(report));
    }

    @Override
    @Transactional
    public void sendReport(Report report) {
        log.info("Try to send report, reportId='{}', subject='{}', mailFrom='{}', mailTo='{}'", report.getId(), report.getSubject(), mailFrom, mailTo);
        MessageMail messageMail = new MessageMail();
        messageMail.setMailBody(new MailBody(report.getDescription()));
        messageMail.setFromEmail(mailFrom);
        messageMail.setToEmails(mailTo);
        messageMail.setSubject(report.getSubject());

        List<MessageAttachment> attachments = new ArrayList<>();

        MessageAttachment attachment = new MessageAttachment();
        attachment.setName(report.getName());
        attachment.setData(report.getContent().getBytes(Charset.forName(report.getEncoding())));
        attachments.add(attachment);
        messageMail.setAttachments(attachments);

        Message message = new Message();
        message.setMessageMail(messageMail);

        try {
            reportDao.changeStatus(report.getId(), ReportStatus.SENT, LocalDateTime.now(ZoneOffset.UTC));
            dudoserClient.send(message);
            log.info("Report have been sent, reportId='{}', subject='{}', mailFrom='{}', mailTo='{}'", report.getId(), report.getSubject(), mailFrom, mailTo);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to change report status to 'READY', reportId='%d'", report.getId()));
        } catch (TException ex) {
            throw new ReportException(String.format("Couldn't send report, reportId='%d', mailFrom='%s', mailTo='%s'", report.getId(), mailFrom, mailTo), ex);
        }
    }
}
