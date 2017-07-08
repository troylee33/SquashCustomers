package se.osdsquash.excel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.poi.POIXMLProperties;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.ShapeTypes;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;

import se.osdsquash.common.SquashProperties;
import se.osdsquash.common.SquashUtil;
import se.osdsquash.common.SubscriptionPeriod;
import se.osdsquash.xml.XmlRepository;
import se.osdsquash.xml.jaxb.CustomerInfoType;
import se.osdsquash.xml.jaxb.CustomerType;
import se.osdsquash.xml.jaxb.InvoiceStatusType;
import se.osdsquash.xml.jaxb.InvoiceType;
import se.osdsquash.xml.jaxb.SubscriptionType;
import se.osdsquash.xml.jaxb.SubscriptionsType;

/**
 * Excel file handler, that can generate and handle Microsoft Excel files (the .xslx / '97 format).
 * <p>
 * The Excel framework used is Apache POI - XSSF.
 * </p>
 */
public class ExcelHandler {

    private static final String INVOICE_FILE_TIMESTAMP_FORMAT = "yyyyMMdd";
    private static final String INVOICE_CREATION_DATE_FORMAT = "yyyy-MM-dd";

    private XmlRepository xmlRepository;

    private InvoiceExcelWorkbook excelWorkbook;
    private InvoiceSheet invoiceSheet;

    public ExcelHandler(XmlRepository xmlRepository) {
        this.xmlRepository = xmlRepository;
    }

