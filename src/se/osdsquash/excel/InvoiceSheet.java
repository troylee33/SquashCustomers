package se.osdsquash.excel;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.POIXMLDocumentPart.RelationPart;
import org.apache.poi.POIXMLFactory;
import org.apache.poi.POIXMLRelation;
import org.apache.poi.hssf.util.PaneInformation;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellRange;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.IgnoredErrorType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFAutoFilter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFPrintSetup;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.helpers.ColumnHelper;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;

/**
 * Invoice Row class. Using a wrapped XSSFSheet.
 * 
 * <p>
 * This sheet can create new rows like the iterator pattern
 * using the method createNextRow(), which will step through
 * row creation from top to bottom.
 * </p>
 */
public class InvoiceSheet implements Sheet {

    // Initialize this before the first row, which is 0
    private int rowIndex = -1;

    private XSSFSheet sheet;

    protected InvoiceSheet(XSSFSheet sheet) {
        this.sheet = sheet;
    }

    /**
     * Returns current row index for this sheet
     * @return The index of the last created row, -1 if no rows
     */
    protected int currentRowIndex() {
        return this.rowIndex;
    }

    /**
     * Creates a new row by incrementing the sheet's row index
     * @return A new row
     */
    protected InvoiceRow createNextRow() {
        XSSFRow row = this.sheet.createRow(++this.rowIndex);
        return new InvoiceRow(row);
    }

    /**
     * Adds a new row as the next row, already having one left padding column added
     * @return A new row, having one cell already
     */
    protected InvoiceRow createNextPaddedRow() {
        InvoiceRow row = this.createNextRow();
        row.createNextCell().setCellValue("    ");
        return row;
    }

    // -------------------  BELOW ARE DELEGATE METHODS FOR THE WRAPPED SHEET -------------------

    @Override
    public Spliterator<Row> spliterator() {
        return this.sheet.spliterator();
    }

    @Override
    public boolean equals(Object obj) {
        return this.sheet.equals(obj);
    }

    @Override
    public int addMergedRegion(CellRangeAddress region) {
        return this.sheet.addMergedRegion(region);
    }

    public int addMergedRegionUnsafe(CellRangeAddress region) {
        return this.sheet.addMergedRegionUnsafe(region);
    }

    @Override
    public void autoSizeColumn(int column) {
        this.sheet.autoSizeColumn(column);
    }

    @Override
    public void autoSizeColumn(int column, boolean useMergedCells) {
        this.sheet.autoSizeColumn(column, useMergedCells);
    }

    @Override
    public XSSFDrawing createDrawingPatriarch() {
        return this.sheet.createDrawingPatriarch();
    }

    @Override
    public void createFreezePane(int colSplit, int rowSplit) {
        this.sheet.createFreezePane(colSplit, rowSplit);
    }

    @Override
    public void createFreezePane(int colSplit, int rowSplit, int leftmostColumn, int topRow) {
        this.sheet.createFreezePane(colSplit, rowSplit, leftmostColumn, topRow);
    }

    @Override
    public XSSFRow createRow(int rownum) {
        return this.sheet.createRow(rownum);
    }

    @Override
    public void createSplitPane(
        int xSplitPos,
        int ySplitPos,
        int leftmostColumn,
        int topRow,
        int activePane) {
        this.sheet.createSplitPane(xSplitPos, ySplitPos, leftmostColumn, topRow, activePane);
    }

    @Override
    public XSSFHyperlink getHyperlink(int row, int column) {
        return this.sheet.getHyperlink(row, column);
    }

    @Override
    public List<XSSFHyperlink> getHyperlinkList() {
        return this.sheet.getHyperlinkList();
    }

    @Override
    public int getFirstRowNum() {
        return this.sheet.getFirstRowNum();
    }

    @Override
    public boolean getFitToPage() {
        return this.sheet.getFitToPage();
    }

    @Override
    public Footer getFooter() {
        return this.sheet.getFooter();
    }

    @Override
    public Header getHeader() {
        return this.sheet.getHeader();
    }

    public Footer getOddFooter() {
        return this.sheet.getOddFooter();
    }

    public Header getOddHeader() {
        return this.sheet.getOddHeader();
    }

