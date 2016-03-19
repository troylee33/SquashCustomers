package se.osdsquash.mail;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.JOptionPane;

import se.osdsquash.common.SquashProperties;
import se.osdsquash.common.SquashUtil;
import se.osdsquash.logger.SquashLogger;

/**
 * Handles e-mailing to customers
 */
public class MailHandler {

    // Returns the path and static start of the temp mail files
    private static final String MAIL_FILENAME_PREFIX = "SquashTempMail_";
    private static final String MAIL_FILE_DIR = System.getProperty("java.io.tmpdir");

    /**
     * Creates a new mail draft and opens it in the default mail program
     * 
     * @param recipientAddress The customer's address
     * @param attachmentPath Optional path and filename to attach to mail, null if no file
     * @param useInvoiceTopic True if to add the invoice text to the mail
     */
    public void createMailDraft(
        String recipientAddress,
        String attachmentPath,
        boolean useInvoiceTopic) {

        FileOutputStream mailOutputStream = null;
        try {
            if (attachmentPath == null) {
                // If no attachment, we can simply open a new mail right away
                String uriString = String.format(
                    "mailto:%s?subject=%s&body=%s",
                    recipientAddress,
                    this.urlEncode("Squash"),
                    this.urlEncode(this.getMailMessage(useInvoiceTopic)));

                String errorMessage = null;
                if (!Desktop.isDesktopSupported()) {
                    errorMessage = "Kan inte initiera mailprogrammet på denna plattform, hittar ingen Desktop.";
                }

                if (errorMessage == null) {
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.mail(new URI(uriString));

                    } catch (UnsupportedOperationException uoException) {
                        errorMessage = "Kan inte initiera mailprogrammet på denna plattform. Felmeddelande: "
                            + uoException.getMessage();
                    } catch (IOException ioException) {
                        errorMessage = "Kan inte öppna mailprogrammet."
                            + " Kontrollera att det finns ett mailprogram. Felmeddelande: "
                            + ioException.getMessage();
                    } catch (Exception exception) {
                        errorMessage = "Kan inte öppna mailprogrammet, ett okänt fel inträffade. Felmeddelande: "
                            + exception.getMessage();
                    }
                }

                if (errorMessage != null) {
                    SquashLogger.getInstance().log(errorMessage, true);
                    JOptionPane
                        .showMessageDialog(null, errorMessage, "Fel", JOptionPane.ERROR_MESSAGE);
                }

            } else {
                // If we have an attachment, it gets a bit more complex:
                // We must create and store a mail file, then open that file which will
                // cause the default mail program to pick it up, with the attachment prepared.

                if (!new File(attachmentPath).isFile()) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Fakturafilen kunde inte hittas: " + attachmentPath,
                        "Fel",
                        JOptionPane.ERROR_MESSAGE);
                }

                // Create a default MimeMessage object
                Session session = Session.getDefaultInstance(new Properties());
                Message message = new MimeMessage(session);

