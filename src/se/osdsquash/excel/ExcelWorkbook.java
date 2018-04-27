package se.osdsquash.excel;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

/**
 * Invoice XSSF Excel Workbook, having one sheet.
 * 
 * <p>
 * Access the sheet using the method getExcelSheet().
 * </p>
 */
public class ExcelWorkbook extends XSSFWorkbook {

    private ExcelSheet excelSheet;

    /**
     * Creates a new Excel workbook with one Invoice sheet
     * @param sheetTitle Name of the sheet/tab
     */
    protected ExcelWorkbook(String sheetTitle) {
        super(XSSFWorkbookType.XLSX);
        this.excelSheet = new ExcelSheet(super.createSheet(sheetTitle));
    }

    /**
     * Returns the sheet for this book
     * @return The sheet
     */
    protected ExcelSheet getExcelSheet() {
        return this.excelSheet;
    }
}