    @Override
    public boolean getHorizontallyCenter() {
        return this.sheet.getHorizontallyCenter();
    }

    @Override
    public int getLastRowNum() {
        return this.sheet.getLastRowNum();
    }

    @Override
    public short getLeftCol() {
        return this.sheet.getLeftCol();
    }

    @Override
    public double getMargin(short margin) {
        return this.sheet.getMargin(margin);
    }

    @Override
    public CellRangeAddress getMergedRegion(int index) {
        return this.sheet.getMergedRegion(index);
    }

    @Override
    public List<CellRangeAddress> getMergedRegions() {
        return this.sheet.getMergedRegions();
    }

    @Override
    public int getNumMergedRegions() {
        return this.sheet.getNumMergedRegions();
    }

    public int getNumHyperlinks() {
        return this.sheet.getNumHyperlinks();
    }

    @Override
    public int getPhysicalNumberOfRows() {
        return this.sheet.getPhysicalNumberOfRows();
    }

    @Override
    public XSSFPrintSetup getPrintSetup() {
        return this.sheet.getPrintSetup();
    }

    @Override
    public boolean getForceFormulaRecalculation() {
        return this.sheet.getForceFormulaRecalculation();
    }

    public Map<IgnoredErrorType, Set<CellRangeAddress>> getIgnoredErrors() {
        return this.sheet.getIgnoredErrors();
    }

    public final PackagePart getPackagePart() {
        return this.sheet.getPackagePart();
    }

    @SuppressWarnings("deprecation")
    public final PackageRelationship getPackageRelationship() {
        return this.sheet.getPackageRelationship();
    }

    @Override
    public PaneInformation getPaneInformation() {
        return this.sheet.getPaneInformation();
    }

    public final POIXMLDocumentPart getParent() {
        return this.sheet.getParent();
    }

    @Override
    public boolean getProtect() {
        return this.sheet.getProtect();
    }

    public final POIXMLDocumentPart getRelationById(String id) {
        return this.sheet.getRelationById(id);
    }

    public final String getRelationId(POIXMLDocumentPart part) {
        return this.sheet.getRelationId(part);
    }

    public final List<RelationPart> getRelationParts() {
        return this.sheet.getRelationParts();
    }

    public final List<POIXMLDocumentPart> getRelations() {
        return this.sheet.getRelations();
    }

    @Override
    public int hashCode() {
        return this.sheet.hashCode();
    }

    @Override
    public XSSFWorkbook getWorkbook() {
        return this.sheet.getWorkbook();
    }

    @Override
    public String getSheetName() {
        return this.sheet.getSheetName();
    }

    @Override
    public XSSFRow getRow(int rownum) {
        return this.sheet.getRow(rownum);
    }

    @Override
    public int[] getRowBreaks() {
        return this.sheet.getRowBreaks();
    }

    @Override
    public boolean getRowSumsBelow() {
        return this.sheet.getRowSumsBelow();
    }

    @Override
    public boolean getRowSumsRight() {
        return this.sheet.getRowSumsRight();
    }

    @Override
    public boolean getScenarioProtect() {
        return this.sheet.getScenarioProtect();
    }

    @Override
    public short getTopRow() {
        return this.sheet.getTopRow();
    }

    @Override
    public boolean getVerticallyCenter() {
        return this.sheet.getVerticallyCenter();
    }

    @Override
    public void groupColumn(int fromColumn, int toColumn) {
        this.sheet.groupColumn(fromColumn, toColumn);
    }

    @Override
    public void groupRow(int fromRow, int toRow) {
        this.sheet.groupRow(fromRow, toRow);
    }

    @Override
    public boolean isColumnBroken(int column) {
        return this.sheet.isColumnBroken(column);
    }

    public void copyRows(List<? extends Row> srcRows, int destStartRow, CellCopyPolicy policy) {
        this.sheet.copyRows(srcRows, destStartRow, policy);
    }

    public void copyRows(
        int srcStartRow,
        int srcEndRow,
        int destStartRow,
        CellCopyPolicy cellCopyPolicy) {
        this.sheet.copyRows(srcStartRow, srcEndRow, destStartRow, cellCopyPolicy);
    }

