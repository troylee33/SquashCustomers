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
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.ShapeTypes;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

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

    private XSSFWorkbook excelWorkbook;
    private XSSFSheet fakturaSheet;

    public ExcelHandler(XmlRepository xmlRepository) {
        this.xmlRepository = xmlRepository;
    }

    /**
     * Generates a new invoice file, using given customer and options.
     * The actual invoice file is saved to local disk.
     * 
     * @param customer A valid customer to create invoice for
     * @param dueDays Nr of due days from now, when invoice must be paid
     * 
     * @return The invoice meta-data object
     */
    public InvoiceType createInvoiceFile(CustomerType customer, int dueDays) {

        CustomerInfoType customerInfo = customer.getCustomerInfo();
        int invoiceNr = this.xmlRepository.getNewInvoiceNr();

        FileOutputStream fileOutput = null;

        try {
            // Start preparing a new Excel sheet
            // ------------------------------------------------------------------------------------

            // Create a new workbook having one sheet
            this.excelWorkbook = new XSSFWorkbook(XSSFWorkbookType.XLSX);
            this.fakturaSheet = this.excelWorkbook.createSheet("Faktura");

            // Set some generic options
            POIXMLProperties.CoreProperties docCoreProperties = this.excelWorkbook
                .getProperties()
                .getCoreProperties();
            docCoreProperties.setTitle("Faktura");
            docCoreProperties.setCreator(SquashProperties.CLUB_NAME);

            this.fakturaSheet.setColumnWidth(0, 3 * 256); // The width must be given as 'nr of character x 256'
            this.fakturaSheet.setColumnWidth(1, 44 * 256);
            this.fakturaSheet.setColumnWidth(2, 15 * 256);
            this.fakturaSheet.setColumnWidth(3, 15 * 256);
            this.fakturaSheet.setDefaultColumnWidth(10);
            this.fakturaSheet.setDefaultRowHeightInPoints(15);
            this.fakturaSheet.setDisplayGridlines(false);
            this.fakturaSheet.setZoom(100);

            XSSFDrawing sheetDrawing = this.fakturaSheet.createDrawingPatriarch();

            // Start on row 0 and increment the rows
            RowCounter rowCounter = new RowCounter();

            // First of all, add some empty space
            this.createPaddingRow(rowCounter.current());
            this.createPaddingRow(rowCounter.next());

            // Create a larger font
            XSSFFont font = this.excelWorkbook.createFont();
            font.setFontHeightInPoints((short) 16);
            font.setBold(true);
            XSSFCellStyle largeFontStyle = this.excelWorkbook.createCellStyle();
            largeFontStyle.setFont(font);

            XSSFRow clubnameAndInvoiceNrRow = this.createNewRow(rowCounter.next());
            clubnameAndInvoiceNrRow.setHeightInPoints(22);

            // For each row, there is a cell counter
            CellCounter cellCounter = rowCounter.getCellCounter();

            // Start with the club logo and invoice nr
            // ------------------------------------------------------------------------------------

            XSSFCell logoCell = clubnameAndInvoiceNrRow.createCell(cellCounter.next());
            logoCell.setCellStyle(largeFontStyle);
            logoCell.setCellValue(SquashProperties.CLUB_NAME);

            clubnameAndInvoiceNrRow.createCell(cellCounter.next());

            // Right align last cell here
            XSSFCell invoiceNrCell = this
                .setRightAlign(clubnameAndInvoiceNrRow.createCell(cellCounter.next()));
            invoiceNrCell.setCellValue("FakturaNr:  " + invoiceNr);

            // Now add club's org.nr and current date
            // ------------------------------------------------------------------------------------

            XSSFRow orgNrAndDateRow = this.createNewRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();
            orgNrAndDateRow.createCell(cellCounter.next()).setCellValue(
                "Org.nr: " + SquashProperties.CLUB_ORG_NR);

            orgNrAndDateRow.createCell(cellCounter.next());

            Calendar invoiceCreationCal = Calendar.getInstance();

            // Right align last cell here
            String invoiceDate = new SimpleDateFormat(INVOICE_CREATION_DATE_FORMAT)
                .format(invoiceCreationCal.getTime());
            XSSFCell invoiceDateCell = this
                .setRightAlign(orgNrAndDateRow.createCell(cellCounter.next()));
            invoiceDateCell.setCellValue("Datum: " + invoiceDate);

            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());

            // Add the "Faktura" text
            // ------------------------------------------------------------------------------------

            XSSFRow fakturaTextRow = this.createNewRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();

            XSSFFont fontFaktura = this.excelWorkbook.createFont();
            fontFaktura.setFontHeightInPoints((short) 15);
            fontFaktura.setBold(true);
            fontFaktura.setItalic(true);
            XSSFCellStyle fakturaFontStyle = this.excelWorkbook.createCellStyle();
            fakturaFontStyle.setFont(fontFaktura);
            XSSFCell fakturaCell = fakturaTextRow.createCell(cellCounter.next());
            fakturaCell.setCellValue("FAKTURA");
            fakturaCell.setCellStyle(fakturaFontStyle);

            // This draws a line just above the "Faktura" text
            XSSFClientAnchor clientAnchorLine1 = sheetDrawing.createAnchor(
                4 /* X start position, relative from the cell top left corner */,
                4 /* Y start position, relative from the cell top left corner */,
                300 /* X end position, relative from the cell top left corner */,
                300 /* Y end position, relative from the cell top left corner */,
                1 /* Which column index to draw from, e.g. starting cell */,
                rowCounter.current() /* Which row index to draw from, e.g. starting cell */,
                4 /* Which column index to draw to */,
                rowCounter.current()) /* Which row index to draw to */;

            XSSFSimpleShape lineShape = sheetDrawing.createSimpleShape(clientAnchorLine1);
            lineShape.setLineStyleColor(220, 220, 220);
            lineShape.setLineWidth(2);
            lineShape.setShapeType(ShapeTypes.LINE);

            this.createPaddingRow(rowCounter.next());

            // This draws a line just below the "Faktura" text
            XSSFClientAnchor clientAnchorLine2 = sheetDrawing.createAnchor(
                4 /* X start position, relative from the cell top left corner */,
                4 /* Y start position, relative from the cell top left corner */,
                300 /* X end position, relative from the cell top left corner */,
                300 /* Y end position, relative from the cell top left corner */,
                1 /* Which column index to draw from, e.g. starting cell */,
                rowCounter.current() /* Which row index to draw from, e.g. starting cell */,
                4 /* Which column index to draw to */,
                rowCounter.current()) /* Which row index to draw to */;

            XSSFSimpleShape lineShape2 = sheetDrawing.createSimpleShape(clientAnchorLine2);
            lineShape2.setLineStyleColor(220, 220, 220);
            lineShape2.setLineWidth(2);
            lineShape2.setShapeType(ShapeTypes.LINE);

            this.createPaddingRow(rowCounter.next());

            // Add customer and club info. The left box is the customer, the right one is the club.
            // ------------------------------------------------------------------------------------

            XSSFRow referencesRow = this.createNewRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();
            this.setBoldFont(referencesRow.createCell(cellCounter.next()), true).setCellValue(
                "Er referens:");
            this.setBoldFont(referencesRow.createCell(cellCounter.next()), true).setCellValue(
                "Vår referens:");

            XSSFRow nameRow = this.createNewRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();
            nameRow.createCell(cellCounter.next()).setCellValue(
                customerInfo.getFirstname() + " " + customerInfo.getLastname());
            nameRow.createCell(cellCounter.next()).setCellValue(SquashProperties.INVOICE_NAME);

            XSSFRow adressRow = this.createNewRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();
            adressRow.createCell(cellCounter.next()).setCellValue(customerInfo.getStreet());
            adressRow.createCell(cellCounter.next()).setCellValue(SquashProperties.INVOICE_STREET);

            XSSFRow cityRow = this.createNewRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();
            cityRow.createCell(cellCounter.next()).setCellValue(
                customerInfo.getPostalCode() + " " + customerInfo.getCity());
            cityRow.createCell(cellCounter.next()).setCellValue(SquashProperties.INVOICE_CITY);

            XSSFRow phoneRow = this.createNewRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();
            phoneRow.createCell(cellCounter.next()).setCellValue(customerInfo.getTelephone());
            phoneRow.createCell(cellCounter.next()).setCellValue(SquashProperties.INVOICE_PHONE);

            XSSFRow emailRow = this.createNewRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();
            XSSFCell emailCell1 = emailRow.createCell(cellCounter.next());
            emailCell1.setCellValue(customerInfo.getEmail());
            this.setEmailLink(emailCell1);

            XSSFCell emailCell2 = emailRow.createCell(cellCounter.next());
            emailCell2.setCellValue(SquashProperties.INVOICE_EMAIL);
            this.setEmailLink(emailCell2);

            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());

            // Write the the track subscription(s) table, e.g. the invoice specification
            // ------------------------------------------------------------------------------------

            XSSFRow trackTableHeaderRow = this.createNewRow(rowCounter.next());

            cellCounter = rowCounter.getCellCounter();
            this
                .setBoldFont(trackTableHeaderRow.createCell(cellCounter.next()), false)
                .setCellValue("  Beskrivning");
            this
                .setBoldFont(trackTableHeaderRow.createCell(cellCounter.next()), false)
                .setCellValue("     ");
            this
                .setBoldFont(trackTableHeaderRow.createCell(cellCounter.next()), false)
                .setCellValue("Belopp");

            // Add border around the header cell range
            String trackHeaderRowArea = trackTableHeaderRow.getCell(1).getAddress().formatAsString()
                + ":"
                + trackTableHeaderRow.getCell(cellCounter.current()).getAddress().formatAsString();
            this.addBorder(trackHeaderRowArea, true);

            // Loop subscriptions and write track cost rows
            double totalPrice = 0d;

            // This gets us the next, upcoming subscription period.
            // We assume all invoices are for the next period:
            SubscriptionPeriod nextPeriod = new SubscriptionPeriod(true);

            // First an empty row in the table...
            XSSFRow firstTableRow = this.createPaddingRow(rowCounter.next());
            XSSFCell firstTableCell = this
                .createPaddingCell(firstTableRow, rowCounter.getCellCounter().next());
            String trackTableStartCellName = firstTableCell.getAddress().formatAsString();

            SubscriptionsType subscriptionsType = customer.getSubscriptions();
            if (subscriptionsType == null || subscriptionsType.getSubscription().isEmpty()) {

                // If no subscriptions, write a red warning info row about this
                XSSFRow noSubscriptionsRow = this.fakturaSheet.createRow(rowCounter.next());
                cellCounter = rowCounter.getCellCounter();

                String warningMessage = " OBS: Det finns inga abonnemang att fakturera!";
                XSSFCell warningTextCell = noSubscriptionsRow.createCell(cellCounter.next());
                warningTextCell.setCellValue(warningMessage);

                XSSFCellStyle warningCellStyle = this.excelWorkbook.createCellStyle();
                XSSFFont redFont = this.excelWorkbook.createFont();
                redFont.setBold(true);
                redFont.setColor(IndexedColors.RED.getIndex());
                warningCellStyle.setFont(redFont);
                warningTextCell.setCellStyle(warningCellStyle);

            } else {
                Iterator<SubscriptionType> subscriptionIterator = subscriptionsType
                    .getSubscription()
                    .iterator();

                while (subscriptionIterator.hasNext()) {
                    SubscriptionType subscription = subscriptionIterator.next();

                    // Write a track info row
                    {
                        XSSFRow trackInfoRow = this.fakturaSheet.createRow(rowCounter.next());

                        cellCounter = rowCounter.getCellCounter();

                        // Write a text like "Abbonemang bana 1, Torsdagar, kl 19:00"
                        String trackInfoText = "  Abonnemang bana "
                            + subscription.getTrackNumber()
                            + ", "
                            + SquashUtil.weekdayTypeToString(subscription.getWeekday())
                            + "ar"
                            + " kl "
                            + SquashUtil.getTrackTimeFromCalendar(subscription.getStartTime());
                        XSSFCell trackInfoCell = trackInfoRow.createCell(cellCounter.next());
                        trackInfoCell.setCellValue(trackInfoText);

                        this.createPaddingCell(trackInfoRow, cellCounter.next());
                        this.createPaddingCell(trackInfoRow, cellCounter.next());
                    }

                    // Write another row with the track period and the price
                    {
                        XSSFRow trackPeriodAndPriceRow = this.fakturaSheet
                            .createRow(rowCounter.next());

                        cellCounter = rowCounter.getCellCounter();

                        String trackPeriodText = "  Gäller perioden "
                            + nextPeriod.getStartDayString()
                            + " till "
                            + nextPeriod.getEndDayString();

                        trackPeriodAndPriceRow
                            .createCell(cellCounter.next())
                            .setCellValue(trackPeriodText);

                        this.createPaddingCell(trackPeriodAndPriceRow, cellCounter.next());

                        double trackPrice;
                        if (customerInfo.isCompany()) {
                            trackPrice = SquashProperties.TRACK_PRICE_COMPANY;
                        } else {
                            trackPrice = SquashProperties.TRACK_PRICE_PERSON;
                        }

                        XSSFCell trackPriceCell = trackPeriodAndPriceRow
                            .createCell(cellCounter.next());
                        this.setLeftAlign(trackPriceCell);
                        trackPriceCell.setCellValue(trackPrice); // TODO, format!

                        totalPrice += trackPrice;
                    }

                    // One empty row between track rows
                    this.createPaddingRow(rowCounter.next());
                }
            }

            // Add some blank rows, to better match the A4 paper height
            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());
            this.createPaddingRow(rowCounter.next());
            XSSFRow lastTrackTableRow = this.createPaddingRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();
            this.createPaddingCell(lastTrackTableRow, cellCounter.next());
            this.createPaddingCell(lastTrackTableRow, cellCounter.next());
            XSSFCell lastTrackTableCell = this
                .createPaddingCell(lastTrackTableRow, cellCounter.next());

            // Add a border around the subscriptions table
            String trackTableCellRange = trackTableStartCellName
                + ":"
                + lastTrackTableCell.getAddress().formatAsString();

            this.addBorder(trackTableCellRange, true);

            // Now continue with some bottom ammount fields and payment info
            // ------------------------------------------------------------------------------------

            // Write the sum row
            XSSFRow sumRow = this.fakturaSheet.createRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();

            this.createPaddingCell(sumRow, cellCounter.next());

            XSSFCell sumTextCell = sumRow.createCell(cellCounter.next());
            sumTextCell.setCellValue("  Summa");
            this.setLeftAlign(sumRow.createCell(cellCounter.next())).setCellValue(totalPrice); // TODO, format!

            // Write the "moms" row, along with payment instructions box
            XSSFRow momsRow = this.fakturaSheet.createRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();

            XSSFCell paymentInfoCell = momsRow.createCell(cellCounter.next());
            paymentInfoCell.setCellValue("Bankgiro: " + SquashProperties.CLUB_BG_NR);
            this.setCenterAlign(paymentInfoCell);

            momsRow.createCell(cellCounter.next()).setCellValue("  Varav moms");

            this.setLeftAlign(momsRow.createCell(cellCounter.next())).setCellValue(0); // TODO, format!. Check if there ARE moms?

            // Write the row with the total ammount to pay
            XSSFRow ammountToPayRow = this.fakturaSheet.createRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();

            // Add the payment due date (no time parts), relative from "now", just below the BG nr
            Calendar dueCal = (Calendar) invoiceCreationCal.clone();
            dueCal.add(Calendar.DATE, dueDays);
            SquashUtil.timeZeroCalendar(dueCal);
            String dueDateString = new SimpleDateFormat(INVOICE_CREATION_DATE_FORMAT)
                .format(dueCal.getTime());

            XSSFCell paymentInfo2Cell = this
                .setBoldFont(ammountToPayRow.createCell(cellCounter.next()), false);
            paymentInfo2Cell.setCellValue("Förfallodag " + dueDateString);
            this.setCenterAlign(paymentInfo2Cell);

            this.setBoldFont(ammountToPayRow.createCell(cellCounter.next()), false).setCellValue(
                "  Att betala");

            XSSFCell totalAmmountCell = this
                .setBoldFont(ammountToPayRow.createCell(cellCounter.next()), false);
            totalAmmountCell.setCellValue(String.valueOf(totalPrice) + " kr"); // TODO, format!

            // Add border around the sum area
            String sumRange = sumTextCell.getAddress().formatAsString()
                + ":"
                + totalAmmountCell.getAddress().formatAsString();
            this.addBorder(sumRange, true);

            // Write a row with payment marking info
            XSSFRow markPaymentRow = this.fakturaSheet.createRow(rowCounter.next());
            cellCounter = rowCounter.getCellCounter();

            XSSFCell paymentInfo3Cell = this
                .setBoldFont(markPaymentRow.createCell(cellCounter.next()), false);
            paymentInfo3Cell.setCellValue("Märk betalningen med FakturaNr!");
            this.setCenterAlign(paymentInfo3Cell);

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

            gregorialCal.setTimeInMillis(nextPeriod.getStartDay().getTimeInMillis());
            invoice.setPeriodStartDate(datatypeFactory.newXMLGregorianCalendar(gregorialCal));

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

    // ------------------------------------------------------------------------------------
    // ---------------------    BELOW ARE HELPER METHODS AND SUCH    ----------------------
    // ------------------------------------------------------------------------------------

    // Adds bold font to a cell and possibly italic, builder pattern
    private XSSFCell setBoldFont(XSSFCell cell, boolean italic) {

        XSSFCellStyle style = this.excelWorkbook.createCellStyle();
        XSSFFont font = this.excelWorkbook.createFont();
        font.setBold(true);
        font.setItalic(italic);
        style.setFont(font);

        cell.setCellStyle(style);
        return cell;
    }

    // Adds text left alignment to a cell, builder pattern
    private XSSFCell setLeftAlign(XSSFCell cell) {
        XSSFCellStyle alignStyle = this.excelWorkbook.createCellStyle();
        alignStyle.setAlignment(CellStyle.ALIGN_LEFT);
        cell.setCellStyle(alignStyle);
        return cell;
    }

    // Adds text right alignment to a cell, builder pattern
    private XSSFCell setRightAlign(XSSFCell cell) {
        XSSFCellStyle alignStyle = this.excelWorkbook.createCellStyle();
        alignStyle.setAlignment(CellStyle.ALIGN_RIGHT);
        cell.setCellStyle(alignStyle);
        return cell;
    }

    // Adds text center alignment to a cell, builder pattern
    private XSSFCell setCenterAlign(XSSFCell cell) {
        XSSFCellStyle alignStyle = this.excelWorkbook.createCellStyle();
        alignStyle.setAlignment(CellStyle.ALIGN_CENTER);
        cell.setCellStyle(alignStyle);
        return cell;
    }

    // Sets given cell's value (if any) to an e-mail hyperlink text, builder pattern
    private XSSFCell setEmailLink(XSSFCell cell) {

        String cellValue = cell.getStringCellValue();
        if (cellValue != null && cellValue.contains("@")) {

            XSSFCellStyle hyperStyle = this.excelWorkbook.createCellStyle();
            XSSFFont hyperFont = this.excelWorkbook.createFont();
            hyperFont.setUnderline(Font.U_SINGLE);
            hyperFont.setColor(IndexedColors.BLUE.getIndex());
            hyperStyle.setFont(hyperFont);

            XSSFCreationHelper creationHelper = this.excelWorkbook.getCreationHelper();

            XSSFHyperlink link = creationHelper
                .createHyperlink(org.apache.poi.common.usermodel.Hyperlink.LINK_EMAIL);
            link.setAddress("mailto:" + cell.getStringCellValue());
            cell.setHyperlink(link);
            cell.setCellStyle(hyperStyle);
        }

        return cell;
    }

    // Adds a black border around a cell area, can also be once cell only
    private void addBorder(String cellRangeSpan, boolean thinnerBorder) {

        short borderStyle = (thinnerBorder ? CellStyle.BORDER_THIN : CellStyle.BORDER_MEDIUM);

        // The range is given as the format "A1:B4"
        CellRangeAddress cellRange = CellRangeAddress.valueOf(cellRangeSpan);
        RegionUtil.setBorderBottom(borderStyle, cellRange, this.fakturaSheet, this.excelWorkbook);
        RegionUtil.setBorderLeft(borderStyle, cellRange, this.fakturaSheet, this.excelWorkbook);
        RegionUtil.setBorderRight(borderStyle, cellRange, this.fakturaSheet, this.excelWorkbook);
        RegionUtil.setBorderTop(borderStyle, cellRange, this.fakturaSheet, this.excelWorkbook);
    }

    // Adds a new row, having one left padding column, so the next cell is always index 1
    private XSSFRow createNewRow(int rowNr) {
        XSSFRow row = this.fakturaSheet.createRow(rowNr);
        this.createLeftPaddingCell(row);
        return row;
    }

    // Adds an empty padding row
    private XSSFRow createPaddingRow(int rowNr) {
        XSSFRow row = this.fakturaSheet.createRow(rowNr);
        this.createLeftPaddingCell(row);
        return row;
    }

    // Adds a first, blank cell to a row - to give it some initial space from the left
    private XSSFCell createLeftPaddingCell(XSSFRow row) {
        XSSFCell firstCell = row.createCell(0);
        firstCell.setCellValue("    ");
        return firstCell;
    }

    // Adds an empty padding cell
    private XSSFCell createPaddingCell(XSSFRow row, int cellNr) {
        XSSFCell cell = row.createCell(cellNr);
        cell.setCellValue("    ");
        return cell;
    }

    private static class RowCounter {

        private static final int START_INDEX = 0;

        private int rowIndex;
        private CellCounter cellCounter;

        private RowCounter() {
            this.rowIndex = START_INDEX;
            this.cellCounter = new CellCounter();
        }

        private int current() {
            return this.rowIndex;
        }

        private int next() {
            this.cellCounter.reset();
            return ++this.rowIndex;
        }

        private CellCounter getCellCounter() {
            return this.cellCounter;
        }
    }

    private static class CellCounter {

        private static final int START_INDEX = 0;

        private int cellIndex;

        private CellCounter() {
            this.cellIndex = START_INDEX;
        }

        private int current() {
            return this.cellIndex;
        }

        private int next() {
            return ++this.cellIndex;
        }

        private void reset() {
            this.cellIndex = START_INDEX;
        }
    }
}
