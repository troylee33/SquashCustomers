package se.osdsquash.excel;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell;

/**
 * Invoice Cell class. Using a wrapped XSSFCell, since one can't extend the XSSFCell class!
 */
public class InvoiceCell implements Cell {

    private static final Locale SWE_LOCALE = new Locale("sv", "SE");
    private static final String EXCEL_CURRENCY_FORMAT = "# ##0,00 kr";

    private XSSFCell cell;

    protected InvoiceCell(XSSFCell cell) {
        this.cell = cell;
    }

    protected XSSFCell getCell() {
        return this.cell;
    }

    /**
     * Sets the cell's value (if any) to an e-mail hyperlink text with correct styling
     */
    protected void applyEmailLink() {

        XSSFWorkbook currentWorkbook = this.cell.getSheet().getWorkbook();

        String cellValue = this.cell.getStringCellValue();
        if (cellValue != null && cellValue.contains("@")) {

            XSSFCellStyle hyperStyle = currentWorkbook.createCellStyle();
            XSSFFont hyperFont = currentWorkbook.createFont();
            hyperFont.setUnderline(Font.U_SINGLE);
            hyperFont.setColor(IndexedColors.BLUE.getIndex());
            hyperStyle.setFont(hyperFont);

            XSSFCreationHelper creationHelper = currentWorkbook.getCreationHelper();

            XSSFHyperlink link = creationHelper
                .createHyperlink(org.apache.poi.common.usermodel.Hyperlink.LINK_EMAIL);
            link.setAddress("mailto:" + this.cell.getStringCellValue());
            this.cell.setHyperlink(link);
            this.cell.setCellStyle(hyperStyle);
        }
    }

    /**
     * Set cell's value to given ammount in Swedish "Kr" currency format.
     * Alignment and bold options also possible.
     * 
     * @param ammount The ammount to set as currency
     * @param rightAlign True to right align, false to not alter alignment
     * @param bold True to use bold font, false to not alter font
     */
    protected void setCurrencyFormat(double ammount, boolean rightAlign, boolean bold) {

        // Double-safety, format to currency in Java first...
        NumberFormat swedishFormat = NumberFormat.getCurrencyInstance(SWE_LOCALE);
        this.cell.setCellValue(swedishFormat.format(ammount));

        // ...and set the same Excel cell format, so Excel won't warn about the cell's format
        XSSFWorkbook currentWorkbook = this.cell.getSheet().getWorkbook();
        XSSFCellStyle currencyStyle = currentWorkbook.createCellStyle();

        if (rightAlign) {
            currencyStyle.setAlignment(CellStyle.ALIGN_RIGHT);
        }

        if (bold) {
            XSSFFont font = currentWorkbook.createFont();
            font.setBold(bold);
            currencyStyle.setFont(font);
        }

        currencyStyle.setDataFormat(
            currentWorkbook
                .getCreationHelper()
                .createDataFormat()
                .getFormat(EXCEL_CURRENCY_FORMAT));

        this.cell.setCellStyle(currencyStyle);
    }

    /**
     * Sets the cell's font styles according to given markers
     * 
     * @param bold True if to set bold font style
     * @param italic True if to set italic font style
     * @param centerAlign True to align text in the center
     */
    protected void applyFontStyles(boolean bold, boolean italic, boolean centerAlign) {

        XSSFWorkbook currentWorkbook = this.cell.getSheet().getWorkbook();

        XSSFCellStyle style = currentWorkbook.createCellStyle();
        XSSFFont font = currentWorkbook.createFont();
        font.setBold(bold);
        font.setItalic(italic);
        style.setFont(font);
        if (centerAlign) {
            style.setAlignment(CellStyle.ALIGN_CENTER);
        }

        this.cell.setCellStyle(style);
    }

    /**
     * Sets the cell's alignment
     * @param align value, see <code>org.apache.poi.ss.usermodel.CellStyle</code> for constant values
     */
    protected void setAlignment(short align) {

        XSSFWorkbook currentWorkbook = this.cell.getSheet().getWorkbook();

        XSSFCellStyle alignStyle = currentWorkbook.createCellStyle();
        alignStyle.setAlignment(align);
        this.cell.setCellStyle(alignStyle);
    }

    // -------------------  BELOW ARE DELEGATE METHODS FOR THE WRAPPED CELL -------------------

