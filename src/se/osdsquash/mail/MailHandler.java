package se.osdsquash.mail;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import javax.swing.JOptionPane;

import se.osdsquash.common.SquashProperties;

/**
 * E-mail handler, for sending mail.
 */
public class MailHandler {

    public MailHandler() {
        // Empty
    }

    /**
     * Creates and opens a new e-mail as a draft in the user's default E-mail client
     * 
     * @param emailAddress The recipient address to send to
     * @param invoiceTopic True if to use the invoice template text, false for an empty mail
     */
    public void createMailDraft(String emailAddress, boolean invoiceTopic) {

        String errorMessage = null;
        if (!Desktop.isDesktopSupported()) {
            errorMessage = "Kan inte initiera mailprogrammet på denna plattform, hittar ingen Desktop.";
        }

        if (errorMessage == null) {
            Desktop desktop = Desktop.getDesktop();
            try {

                desktop.mail(this.getMailURI(emailAddress, invoiceTopic));

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
    }

    private URI getMailURI(String emailAddress, boolean invoiceTopic) {

        final String subject;
        final String mailContent;

        if (invoiceTopic) {
            subject = "Squash-faktura";

            final StringBuilder messageBuilder = new StringBuilder(512);
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
            mailContent = messageBuilder.toString();
        } else {
            subject = "Squash";

            final StringBuilder messageBuilder = new StringBuilder(512);
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
            mailContent = messageBuilder.toString();
        }

        String uriString = String.format(
            "mailto:%s?subject=%s&body=%s",
            emailAddress,
            this.urlEncode(subject),
            this.urlEncode(mailContent.toString()));

        try {
            return new URI(uriString);

        } catch (URISyntaxException exception) {
            throw new RuntimeException(exception);
        }
    }

    // Fixes any illegal characters
    private String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }
    }
}
