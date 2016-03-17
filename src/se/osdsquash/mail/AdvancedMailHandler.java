package se.osdsquash.mail;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import se.osdsquash.xml.XmlRepository;

public class AdvancedMailHandler {

    public void createMailDraft(String recipientAddress, String attachmentFilename) {

        FileOutputStream mailOutputStream = null;
        try {
            // Create a default MimeMessage object
            Session session = Session.getDefaultInstance(new Properties());
            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(SquashProperties.INVOICE_EMAIL));
            message
                .setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientAddress));
            message.setSubject("Squash");

            // Create a multipart message, this is the "master" part
            Multipart multipart = new MimeMultipart();

            // Create the message/content part of the mail
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("Hello, olles mail");
            multipart.addBodyPart(messageBodyPart);

            // Add another part, which is the attachment
            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attachmentFilename);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(attachmentFilename);
            multipart.addBodyPart(messageBodyPart);

            // Complete the message
            message.setContent(multipart);
            message.saveChanges();

            // Write to local temporary file
            String eMailFilename = XmlRepository.INVOICES_DIR_PATH
                + "/TempMail"
                + (String.valueOf(new Date().getTime()))
                + ".eml";
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
                        + " Kontrollera att det finns ett standard-mailprogram. Felmeddelande: "
                        + ioException.getMessage();
                } catch (Exception exception) {
                    errorMessage = "Kan inte öppna mailprogrammet, ett okänt fel inträffade. Felmeddelande: "
                        + exception.getMessage();
                }
            }

            if (errorMessage != null) {
                JOptionPane.showMessageDialog(null, errorMessage, "Fel", JOptionPane.ERROR_MESSAGE);
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
}
