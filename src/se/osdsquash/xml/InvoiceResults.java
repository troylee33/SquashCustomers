package se.osdsquash.xml;

import java.util.List;

/**
 * Class holding invoice creation results
 */
public class InvoiceResults {

    private final List<String> invoiceFilenames;
    private final List<String> customersWithoutSubscriptions;

    protected InvoiceResults(
        List<String> invoiceFilenames,
        List<String> customersWithoutSubscriptions) {

        this.invoiceFilenames = invoiceFilenames;
        this.customersWithoutSubscriptions = customersWithoutSubscriptions;
    }

    /**
     * Returns a filename list of all invoices created, e.g.
     * one for all existing customers.
     * 
     * @return All invoice filenames created
     */
    public List<String> getAllInvoiceFilenames() {
        return this.invoiceFilenames;
    }

    /**
     * Returns a "warning list" with all the customers whose
     * invoice did not contain any subscription at all.
     * 
     * @return All customer names that got an empty invoice
     */
    public List<String> getEmptyInvoiceCustomers() {
        return this.customersWithoutSubscriptions;
    }
}
