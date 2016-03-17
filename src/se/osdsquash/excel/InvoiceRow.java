package se.osdsquash.excel;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow;

/**
 * Invoice Row class. Using a wrapped XSSFRow.
 */
public class InvoiceRow implements Row {

    // Initialize this before the first cell, which is 0
    private int cellColumnIndex = -1;

    private XSSFRow row;

    public InvoiceRow(XSSFRow row) {
        this.row = row;
    }

    public XSSFRow getRow() {
        return this.row;
    }

    /**
     * Creates a new cell by incrementing the row's cell index
     * @return A new cell
     */
    protected InvoiceCell createNextCell() {
        XSSFCell newCell = this.row.createCell(++this.cellColumnIndex);
        return new InvoiceCell(newCell);
    }

    // -------------------  BELOW ARE DELEGATE METHODS FOR THE WRAPPED ROW -------------------

    @Override
    public Iterator<Cell> cellIterator() {
        return this.row.cellIterator();
    }

    public int compareTo(XSSFRow other) {
        return this.row.compareTo(other);
    }

    public void copyRowFrom(Row srcRow, CellCopyPolicy policy) {
        this.row.copyRowFrom(srcRow, policy);
    }

    @Override
    public XSSFCell createCell(int columnIndex, int type) {
        return this.row.createCell(columnIndex, type);
    }

    @Override
    public XSSFCell createCell(int columnIndex) {
        return this.row.createCell(columnIndex);
    }

    @Override
    public boolean equals(Object obj) {
        return this.row.equals(obj);
    }

    @Override
    public void forEach(Consumer<? super Cell> action) {
        this.row.forEach(action);
    }

    public CTRow getCTRow() {
        return this.row.getCTRow();
    }

    @Override
    public XSSFCell getCell(int cellnum, MissingCellPolicy policy) {
        return this.row.getCell(cellnum, policy);
    }

    @Override
    public XSSFCell getCell(int cellnum) {
        return this.row.getCell(cellnum);
    }

    @Override
    public short getFirstCellNum() {
        return this.row.getFirstCellNum();
    }

    @Override
    public short getHeight() {
        return this.row.getHeight();
    }

    @Override
    public float getHeightInPoints() {
        return this.row.getHeightInPoints();
    }

    @Override
    public short getLastCellNum() {
        return this.row.getLastCellNum();
    }

    @Override
    public int getOutlineLevel() {
        return this.row.getOutlineLevel();
    }

    @Override
    public int getPhysicalNumberOfCells() {
        return this.row.getPhysicalNumberOfCells();
    }

    @Override
    public int getRowNum() {
        return this.row.getRowNum();
    }

    @Override
    public XSSFCellStyle getRowStyle() {
        return this.row.getRowStyle();
    }

    @Override
    public XSSFSheet getSheet() {
        return this.row.getSheet();
    }

    @Override
    public boolean getZeroHeight() {
        return this.row.getZeroHeight();
    }

    @Override
    public int hashCode() {
        return this.row.hashCode();
    }

    @Override
    public boolean isFormatted() {
        return this.row.isFormatted();
    }

    @Override
    public Iterator<Cell> iterator() {
        return this.row.iterator();
    }

    @Override
    public void removeCell(Cell cell) {
        this.row.removeCell(cell);
    }

    @Override
    public void setHeight(short height) {
        this.row.setHeight(height);
    }

    @Override
    public void setHeightInPoints(float height) {
        this.row.setHeightInPoints(height);
    }

    @Override
    public void setRowNum(int rowIndex) {
        this.row.setRowNum(rowIndex);
    }

    @Override
    public void setRowStyle(CellStyle style) {
        this.row.setRowStyle(style);
    }

    @Override
    public void setZeroHeight(boolean height) {
        this.row.setZeroHeight(height);
    }

    @Override
    public Spliterator<Cell> spliterator() {
        return this.row.spliterator();
    }

    @Override
    public String toString() {
        return this.row.toString();
    }
}
