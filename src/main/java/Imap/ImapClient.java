package Imap;

import Config.LoadYaml;
import Report.ReportType;

import javax.mail.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

/*
  Class to connect to zimbra mailbox to download emails with report attachments.
 */
public class ImapClient {
    private final Store store;
    private final Folder inbox;
    private final Session session;
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private LoadYaml loadYaml = new LoadYaml();

    private static final int DEFAULT_RETRIES = 3;
    private static final int DEFAULT_INTERVAL_MILLISECS = 1 * 1000;

    public ImapClient() throws IOException {

        this.host = loadYaml.getConfig("hostname");
        this.port = parseInt(loadYaml.getConfig("port"));
        this.user = loadYaml.getConfig("user");
        this.password = loadYaml.getConfig("password");

        // Time out: 2 min
        String socketTimeoutProp = Integer.toString(120 * 1000);
        // Data loaded from yaml file
        Properties properties = new Properties();
        properties.put("mail.imap.starttls.enable", "true");
        properties.put("host", host);
        properties.put("mail.transport.protocol", "smtp");
        properties.put("host", host);
        properties.put("user", user);
        properties.put("password", password);
        properties.put("mail.smtp.connectiontimeout", socketTimeoutProp);
        properties.put("mail.smtp.timeout", socketTimeoutProp);

        session = Session.getDefaultInstance(properties, null);


        // Connect to mail server
        try {
            store = session.getStore("imap");
            store.connect(host, port, user, password);
            Folder folder = store.getDefaultFolder();
            inbox = folder.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
        } catch (MessagingException e) {
            throw new IllegalStateException("Encountered exception while setting up ImapClient", e);
        }
    }

    /**
     * Download attachment from email.
     *
     * @param pod
     * @param reportType
     * @param month
     * @return
     * @throws IOException
     * @throws MessagingException
     * @throws InterruptedException
     */
    public Part waitForMessageAndGetAttachment(String pod, ReportType reportType, int month)
            throws IOException, MessagingException, InterruptedException {
        Message emailMsg = waitForMessageByReportName(pod, reportType, month);
        return getAttachmentFromMessage(emailMsg);
    }

    /**
     * Search for message by report type + month + pod name.
     *
     * @param pod
     * @param reportType
     * @param month
     * @return
     * @throws MessagingException
     * @throws InterruptedException
     */
    public Message waitForMessageByReportName(String pod, ReportType reportType, int month)
            throws MessagingException, InterruptedException {
        int year = Calendar.getInstance().get(Calendar.YEAR);

        System.out.println();
        System.out.println("Getting report for: " + pod + " - " + reportType.toString() + " - Month: " + month + ", Year: " + year + ".");

        Message message = waitForLatestMessageByPredicate(m -> m.getSubject() != null
                && m.getSubject().contains(reportType.toString())
                && m.getSubject().contains(month + "/")
                && m.getSubject().contains(String.valueOf(year))
                && m.getSubject().contains(pod));

        return message;
    }

    /**
     * Get the first matching email from inbox.
     *
     * @param p
     * @return
     * @throws InterruptedException
     * @throws MessagingException
     */
    public Message waitForLatestMessageByPredicate(PredicateNoExceptions<Message> p)
            throws InterruptedException, MessagingException {
        List<Message> someMessages = waitForMessageListByPredicate(p);
        if (someMessages != null && someMessages.size() > 0) {
            return someMessages.get(0);
        }
        return null;
    }

    /**
     * Represents a predicate T (boolean-valued function that targets for lambda
     * expression) of one argument.
     */
    public interface PredicateNoExceptions<T> extends Predicate<T> {
        // Return true if the input argument matches the predicate, otherwise
        // false
        default boolean test(T t) {
            try {
                return testThrows(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        boolean testThrows(T t) throws Exception;
    }

    /**
     * Searching email by predicate.
     *
     * @param p
     * @return
     * @throws InterruptedException
     * @throws MessagingException
     */
    public List<Message> waitForMessageListByPredicate(PredicateNoExceptions<Message> p)
            throws InterruptedException, MessagingException {
        List<Message> someMessages = null;
        // Max number of retries
        int retriesLeft = DEFAULT_RETRIES;

        do {
            try {
                Thread.sleep(DEFAULT_INTERVAL_MILLISECS);
                someMessages = getAllMessagesWithPredicate(p);
            } catch (InterruptedException | MessagingException e) {
            }
        } while (someMessages.size() < 1 && --retriesLeft > 0);

        return someMessages;
    }


    /**
     * This method will return a message list with all messages matching with
     * filter: p.
     *
     * @return a list of matched messages
     * @throws MessagingException
     */
    public List<Message> getAllMessagesWithPredicate(PredicateNoExceptions<Message> p)
            throws MessagingException {
        return ((List<Message>) Arrays.stream(inbox.getMessages())
                .filter(p).collect(Collectors.toList()));
    }


    /**
     * This method will call getAttachment() method to get the attachment from
     * the message.
     *
     * @return Part attachment
     * @throws IOException
     * @throws MessagingException
     */
    public Part getAttachmentFromMessage(Message message) throws IOException, MessagingException {
        try {
            Part attachment = getAttachment(message);
            return attachment;
        } catch (NullPointerException e) {
        }
        return null;
    }

    /** Actual method to look up the attachment inside a message and get it.
     *
             * @return Part attachment
     * @throws IOException
     * @throws MessagingException
     */
    public Part getAttachment(Message message) throws IOException, MessagingException {
        Multipart mp = (Multipart) message.getContent();
        for (int i = 0, n = mp.getCount(); i < n; i++) {
            Part part = mp.getBodyPart(i);
            String disposition = part.getDisposition();
            if (disposition != null && disposition.equals(Part.ATTACHMENT)) {
                return part;
            }
        }
        return null;
    }
}