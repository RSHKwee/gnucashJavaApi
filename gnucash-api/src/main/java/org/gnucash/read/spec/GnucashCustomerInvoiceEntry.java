package org.gnucash.read.spec;

import org.gnucash.numbers.Amount;
import org.gnucash.read.GnucashGenerInvoiceEntry;

public interface GnucashCustomerInvoiceEntry extends GnucashGenerInvoiceEntry {
	String getInvoiceID();

	GnucashCustomerInvoice getInvoice() throws WrongInvoiceTypeException;

	// -----------------------------------------------------------------

	Amount getPrice() throws WrongInvoiceTypeException;

	String getPriceFormatted() throws WrongInvoiceTypeException;

}
