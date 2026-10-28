package org.gnucash.read.spec;

import org.gnucash.numbers.Amount;
import org.gnucash.read.GnucashGenerInvoiceEntry;

public interface GnucashVendorBillEntry extends GnucashGenerInvoiceEntry {
	String getBillID();

	GnucashVendorBill getBill() throws WrongInvoiceTypeException;

	// -----------------------------------------------------------------

	Amount getPrice() throws WrongInvoiceTypeException;

	String getPriceFormatted() throws WrongInvoiceTypeException;

}