    @Override
    public int hashCode() {
        return this.cell.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.cell.equals(obj);
    }

    public void copyCellFrom(Cell srcCell, CellCopyPolicy policy) {
        this.cell.copyCellFrom(srcCell, policy);
    }

    @Override
    public XSSFSheet getSheet() {
        return this.cell.getSheet();
    }

    @Override
    public XSSFRow getRow() {
        return this.cell.getRow();
    }

    @Override
    public boolean getBooleanCellValue() {
        return this.cell.getBooleanCellValue();
    }

    @Override
    public void setCellValue(boolean value) {
        this.cell.setCellValue(value);
    }

    @Override
    public double getNumericCellValue() {
        return this.cell.getNumericCellValue();
    }

    @Override
    public void setCellValue(double value) {
        this.cell.setCellValue(value);
    }

    @Override
    public String getStringCellValue() {
        return this.cell.getStringCellValue();
    }

    @Override
    public XSSFRichTextString getRichStringCellValue() {
        return this.cell.getRichStringCellValue();
    }

    @Override
    public void setCellValue(String str) {
        this.cell.setCellValue(str);
    }

    @Override
    public void setCellValue(RichTextString str) {
        this.cell.setCellValue(str);
    }

    @Override
    public String getCellFormula() {
        return this.cell.getCellFormula();
    }

    @Override
    public void setCellFormula(String formula) {
        this.cell.setCellFormula(formula);
    }

    @Override
    public int getColumnIndex() {
        return this.cell.getColumnIndex();
    }

    @Override
    public int getRowIndex() {
        return this.cell.getRowIndex();
    }

    public String getReference() {
        return this.cell.getReference();
    }

    @Override
    public CellAddress getAddress() {
        return this.cell.getAddress();
    }

    @Override
    public XSSFCellStyle getCellStyle() {
        return this.cell.getCellStyle();
    }

    @Override
    public void setCellStyle(CellStyle style) {
        this.cell.setCellStyle(style);
    }

    @Override
    public int getCellType() {
        return this.cell.getCellType();
    }

    @Override
    public int getCachedFormulaResultType() {
        return this.cell.getCachedFormulaResultType();
    }

    @Override
    public Date getDateCellValue() {
        return this.cell.getDateCellValue();
    }

    @Override
    public void setCellValue(Date value) {
        this.cell.setCellValue(value);
    }

    @Override
    public void setCellValue(Calendar value) {
        this.cell.setCellValue(value);
    }

    public String getErrorCellString() {
        return this.cell.getErrorCellString();
    }

    @Override
    public byte getErrorCellValue() {
        return this.cell.getErrorCellValue();
    }

    @Override
    public void setCellErrorValue(byte errorCode) {
        this.cell.setCellErrorValue(errorCode);
    }

    public void setCellErrorValue(FormulaError error) {
        this.cell.setCellErrorValue(error);
    }

    @Override
    public void setAsActiveCell() {
        this.cell.setAsActiveCell();
    }

    @Override
    public void setCellType(int cellType) {
        this.cell.setCellType(cellType);
    }

    @Override
    public String toString() {
        return this.cell.toString();
    }

    public String getRawValue() {
        return this.cell.getRawValue();
    }

    @Override
    public XSSFComment getCellComment() {
        return this.cell.getCellComment();
    }

    @Override
    public void setCellComment(Comment comment) {
        this.cell.setCellComment(comment);
    }

    @Override
    public void removeCellComment() {
        this.cell.removeCellComment();
    }

    @Override
    public XSSFHyperlink getHyperlink() {
        return this.cell.getHyperlink();
    }

    @Override
    public void setHyperlink(Hyperlink hyperlink) {
        this.cell.setHyperlink(hyperlink);
    }

    @Override
    public void removeHyperlink() {
        this.cell.removeHyperlink();
    }

    public CTCell getCTCell() {
        return this.cell.getCTCell();
    }

    public void setCTCell(CTCell cell) {
        this.cell.setCTCell(cell);
    }

    @Override
    public CellRangeAddress getArrayFormulaRange() {
        return this.cell.getArrayFormulaRange();
    }

    @Override
    public boolean isPartOfArrayFormulaGroup() {
        return this.cell.isPartOfArrayFormulaGroup();
    }
}
