package org.gnucash.write;

import org.gnucash.read.GnucashVendor;
import org.gnucash.read.auxiliary.GCshAddress;
import org.gnucash.write.auxiliary.GCshWritableAddress;
import org.gnucash.read.GnucashObject;

/**
 * Vendor that can be modified
 */
public interface GnucashWritableVendor extends GnucashVendor, GnucashWritableObject {

	void remove();

	/**
	 * @param number the user-assigned number of this Vendor (may contain non-digits)
	 * @see {@link GnucashVendor#getNumber()}
	 */
	void setNumber(String number);

	void setName(String name);

	void setAddress(GCshAddress adr);

	GCshWritableAddress getWritableAddress();

	GCshWritableAddress getAddress();

	/**
	 * @param name  the name of the user-defined attribute
	 * @param value the value or null if not set
	 * @see {@link GnucashObject#getUserDefinedAttribute(String)}
	 */
	void setUserDefinedAttribute(final String name, final String value);
}
