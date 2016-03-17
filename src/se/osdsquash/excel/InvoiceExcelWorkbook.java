package se.osdsquash.excel;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

/**
 * Invoice XSSF Excel Workbook, having one sheet for the invoice.
 * 
 * <p>
 * Access the sheet using the method getInvoiceSheet().
 * </p>
 */
public class InvoiceExcelWorkbook extends XSSFWorkbook {

    private InvoiceSheet invoiceSheet;

    /**
     * Creates a new Excel workbook with one Invoice sheet
     */
    protected InvoiceExcelWorkbook() {
        super(XSSFWorkbookType.XLSX);
        this.invoiceSheet = new InvoiceSheet(super.createSheet("Faktura"));
    }

    /**
     * Returns the sheet for this book - custom made for the invoice
     * @return The sheet
     */
    protected InvoiceSheet getInvoiceSheet() {
        return this.invoiceSheet;
    }
}
