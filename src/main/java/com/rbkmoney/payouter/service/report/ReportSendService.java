package com.rbkmoney.payouter.service.report;

import com.rbkmoney.damsel.message_sender.*;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 06.02.17
 **/
@Service
public class ReportSendService {

    Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MessageSenderSrv.Iface dudoser;

    @Value("${service.dudoser.mail.from}")
    private String mailFrom;

    public void sendEmail(List<String> mailTo, String subject, Report report, String encoding) throws TException {
        log.info("Try to send Email. Subject: {}", subject);
        MessageMail messageMail = new MessageMail();
        messageMail.setMailBody(new MailBody(report.getDescription()));
        messageMail.setFromEmail(mailFrom);
        messageMail.setToEmails(mailTo);
        messageMail.setSubject(subject);

        List<MessageAttachment> attachments = new ArrayList<>();

        MessageAttachment attachment = new MessageAttachment();
        attachment.setName(report.getName());
        attachment.setData(report.getContent().getBytes(Charset.forName(encoding)));
        attachments.add(attachment);
        messageMail.setAttachments(attachments);

        Message message = new Message();
        message.setMessageMail(messageMail);

        dudoser.send(message);
        log.info("Email sent. Subject: {} ", subject);
    }
}
