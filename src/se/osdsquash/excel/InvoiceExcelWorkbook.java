package se.osdsquash.excel;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

/**
 * Invoice XSSF Excel Workbook, having one sheet for the invoice
 */
public class InvoiceExcelWorkbook extends XSSFWorkbook {

    private XSSFSheet invoiceSheet;

    // Initialize this before the first row, which is 0
    private int rowIndex = -1;

    protected InvoiceExcelWorkbook() {
        super(XSSFWorkbookType.XLSX);
        this.invoiceSheet = super.createSheet("Faktura");
    }

    protected InvoiceRow createNextRow() {
        XSSFRow row = this.invoiceSheet.createRow(++this.rowIndex);
        return new InvoiceRow(row);
    }
}
