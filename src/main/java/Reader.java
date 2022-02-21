import Config.LoadYaml;
import Imap.ImapClient;
import Report.ReportType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.mail.MessagingException;
import javax.mail.Part;
import java.io.File;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.*;

/*
   Class to read the attachment information.
 */
public class Reader {
    private Part attachment;
    private ReportType reportType;
    private LoadYaml loadYaml = new LoadYaml();
    private ImapClient imapClient = new ImapClient();

    public Reader() throws IOException {
    }

    /**
     * Read the percentage number from coverage report.
     *
     * @param month
     * @return
     * @throws IOException
     * @throws MessagingException
     * @throws InterruptedException
     */
    public Map<String, String> coverageReportReader(int month) throws IOException, MessagingException, InterruptedException {
        Map<String, String> reportResult = new HashMap<String, String>();
        List<String> podList;
        String monthString = new DateFormatSymbols().getMonths()[month-1];
        int year = Calendar.getInstance().get(Calendar.YEAR);

        reportResult.put("date", monthString + " " + year);

        podList = getPodList();
        for(String pod: podList){
            String result;
            try {
                attachment = imapClient.waitForMessageAndGetAttachment(pod, reportType.COVERAGE, month);
                result = getReportAutomatedPercentage(attachment);
            } catch (Exception e) {
                System.out.println("Failed to get report details for: " + pod + ", " + reportType.COVERAGE.toString());
                result = "n/a";
            }
            reportResult.put(pod.toLowerCase(), result);
        }
        return reportResult;

    }

    /**
     * Read the percentage number from newly automated report.
     *
     * @param month
     * @return
     * @throws IOException
     * @throws MessagingException
     * @throws InterruptedException
     */
    public Map<String, String> newAutomatedReportReader(int month) throws IOException, MessagingException, InterruptedException {
        Map<String, String> reportResult = new HashMap<String, String>();
        List<String> podList;
        ReportType reportType = null;

        podList = getPodList();
        for(String pod: podList){
            String result;
            try {
                attachment = imapClient.waitForMessageAndGetAttachment(pod, reportType.NEW, month);
                result = getReportAutomatedPercentage(attachment);
            } catch (Exception e) {
                System.out.println("Failed to get report details for: " + pod + ", " + reportType.NEW.toString());
                result = "n/a";
            }
            reportResult.put(pod, result);
        }
        return reportResult;

    }

    /**
     * Very hard coded, will fail to get the data if the format changes. It will return the 2nd value that follows
     * "Automated" in report.
     *
     * @param attachment
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    private String getNewlyAutomatedNumber(Part attachment) throws IOException, MessagingException {
        String content = getAttachmentContent(attachment);
        String contentLine[] = StringUtils.split(content, System.lineSeparator());
        for(int i = 0; i< contentLine.length; i++){
            String line[] = contentLine[i].split("\\s+");
            if(line[0].contains("Automated")){
                return line[1];
            }
        }
        return "n/a";
    }

    /**
     * Read data for all the pods listed in config file.
     *
     * @return
     * @throws IOException
     */
    public List<String> getPodList() throws IOException {
        List<String> podList;
        String pods = loadYaml.getConfig("Pods");
        podList = Arrays.asList(pods.split("\\s*,\\s*"));
        return  podList;
    }

    /**
     * Very hard coded, will fail to get the data if the format changes. It will return the 2nd value that follows
     * "Automated" in report.
     * @param attachment
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    private String getReportAutomatedPercentage(Part attachment) throws MessagingException, IOException {
        String content = getAttachmentContent(attachment);
        String contentLine[] = StringUtils.split(content, System.lineSeparator());
        for(int i = 0; i< contentLine.length; i++){
            String line[] = contentLine[i].split("\\s+");
            if(line[0].contains("Automated")){
                return line[2];
            }
        }
        return "n/a";
    }

    /**
     * Parse the report data to a string.
     * @param attachment
     * @return
     * @throws IOException
     * @throws MessagingException
     */
    private String getAttachmentContent(Part attachment) throws IOException, MessagingException {
        File file = File.createTempFile("prefix-", "-suffix");
        FileUtils.copyInputStreamToFile(attachment.getInputStream(), file);

        PDDocument document = PDDocument.load(file);
        String content = null;
        if (!document.isEncrypted()) {
            PDFTextStripper stripper = new PDFTextStripper();
            content = stripper.getText(document);
        }
        document.close();
        file.deleteOnExit();
        return content;
    }
}
