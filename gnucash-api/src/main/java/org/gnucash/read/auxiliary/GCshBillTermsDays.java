package org.gnucash.read.auxiliary;

import org.gnucash.numbers.Amount;

public interface GCshBillTermsDays {

	Integer getDueDays();

	Integer getDiscountDays();

	Amount getDiscount();

}
