package org.gnucash.read.auxiliary;

import org.gnucash.numbers.Amount;

public interface GCshBillTermsProximo {

	Integer getDueDay();

	Integer getDiscountDay();

	Amount getDiscount();

}