    public void addHyperlink(XSSFHyperlink hyperlink) {
        this.sheet.addHyperlink(hyperlink);
    }

    public boolean hasComments() {
        return this.sheet.hasComments();
    }

    public CTCellFormula getSharedFormula(int sid) {
        return this.sheet.getSharedFormula(sid);
    }

    public boolean isAutoFilterLocked() {
        return this.sheet.isAutoFilterLocked();
    }

    public void enableLocking() {
        this.sheet.enableLocking();
    }

    public void disableLocking() {
        this.sheet.disableLocking();
    }

    @Override
    public void addValidationData(DataValidation dataValidation) {
        this.sheet.addValidationData(dataValidation);
    }

    public XSSFTable createTable() {
        return this.sheet.createTable();
    }

    public List<XSSFTable> getTables() {
        return this.sheet.getTables();
    }

    @Override
    public XSSFSheetConditionalFormatting getSheetConditionalFormatting() {
        return this.sheet.getSheetConditionalFormatting();
    }

    @Override
    public CellRangeAddress getRepeatingRows() {
        return this.sheet.getRepeatingRows();
    }

    @Override
    public CellRangeAddress getRepeatingColumns() {
        return this.sheet.getRepeatingColumns();
    }

    public XSSFPivotTable createPivotTable(
        AreaReference source,
        CellReference position,
        Sheet sourceSheet) {
        return this.sheet.createPivotTable(source, position, sourceSheet);
    }

    public XSSFPivotTable createPivotTable(AreaReference source, CellReference position) {
        return this.sheet.createPivotTable(source, position);
    }

    public List<XSSFPivotTable> getPivotTables() {
        return this.sheet.getPivotTables();
    }

    public void addIgnoredErrors(CellReference cell, IgnoredErrorType... ignoredErrorTypes) {
        this.sheet.addIgnoredErrors(cell, ignoredErrorTypes);
    }

    public void addIgnoredErrors(CellRangeAddress region, IgnoredErrorType... ignoredErrorTypes) {
        this.sheet.addIgnoredErrors(region, ignoredErrorTypes);
    }

    @SuppressWarnings("deprecation")
    public final void addRelation(String id, POIXMLDocumentPart part) {
        this.sheet.addRelation(id, part);
    }

    public final RelationPart addRelation(
        String relId,
        POIXMLRelation relationshipType,
        POIXMLDocumentPart part) {
        return this.sheet.addRelation(relId, relationshipType, part);
    }

    public final POIXMLDocumentPart createRelationship(
        POIXMLRelation descriptor,
        POIXMLFactory factory,
        int idx) {
        return this.sheet.createRelationship(descriptor, factory, idx);
    }

    public final POIXMLDocumentPart createRelationship(
        POIXMLRelation descriptor,
        POIXMLFactory factory) {
        return this.sheet.createRelationship(descriptor, factory);
    }

    @Override
    public void forEach(Consumer<? super Row> action) {
        this.sheet.forEach(action);
    }

    public CTWorksheet getCTWorksheet() {
        return this.sheet.getCTWorksheet();
    }

    public ColumnHelper getColumnHelper() {
        return this.sheet.getColumnHelper();
    }

    public void validateMergedRegions() {
        this.sheet.validateMergedRegions();
    }

    @Override
    public XSSFDrawing getDrawingPatriarch() {
        return this.sheet.getDrawingPatriarch();
    }

    @SuppressWarnings("deprecation")
    @Override
    public XSSFComment getCellComment(int row, int column) {
        return this.sheet.getCellComment(row, column);
    }

    @Override
    public XSSFComment getCellComment(CellAddress address) {
        return this.sheet.getCellComment(address);
    }

    @Override
    public Map<CellAddress, XSSFComment> getCellComments() {
        return this.sheet.getCellComments();
    }

    @Override
    public int[] getColumnBreaks() {
        return this.sheet.getColumnBreaks();
    }

    @Override
    public int getColumnWidth(int columnIndex) {
        return this.sheet.getColumnWidth(columnIndex);
    }

    @Override
    public float getColumnWidthInPixels(int columnIndex) {
        return this.sheet.getColumnWidthInPixels(columnIndex);
    }