    /**
     * Generates a new invoice file, using given customer and options.
     * The actual invoice file is saved to local disk.
     * 
     * @param customer A valid customer to create invoice for
     * @param dueDays Nr of due days from now, when invoice must be paid
     * @param nextPeriod True if to use NEXT period, otherwise current one
     * 
     * @return The invoice meta-data object
     */
    public InvoiceType createInvoiceFile(CustomerType customer, int dueDays, boolean nextPeriod) {

        CustomerInfoType customerInfo = customer.getCustomerInfo();
        int invoiceNr = this.xmlRepository.getNewInvoiceNr();

        FileOutputStream fileOutput = null;

        try {
            // Start preparing a new Excel sheet
            // ------------------------------------------------------------------------------------

            // Create a new workbook having one sheet
            this.excelWorkbook = new InvoiceExcelWorkbook();
            this.invoiceSheet = this.excelWorkbook.getInvoiceSheet();

            // Set some generic options
            POIXMLProperties.CoreProperties docCoreProperties = this.excelWorkbook
                .getProperties()
                .getCoreProperties();
            docCoreProperties.setTitle("Faktura");
            docCoreProperties.setCreator(SquashProperties.CLUB_NAME);

            this.invoiceSheet.setColumnWidth(0, 3 * 256); // The width must be given as 'nr of character x 256'
            this.invoiceSheet.setColumnWidth(1, 44 * 256);
            this.invoiceSheet.setColumnWidth(2, 13 * 256);
            this.invoiceSheet.setColumnWidth(3, 14 * 256);
            this.invoiceSheet.setDefaultColumnWidth(10);
            this.invoiceSheet.setDefaultRowHeightInPoints(15);
            this.invoiceSheet.setDisplayGridlines(false);
            this.invoiceSheet.setZoom(100);

            XSSFDrawing sheetDrawing = this.invoiceSheet.createDrawingPatriarch();

            // First of all, add some empty space
            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();

            // Create a larger font
            XSSFFont font = this.excelWorkbook.createFont();
            font.setFontHeightInPoints((short) 16);
            font.setBold(true);
            XSSFCellStyle largeFontStyle = this.excelWorkbook.createCellStyle();
            largeFontStyle.setFont(font);

            InvoiceRow clubnameAndInvoiceNrRow = this.invoiceSheet.createNextPaddedRow();
            clubnameAndInvoiceNrRow.setHeightInPoints(22);

            // Start with the club logo and invoice nr
            // ------------------------------------------------------------------------------------

            InvoiceCell logoCell = clubnameAndInvoiceNrRow.createNextCell();
            logoCell.setCellStyle(largeFontStyle);
            logoCell.setCellValue(SquashProperties.CLUB_NAME);
            clubnameAndInvoiceNrRow.createNextCell();

            // Right align last cell here
            InvoiceCell invoiceNrCell = clubnameAndInvoiceNrRow.createNextCell();
            invoiceNrCell.setAlignment(CellStyle.ALIGN_CENTER);
            invoiceNrCell.setCellValue("FakturaNr:  " + invoiceNr);

            // Now add club's org.nr and current date
            // ------------------------------------------------------------------------------------

            InvoiceRow orgNrAndDateRow = this.invoiceSheet.createNextPaddedRow();

            orgNrAndDateRow
                .createNextCell()
                .setCellValue("Org.nr: " + SquashProperties.CLUB_ORG_NR);
            orgNrAndDateRow.createNextCell();

            Calendar invoiceCreationCal = Calendar.getInstance();

            // Right align last cell here
            String invoiceDate = new SimpleDateFormat(INVOICE_CREATION_DATE_FORMAT)
                .format(invoiceCreationCal.getTime());
            InvoiceCell invoiceDateCell = orgNrAndDateRow.createNextCell();
            invoiceDateCell.setAlignment(CellStyle.ALIGN_CENTER);
            invoiceDateCell.setCellValue("Datum:  " + invoiceDate);

            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();

            // Add the "Faktura" text
            // ------------------------------------------------------------------------------------

            InvoiceRow fakturaTextRow = this.invoiceSheet.createNextPaddedRow();

            XSSFFont fontFaktura = this.excelWorkbook.createFont();
            fontFaktura.setFontHeightInPoints((short) 15);
            fontFaktura.setBold(true);
            fontFaktura.setItalic(true);
            XSSFCellStyle fakturaFontStyle = this.excelWorkbook.createCellStyle();
            fakturaFontStyle.setFont(fontFaktura);
            InvoiceCell fakturaCell = fakturaTextRow.createNextCell();
            fakturaCell.setCellValue("FAKTURA");
            fakturaCell.setCellStyle(fakturaFontStyle);

            // This draws a line just above the "Faktura" text
            XSSFClientAnchor clientAnchorLine1 = sheetDrawing.createAnchor(
                4 /* X start position, relative from the cell top left corner */,
                4 /* Y start position, relative from the cell top left corner */,
                300 /* X end position, relative from the cell top left corner */,
                300 /* Y end position, relative from the cell top left corner */,
                1 /* Which column index to draw from, e.g. starting cell */,
                this.invoiceSheet
                    .currentRowIndex() /* Which row index to draw from, e.g. starting cell */,
                4 /* Which column index to draw to */,
                this.invoiceSheet.currentRowIndex()) /* Which row index to draw to */;

            XSSFSimpleShape lineShape = sheetDrawing.createSimpleShape(clientAnchorLine1);
            lineShape.setLineStyleColor(220, 220, 220);
            lineShape.setLineWidth(2);
            lineShape.setShapeType(ShapeTypes.LINE);
            this.invoiceSheet.createNextPaddedRow();

            // This draws a line just below the "Faktura" text
            XSSFClientAnchor clientAnchorLine2 = sheetDrawing.createAnchor(
                4 /* X start position, relative from the cell top left corner */,
                4 /* Y start position, relative from the cell top left corner */,
                300 /* X end position, relative from the cell top left corner */,
                300 /* Y end position, relative from the cell top left corner */,
                1 /* Which column index to draw from, e.g. starting cell */,
                this.invoiceSheet
                    .currentRowIndex() /* Which row index to draw from, e.g. starting cell */,
                4 /* Which column index to draw to */,
                this.invoiceSheet.currentRowIndex()) /* Which row index to draw to */;

            XSSFSimpleShape lineShape2 = sheetDrawing.createSimpleShape(clientAnchorLine2);
            lineShape2.setLineStyleColor(220, 220, 220);
            lineShape2.setLineWidth(2);
            lineShape2.setShapeType(ShapeTypes.LINE);
            this.invoiceSheet.createNextPaddedRow();

            // Add customer and club info. The left box is the customer, the right one is the club.
            // ------------------------------------------------------------------------------------

            InvoiceRow referencesRow = this.invoiceSheet.createNextPaddedRow();

            InvoiceCell yourReferenceCell = referencesRow.createNextCell();
            yourReferenceCell.setCellValue("Er referens:");
            yourReferenceCell.applyFontStyles(true, true);

            InvoiceCell ourReferenceCell = referencesRow.createNextCell();
            ourReferenceCell.setCellValue("Vår referens:");
            ourReferenceCell.applyFontStyles(true, true);

            InvoiceRow nameRow = this.invoiceSheet.createNextPaddedRow();
            nameRow.createNextCell().setCellValue(
                customerInfo.getFirstname() + " " + customerInfo.getLastname());
            nameRow.createNextCell().setCellValue(SquashProperties.INVOICE_NAME);

            InvoiceRow adressRow = this.invoiceSheet.createNextPaddedRow();
            adressRow.createNextCell().setCellValue(customerInfo.getStreet());
            adressRow.createNextCell().setCellValue(SquashProperties.INVOICE_STREET);

            InvoiceRow cityRow = this.invoiceSheet.createNextPaddedRow();
            cityRow.createNextCell().setCellValue(
                customerInfo.getPostalCode() + " " + customerInfo.getCity());
            cityRow.createNextCell().setCellValue(SquashProperties.INVOICE_CITY);

            InvoiceRow phoneRow = this.invoiceSheet.createNextPaddedRow();
            phoneRow.createNextCell().setCellValue(customerInfo.getTelephone());
            phoneRow.createNextCell().setCellValue(SquashProperties.INVOICE_PHONE);

            InvoiceRow emailRow = this.invoiceSheet.createNextPaddedRow();
            InvoiceCell emailCell1 = emailRow.createNextCell();
            emailCell1.setCellValue(customerInfo.getEmail());
            emailCell1.applyEmailLink();

            InvoiceCell emailCell2 = emailRow.createNextCell();
            emailCell2.setCellValue(SquashProperties.INVOICE_EMAIL);
            emailCell2.applyEmailLink();

            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();

            // Write the the track subscription(s) table, e.g. the invoice specification
            // ------------------------------------------------------------------------------------

            InvoiceRow trackTableHeaderRow = this.invoiceSheet.createNextPaddedRow();

            InvoiceCell descriptionCell = trackTableHeaderRow.createNextCell();
            descriptionCell.setCellValue("  Beskrivning");
            descriptionCell.applyFontStyles(true, false);

            // Skip one cell...
            trackTableHeaderRow.createNextCellPadded();

            InvoiceCell ammountCell = trackTableHeaderRow.createNextCell();
            ammountCell.setCellValue("          Belopp");
            ammountCell.applyFontStyles(true, false);

            // Add border around the header cell range
            String trackHeaderRowArea = trackTableHeaderRow.getCell(1).getAddress().formatAsString()
                + ":"
                + trackTableHeaderRow
                    .getCell(trackTableHeaderRow.currentCellIndex())
                    .getAddress()
                    .formatAsString();
            this.addBorder(trackHeaderRowArea, true);

            // Loop subscriptions and write track cost rows
            double totalPrice = 0d;

            // This gets us the correct subscription period
            SubscriptionPeriod period = new SubscriptionPeriod(nextPeriod);

            // First an empty row in the table...
            InvoiceRow firstTableRow = this.invoiceSheet.createNextPaddedRow();
            InvoiceCell firstTableCell = firstTableRow.createNextCellPadded();
            String trackTableStartCellName = firstTableCell.getAddress().formatAsString();

            boolean hasSubscriptions;
            SubscriptionsType subscriptionsType = customer.getSubscriptions();
            if (subscriptionsType == null || subscriptionsType.getSubscription().isEmpty()) {

                hasSubscriptions = false;

                // If no subscriptions, write a red warning info row about this
                InvoiceRow noSubscriptionsRow = this.invoiceSheet.createNextRow();

                // Skip through first cell, that's just the padding cell
                noSubscriptionsRow.createNextCell();

                String warningMessage = " OBS: Det finns inga abonnemang att fakturera!";
                InvoiceCell warningTextCell = noSubscriptionsRow.createNextCell();
                warningTextCell.setCellValue(warningMessage);

                XSSFCellStyle warningCellStyle = this.excelWorkbook.createCellStyle();
                XSSFFont redFont = this.excelWorkbook.createFont();
                redFont.setBold(true);
                redFont.setColor(IndexedColors.RED.getIndex());
                warningCellStyle.setFont(redFont);
                warningTextCell.setCellStyle(warningCellStyle);

            } else {

                hasSubscriptions = true;

                Iterator<SubscriptionType> subscriptionIterator = subscriptionsType
                    .getSubscription()
                    .iterator();

                while (subscriptionIterator.hasNext()) {
                    SubscriptionType subscription = subscriptionIterator.next();

                    // Write a track info row
                    {
                        InvoiceRow trackInfoRow = this.invoiceSheet.createNextRow();

                        // Skip through first cell, that's just the padding cell
                        trackInfoRow.createNextCell();

                        // Write a text like "Abbonemang bana 1, Torsdagar, kl 19:00"
                        String trackInfoText = "  Abonnemang bana "
                            + subscription.getTrackNumber()
                            + ", "
                            + SquashUtil.weekdayTypeToString(subscription.getWeekday())
                            + "ar"
                            + " kl "
                            + SquashUtil.getTrackTimeFromCalendar(subscription.getStartTime());

                        InvoiceCell trackInfoCell = trackInfoRow.createNextCell();

                        trackInfoCell.setCellValue(trackInfoText);

                        trackInfoRow.createNextCellPadded();
                        trackInfoRow.createNextCellPadded();
                    }

                    // Write another row with the track period and the price
                    {
                        InvoiceRow trackPeriodAndPriceRow = this.invoiceSheet.createNextRow();

                        // Skip through first cell, that's just the padding cell
                        trackPeriodAndPriceRow.createNextCell();

                        String trackPeriodText = "  Gäller perioden "
                            + period.getStartDayString()
                            + " till "
                            + period.getEndDayString();

                        trackPeriodAndPriceRow.createNextCell().setCellValue(trackPeriodText);
                        trackPeriodAndPriceRow.createNextCellPadded();

                        double trackPrice;
                        if (customerInfo.isCompany()) {
                            trackPrice = SquashProperties.TRACK_PRICE_COMPANY;
                        } else {
                            trackPrice = SquashProperties.TRACK_PRICE_PERSON;
                        }

                        InvoiceCell trackPriceCell = trackPeriodAndPriceRow.createNextCell();
                        trackPriceCell.setCurrencyFormat(trackPrice, true, false);

                        totalPrice += trackPrice;
                    }

                    // One empty row between track rows
                    this.invoiceSheet.createNextPaddedRow();
                }
            }

            // Add some blank rows, to better match the A4 paper height
            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();
            this.invoiceSheet.createNextPaddedRow();

            InvoiceRow lastTrackTableRow = this.invoiceSheet.createNextPaddedRow();

            lastTrackTableRow.createNextCellPadded();
            lastTrackTableRow.createNextCellPadded();
            InvoiceCell lastTrackTableCell = lastTrackTableRow.createNextCellPadded();

            // Add a border around the subscriptions table
            String trackTableCellRange = trackTableStartCellName
                + ":"
                + lastTrackTableCell.getAddress().formatAsString();

            this.addBorder(trackTableCellRange, true);

            // Now continue with some bottom ammount fields and payment info
            // ------------------------------------------------------------------------------------

            // Write the sum row
            InvoiceRow sumRow = this.invoiceSheet.createNextRow();
            sumRow.createNextCellPadded();
            sumRow.createNextCellPadded();

            InvoiceCell sumTextCell = sumRow.createNextCell();
            sumTextCell.setCellValue("  Summa");

            InvoiceCell sumValueCell = sumRow.createNextCell();
            sumValueCell.setCurrencyFormat(totalPrice, true, false);

            // Write the "moms" row, along with payment instructions box
            InvoiceRow momsRow = this.invoiceSheet.createNextRow();
            momsRow.createNextCellPadded();

            InvoiceCell paymentInfoCell = momsRow.createNextCell();
            paymentInfoCell.setCellValue("Bankgiro: " + SquashProperties.CLUB_BG_NR);
            paymentInfoCell.setAlignment(CellStyle.ALIGN_CENTER);

            // TODO: Check if there IS moms to set ?!?!?!?!
            double momsValue = 0d;

            momsRow.createNextCell().setCellValue("  Varav moms");
            InvoiceCell momsValueCell = momsRow.createNextCell();
            momsValueCell.setCurrencyFormat(momsValue, true, false);

            // Write the row with the total ammount to pay
            InvoiceRow ammountToPayRow = this.invoiceSheet.createNextRow();
            ammountToPayRow.createNextCellPadded();

            // Add the payment due date (no time parts), relative from "now", just below the BG nr
            Calendar dueCal = (Calendar) invoiceCreationCal.clone();
            dueCal.add(Calendar.DATE, dueDays);
            SquashUtil.timeZeroCalendar(dueCal);
            String dueDateString = new SimpleDateFormat(INVOICE_CREATION_DATE_FORMAT)
                .format(dueCal.getTime());

            InvoiceCell paymentInfo2Cell = ammountToPayRow.createNextCell();
            paymentInfo2Cell.applyFontStyles(true, false);

            paymentInfo2Cell.setCellValue("Förfallodag " + dueDateString);
            paymentInfo2Cell.setAlignment(CellStyle.ALIGN_CENTER);

            InvoiceCell ammountToPayTextCell = ammountToPayRow.createNextCell();
            ammountToPayTextCell.setCellValue("  Att betala");
            ammountToPayTextCell.applyFontStyles(true, false);

            InvoiceCell totalAmmountCell = ammountToPayRow.createNextCell();
            totalAmmountCell.setCurrencyFormat(totalPrice, true, true);

            // Add border around the sum area
            String sumRange = sumTextCell.getAddress().formatAsString()
                + ":"
                + totalAmmountCell.getAddress().formatAsString();
            this.addBorder(sumRange, true);

            // Write a row with payment marking info
            InvoiceRow markPaymentRow = this.invoiceSheet.createNextRow();
            markPaymentRow.createNextCellPadded();

            InvoiceCell paymentInfo3Cell = markPaymentRow.createNextCell();
            paymentInfo3Cell.setCellValue("Märk betalningen med FakturaNr!");
            paymentInfo3Cell.setAlignment(CellStyle.ALIGN_CENTER);

            // Write the workbook to a new file
            // ------------------------------------------------------------------------------------

            String fileTimestamp = new SimpleDateFormat(INVOICE_FILE_TIMESTAMP_FORMAT)
                .format(new Date());

            // Make sure that the sub-directory with the current day exists
            File currentInvoicesDayDir = new File(
                XmlRepository.INVOICES_DIR_PATH + "/" + invoiceDate);
            if (!currentInvoicesDayDir.exists()) {
                currentInvoicesDayDir.mkdirs();
            }

            StringBuilder filePath = new StringBuilder();
            filePath.append(currentInvoicesDayDir.getPath());
            filePath.append("/");
            filePath.append(customerInfo.getCustomerNumber());
            filePath.append("_");
            if (SquashUtil.isSet(customerInfo.getFirstname())) {
                filePath.append(customerInfo.getFirstname());
                filePath.append("_");
            }
            if (SquashUtil.isSet(customerInfo.getLastname())) {
                filePath.append(customerInfo.getLastname());
                filePath.append("_");
            }
            filePath.append("Faktura_");
            filePath.append(invoiceNr);
            filePath.append("_");
            filePath.append(fileTimestamp);
            filePath.append(".xlsx");
            fileOutput = new FileOutputStream(filePath.toString(), false);
            this.excelWorkbook.write(fileOutput);

            // Create meta-data object and return it
            // ------------------------------------------------------------------------------------

            InvoiceType invoice = this.xmlRepository.getNewInvoice();

            GregorianCalendar gregorialCal = new GregorianCalendar();
            DatatypeFactory datatypeFactory;
            try {
                datatypeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException exception) {
                throw new RuntimeException(exception);
            }
            gregorialCal.setTimeInMillis(invoiceCreationCal.getTimeInMillis());
            invoice.setCreatedDate(datatypeFactory.newXMLGregorianCalendar(gregorialCal));

            gregorialCal.setTimeInMillis(dueCal.getTimeInMillis());
            invoice.setDueDate(datatypeFactory.newXMLGregorianCalendar(gregorialCal));

            if (hasSubscriptions) {
                gregorialCal.setTimeInMillis(period.getStartDay().getTimeInMillis());
                invoice.setPeriodStartDate(datatypeFactory.newXMLGregorianCalendar(gregorialCal));
            }

            invoice.setInvoiceNumber(invoiceNr);
            invoice.setInvoiceStatus(InvoiceStatusType.NEW);
            invoice.setRelativeFilePath(filePath.toString());

            // Important to connect the invoice to the customer
            this.xmlRepository.addInvoiceToCustomer(customer, invoice);

            return invoice;

        } catch (IOException exception) {
            throw new RuntimeException(exception);

            // Resource cleanup:
        } finally {
            if (fileOutput != null) {
                try {
                    fileOutput.close();
                } catch (Exception ex) {
                    //Ignore this...
                }
            }
            if (this.excelWorkbook != null) {
                try {
                    this.excelWorkbook.close();
                } catch (Exception ex) {
                    //Ignore this...
                }
            }
        }
    }

    // Adds a black border around a cell area, can also be once cell only
    private void addBorder(String cellRangeSpan, boolean thinnerBorder) {

        short borderStyle = (thinnerBorder ? CellStyle.BORDER_THIN : CellStyle.BORDER_MEDIUM);

        // The range is given as the format "A1:B4"
        CellRangeAddress cellRange = CellRangeAddress.valueOf(cellRangeSpan);
        RegionUtil.setBorderBottom(borderStyle, cellRange, this.invoiceSheet, this.excelWorkbook);
        RegionUtil.setBorderLeft(borderStyle, cellRange, this.invoiceSheet, this.excelWorkbook);
        RegionUtil.setBorderRight(borderStyle, cellRange, this.invoiceSheet, this.excelWorkbook);
        RegionUtil.setBorderTop(borderStyle, cellRange, this.invoiceSheet, this.excelWorkbook);
    }
}
