package se.osdsquash.gui;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import se.osdsquash.gui.InvoicesTable.InvoiceTableModel;

/**
 * Synchronized class that can listen and track GUI component changes
 * becoming "dirty",  e.g. modified and unsaved, otherwise "clean".
 * 
 * <p>
 * This class implements all change listeners needed for all our editable components.
 * </p>
 */
public class DirtyMarker implements DocumentListener, ChangeListener, TableModelListener {

    private AtomicBoolean dirtyBoolean;

    // Set true if you need to debug dirty tracking:
    private boolean debugMode = false;

    /**
     * Creates the marker as clean and not dirty
     */
    protected DirtyMarker() {
        this.dirtyBoolean = new AtomicBoolean(false);
    }

    protected void setDirty() {
        this.dirtyBoolean.set(true);
    }

    protected void setClean() {
        this.dirtyBoolean.set(false);
        if (this.debugMode) {
            System.out.println("setClean called");
        }
    }

    public boolean isDirty() {
        return this.dirtyBoolean.get();
    }

    //  This handles JCheckBox change events - the ChangeListener
    // -----------------------------------------------------------
    @Override
    public void stateChanged(ChangeEvent e) {
        this.setDirty();
        if (this.debugMode) {
            System.out.println("stateChanged called");
        }
    }

    //  This handles JTextField change events - the DocumentListener
    // --------------------------------------------------------------
    @Override
    public void insertUpdate(DocumentEvent e) {
        this.setDirty();
        if (this.debugMode) {
            System.out.println("insertUpdate called");
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        this.setDirty();
        if (this.debugMode) {
            System.out.println("removeUpdate called");
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        this.setDirty();
        if (this.debugMode) {
            System.out.println("changedUpdate called");
        }
    }

    //  This handles JTable change events - the TableModelListener
    // ------------------------------------------------------------
    @Override
    public void tableChanged(TableModelEvent e) {

        // Hack to prevent an invoice deletion to mark customer dirty.
        // Such a delete doesn't need a Save, it is already done.
        if (e.getType() == TableModelEvent.DELETE
            && e.getSource().getClass().equals(InvoiceTableModel.class)) {
            if (this.debugMode) {
                System.out.println(
                    "tableChanged of type InvoiceTableModel DELETE called, ignoring dirty marking");
            }
            return;
        }
        this.setDirty();
        if (this.debugMode) {
            System.out.println("tableChanged called");
        }
    }
}