    @Override
    public int getDefaultColumnWidth() {
        return this.sheet.getDefaultColumnWidth();
    }

    @Override
    public short getDefaultRowHeight() {
        return this.sheet.getDefaultRowHeight();
    }

    @Override
    public float getDefaultRowHeightInPoints() {
        return this.sheet.getDefaultRowHeightInPoints();
    }

    @Override
    public CellStyle getColumnStyle(int column) {
        return this.sheet.getColumnStyle(column);
    }

    @Override
    public void setRightToLeft(boolean value) {
        this.sheet.setRightToLeft(value);
    }

    @Override
    public boolean isRightToLeft() {
        return this.sheet.isRightToLeft();
    }

    @Override
    public boolean getDisplayGuts() {
        return this.sheet.getDisplayGuts();
    }

    @Override
    public void setDisplayGuts(boolean value) {
        this.sheet.setDisplayGuts(value);
    }

    @Override
    public boolean isDisplayZeros() {
        return this.sheet.isDisplayZeros();
    }

    @Override
    public void setDisplayZeros(boolean value) {
        this.sheet.setDisplayZeros(value);
    }

    public Footer getEvenFooter() {
        return this.sheet.getEvenFooter();
    }

    public Footer getFirstFooter() {
        return this.sheet.getFirstFooter();
    }

    public Header getEvenHeader() {
        return this.sheet.getEvenHeader();
    }

    public Header getFirstHeader() {
        return this.sheet.getFirstHeader();
    }

    @Override
    public void setMargin(short margin, double size) {
        this.sheet.setMargin(margin, size);
    }

    @Override
    public void protectSheet(String password) {
        this.sheet.protectSheet(password);
    }

    public void setSheetPassword(String password, HashAlgorithm hashAlgo) {
        this.sheet.setSheetPassword(password, hashAlgo);
    }

    public boolean validateSheetPassword(String password) {
        return this.sheet.validateSheetPassword(password);
    }

    @Override
    public void setRowSumsBelow(boolean value) {
        this.sheet.setRowSumsBelow(value);
    }

    @Override
    public void setRowSumsRight(boolean value) {
        this.sheet.setRowSumsRight(value);
    }

    @Override
    public boolean isColumnHidden(int columnIndex) {
        return this.sheet.isColumnHidden(columnIndex);
    }

    @Override
    public boolean isDisplayFormulas() {
        return this.sheet.isDisplayFormulas();
    }

    @Override
    public boolean isDisplayGridlines() {
        return this.sheet.isDisplayGridlines();
    }

    @Override
    public void setDisplayGridlines(boolean show) {
        this.sheet.setDisplayGridlines(show);
    }

    @Override
    public boolean isDisplayRowColHeadings() {
        return this.sheet.isDisplayRowColHeadings();
    }

    @Override
    public void setDisplayRowColHeadings(boolean show) {
        this.sheet.setDisplayRowColHeadings(show);
    }

    @Override
    public boolean isPrintGridlines() {
        return this.sheet.isPrintGridlines();
    }

    @Override
    public void setPrintGridlines(boolean value) {
        this.sheet.setPrintGridlines(value);
    }

    @Override
    public boolean isRowBroken(int row) {
        return this.sheet.isRowBroken(row);
    }

    @Override
    public void setRowBreak(int row) {
        this.sheet.setRowBreak(row);
    }

    @Override
    public void removeColumnBreak(int column) {
        this.sheet.removeColumnBreak(column);
    }

    @Override
    public void removeMergedRegion(int index) {
        this.sheet.removeMergedRegion(index);
    }

    public void removeMergedRegions(Collection<Integer> indices) {
        this.sheet.removeMergedRegions(indices);
    }

    @Override
    public void removeRow(Row row) {
        this.sheet.removeRow(row);
    }

    @Override
    public void removeRowBreak(int row) {
        this.sheet.removeRowBreak(row);
    }

    @Override
    public void setForceFormulaRecalculation(boolean value) {
        this.sheet.setForceFormulaRecalculation(value);
    }

    @Override
    public Iterator<Row> rowIterator() {
        return this.sheet.rowIterator();
    }

    @Override
    public Iterator<Row> iterator() {
        return this.sheet.iterator();
    }