                message.setFrom(new InternetAddress(SquashProperties.INVOICE_EMAIL));
                message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientAddress));
                message.setSubject("Squash-faktura");

                // Create a multipart message, this is the "master" part
                Multipart multipart = new MimeMultipart();

                // Create the message/content part of the mail
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(this.getMailMessage(useInvoiceTopic));
                multipart.addBodyPart(messageBodyPart);

                // Add another part, which is the attachment
                messageBodyPart = new MimeBodyPart();
                DataSource fileSource = new FileDataSource(attachmentPath);
                messageBodyPart.setDataHandler(new DataHandler(fileSource));
                messageBodyPart.setFileName(SquashUtil.getFilenameFromPath(attachmentPath));
                multipart.addBodyPart(messageBodyPart);

                // Complete the message
                message.setContent(multipart);
                message.saveChanges();

                // Write to local temporary file
                final String mailFileSuffix = ".eml";
                String eMailFilename = MAIL_FILE_DIR
                    + "/"
                    + MAIL_FILENAME_PREFIX
                    + (String.valueOf(new Date().getTime()))
                    + mailFileSuffix;
                mailOutputStream = new FileOutputStream(eMailFilename);
                message.writeTo(mailOutputStream);
                mailOutputStream.close();

                String errorMessage = null;
                if (!Desktop.isDesktopSupported()) {
                    errorMessage = "Kan inte initiera mailprogrammet på denna plattform, hittar ingen Desktop.";
                }

                if (errorMessage == null) {
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.open(new File(eMailFilename));

                    } catch (UnsupportedOperationException uoException) {
                        errorMessage = "Kan inte initiera mailprogrammet på denna plattform. Felmeddelande: "
                            + uoException.getMessage();
                    } catch (IOException ioException) {
                        errorMessage = "Kan inte öppna mailprogrammet."
                            + " Kontrollera att det finns ett mailprogram som kan öppna "
                            + mailFileSuffix
                            + "-filer. Felmeddelande: "
                            + ioException.getMessage();
                    } catch (Exception exception) {
                        errorMessage = "Kan inte öppna mailprogrammet, ett okänt fel inträffade. Felmeddelande: "
                            + exception.getMessage();
                    }
                }

                if (errorMessage != null) {
                    SquashLogger.getInstance().log(errorMessage, true);
                    JOptionPane
                        .showMessageDialog(null, errorMessage, "Fel", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (MessagingException | IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (mailOutputStream != null) {
                try {
                    mailOutputStream.close();
                } catch (Exception ex) {
                    // Ignore...
                }
            }
        }
    }

    // Builds the text message in the mail, with or without invoice template text
    private String getMailMessage(boolean useInvoiceTopic) {

        final StringBuilder messageBuilder = new StringBuilder(512);
        if (useInvoiceTopic) {
            messageBuilder.append("\n");
            messageBuilder.append("Hej!");
            messageBuilder.append("\n");
            messageBuilder.append("\n");
            messageBuilder
                .append("Bifogat i detta mail finns fakturan för ditt squash-abonnemang.");
            messageBuilder.append("\n");
            messageBuilder.append("Vänligen notera betalningsinstruktionerna i fakturafilen.");
            messageBuilder.append("\n");
            messageBuilder.append("\n");
            messageBuilder.append("Lycka till med squashen!");
            messageBuilder.append("\n");
            messageBuilder.append("\n");
            messageBuilder.append("Med vänlig hälsning");
            messageBuilder.append("\n");
            messageBuilder.append(SquashProperties.INVOICE_NAME);
            messageBuilder.append("\n");
            messageBuilder.append(SquashProperties.CLUB_NAME);
            messageBuilder.append("\n");
        } else {
            messageBuilder.append("\n");
            messageBuilder.append("Hej!");
            messageBuilder.append("\n");
            messageBuilder.append("\n");
            messageBuilder.append("\n");
            messageBuilder.append("Med vänlig hälsning");
            messageBuilder.append("\n");
            messageBuilder.append(SquashProperties.INVOICE_NAME);
            messageBuilder.append("\n");
            messageBuilder.append(SquashProperties.CLUB_NAME);
            messageBuilder.append("\n");
        }

        return messageBuilder.toString();
    }

    private String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes old mail temp files
     */
    public static void deleteMailTempFiles() {

        SquashLogger logger = SquashLogger.getInstance();

        logger.log("Searching and deleting old temporary files...", false);

        // Check for very old mail files and delete them
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, -1);
        final long thresholdMillis = cal.getTimeInMillis();

        File[] tooOldFiles = new File(MAIL_FILE_DIR).listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isFile()
                    && pathname.getName().contains(MAIL_FILENAME_PREFIX)
                    && pathname.lastModified() < thresholdMillis;
            }
        });

        if (tooOldFiles != null) {
            for (File file : tooOldFiles) {
                try {
                    if (!file.delete()) {
                        logger
                            .log("Notis: Kunde ej radera gammal mail-fil: " + file.getPath(), true);
                    }
                } catch (Exception ex) {
                    logger.log("Notis: Kunde ej radera gammal mail-fil: " + file.getPath(), true);
                }
            }
        }

        logger.log("Temporary files delete finished...", false);
    }
}
