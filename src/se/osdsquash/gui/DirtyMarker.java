package se.osdsquash.gui;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

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

    /**
     * Inits the marker as clean
     */
    protected DirtyMarker() {
        this.dirtyBoolean = new AtomicBoolean(false);
    }

    protected void setDirty() {
        this.dirtyBoolean.compareAndSet(false, true);
    }

    protected void setClean() {
        this.dirtyBoolean.compareAndSet(true, false);
    }

    public boolean isDirty() {
        return this.dirtyBoolean.get();
    }

    //  This handles JCheckBox change events - the ChangeListener
    // -----------------------------------------------------------
    @Override
    public void stateChanged(ChangeEvent e) {
        this.setDirty();
    }

    //  This handles JTextField change events - the DocumentListener
    // --------------------------------------------------------------
    @Override
    public void insertUpdate(DocumentEvent e) {
        this.setDirty();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        this.setDirty();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        this.setDirty();
    }

    //  This handles JTable change events - the TableModelListener
    // ------------------------------------------------------------
    @Override
    public void tableChanged(TableModelEvent e) {
        this.setDirty();
    }
}