    @Override
    public boolean getAutobreaks() {
        return this.sheet.getAutobreaks();
    }

    @Override
    public void setAutobreaks(boolean value) {
        this.sheet.setAutobreaks(value);
    }

    @Override
    public void setColumnBreak(int column) {
        this.sheet.setColumnBreak(column);
    }

    @Override
    public void setColumnGroupCollapsed(int columnNumber, boolean collapsed) {
        this.sheet.setColumnGroupCollapsed(columnNumber, collapsed);
    }

    @Override
    public void setColumnHidden(int columnIndex, boolean hidden) {
        this.sheet.setColumnHidden(columnIndex, hidden);
    }

    @Override
    public void setColumnWidth(int columnIndex, int width) {
        this.sheet.setColumnWidth(columnIndex, width);
    }

    @Override
    public void setDefaultColumnStyle(int column, CellStyle style) {
        this.sheet.setDefaultColumnStyle(column, style);
    }

    @Override
    public void setDefaultColumnWidth(int width) {
        this.sheet.setDefaultColumnWidth(width);
    }

    @Override
    public void setDefaultRowHeight(short height) {
        this.sheet.setDefaultRowHeight(height);
    }

    @Override
    public void setDefaultRowHeightInPoints(float height) {
        this.sheet.setDefaultRowHeightInPoints(height);
    }

    @Override
    public void setDisplayFormulas(boolean show) {
        this.sheet.setDisplayFormulas(show);
    }

    @Override
    public void setFitToPage(boolean b) {
        this.sheet.setFitToPage(b);
    }

    @Override
    public void setHorizontallyCenter(boolean value) {
        this.sheet.setHorizontallyCenter(value);
    }

    @Override
    public void setVerticallyCenter(boolean value) {
        this.sheet.setVerticallyCenter(value);
    }

    @Override
    public void setRowGroupCollapsed(int rowIndex, boolean collapse) {
        this.sheet.setRowGroupCollapsed(rowIndex, collapse);
    }

    public int findEndOfRowOutlineGroup(int row) {
        return this.sheet.findEndOfRowOutlineGroup(row);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setZoom(int numerator, int denominator) {
        this.sheet.setZoom(numerator, denominator);
    }

    @Override
    public void setZoom(int scale) {
        this.sheet.setZoom(scale);
    }

    @Override
    public void shiftRows(int startRow, int endRow, int n) {
        this.sheet.shiftRows(startRow, endRow, n);
    }

    @Override
    public void shiftRows(
        int startRow,
        int endRow,
        int n,
        boolean copyRowHeight,
        boolean resetOriginalRowHeight) {
        this.sheet.shiftRows(startRow, endRow, n, copyRowHeight, resetOriginalRowHeight);
    }

    @Override
    public void showInPane(int toprow, int leftcol) {
        this.sheet.showInPane(toprow, leftcol);
    }

    @Override
    public void ungroupColumn(int fromColumn, int toColumn) {
        this.sheet.ungroupColumn(fromColumn, toColumn);
    }

    @Override
    public void ungroupRow(int fromRow, int toRow) {
        this.sheet.ungroupRow(fromRow, toRow);
    }

    @Override
    public boolean isSelected() {
        return this.sheet.isSelected();
    }

    @Override
    public void setSelected(boolean value) {
        this.sheet.setSelected(value);
    }

    public void removeHyperlink(int row, int column) {
        this.sheet.removeHyperlink(row, column);
    }

    @Override
    public CellAddress getActiveCell() {
        return this.sheet.getActiveCell();
    }

    @SuppressWarnings("deprecation")
    public void setActiveCell(String cellRef) {
        this.sheet.setActiveCell(cellRef);
    }

    @Override
    public void setActiveCell(CellAddress address) {
        this.sheet.setActiveCell(address);
    }

    public boolean isDeleteColumnsLocked() {
        return this.sheet.isDeleteColumnsLocked();
    }

    public boolean isDeleteRowsLocked() {
        return this.sheet.isDeleteRowsLocked();
    }

    public boolean isFormatCellsLocked() {
        return this.sheet.isFormatCellsLocked();
    }

    public boolean isFormatColumnsLocked() {
        return this.sheet.isFormatColumnsLocked();
    }

    public boolean isFormatRowsLocked() {
        return this.sheet.isFormatRowsLocked();
    }

    public boolean isInsertColumnsLocked() {
        return this.sheet.isInsertColumnsLocked();
    }

    public boolean isInsertHyperlinksLocked() {
        return this.sheet.isInsertHyperlinksLocked();
    }

    public boolean isInsertRowsLocked() {
        return this.sheet.isInsertRowsLocked();
    }

    public boolean isPivotTablesLocked() {
        return this.sheet.isPivotTablesLocked();
    }

    public boolean isSortLocked() {
        return this.sheet.isSortLocked();
    }

    public boolean isObjectsLocked() {
        return this.sheet.isObjectsLocked();
    }

    public boolean isScenariosLocked() {
        return this.sheet.isScenariosLocked();
    }

    public boolean isSelectLockedCellsLocked() {
        return this.sheet.isSelectLockedCellsLocked();
    }

    public boolean isSelectUnlockedCellsLocked() {
        return this.sheet.isSelectUnlockedCellsLocked();
    }

    public boolean isSheetLocked() {
        return this.sheet.isSheetLocked();
    }

    public void lockAutoFilter(boolean enabled) {
        this.sheet.lockAutoFilter(enabled);
    }

    public void lockDeleteColumns(boolean enabled) {
        this.sheet.lockDeleteColumns(enabled);
    }

    public void lockDeleteRows(boolean enabled) {
        this.sheet.lockDeleteRows(enabled);
    }

    public void lockFormatCells(boolean enabled) {
        this.sheet.lockFormatCells(enabled);
    }

    public void lockFormatColumns(boolean enabled) {
        this.sheet.lockFormatColumns(enabled);
    }

    public void lockFormatRows(boolean enabled) {
        this.sheet.lockFormatRows(enabled);
    }

    public void lockInsertColumns(boolean enabled) {
        this.sheet.lockInsertColumns(enabled);
    }

    public void lockInsertHyperlinks(boolean enabled) {
        this.sheet.lockInsertHyperlinks(enabled);
    }

    public void lockInsertRows(boolean enabled) {
        this.sheet.lockInsertRows(enabled);
    }

    public void lockPivotTables(boolean enabled) {
        this.sheet.lockPivotTables(enabled);
    }

    public void lockSort(boolean enabled) {
        this.sheet.lockSort(enabled);
    }

    public void lockObjects(boolean enabled) {
        this.sheet.lockObjects(enabled);
    }

    public void lockScenarios(boolean enabled) {
        this.sheet.lockScenarios(enabled);
    }

    public void lockSelectLockedCells(boolean enabled) {
        this.sheet.lockSelectLockedCells(enabled);
    }

    public void lockSelectUnlockedCells(boolean enabled) {
        this.sheet.lockSelectUnlockedCells(enabled);
    }

    @Override
    public CellRange<XSSFCell> setArrayFormula(String formula, CellRangeAddress range) {
        return this.sheet.setArrayFormula(formula, range);
    }

    @Override
    public CellRange<XSSFCell> removeArrayFormula(Cell cell) {
        return this.sheet.removeArrayFormula(cell);
    }

    @Override
    public DataValidationHelper getDataValidationHelper() {
        return this.sheet.getDataValidationHelper();
    }

    @Override
    public List<XSSFDataValidation> getDataValidations() {
        return this.sheet.getDataValidations();
    }

    @Override
    public XSSFAutoFilter setAutoFilter(CellRangeAddress range) {
        return this.sheet.setAutoFilter(range);
    }

    public void setTabColor(int colorIndex) {
        this.sheet.setTabColor(colorIndex);
    }

    @Override
    public void setRepeatingRows(CellRangeAddress rowRangeRef) {
        this.sheet.setRepeatingRows(rowRangeRef);
    }

    @Override
    public void setRepeatingColumns(CellRangeAddress columnRangeRef) {
        this.sheet.setRepeatingColumns(columnRangeRef);
    }

    @Override
    public int getColumnOutlineLevel(int columnIndex) {
        return this.sheet.getColumnOutlineLevel(columnIndex);
    }

    @Override
    public String toString() {
        return this.sheet.toString();
    }
}
