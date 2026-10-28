package org.gnucash.read.impl.auxiliary;

import org.gnucash.numbers.Amount;

/**
 * This Class represents a sum of the taxes of
 * multiple invoice-lines for one of the different
 * tax-percentages that occured.<br/>
 * e.g. you may have 2 sales-tax-rates of 7% and 16%
 * and both occur, so you will get 2 instances
 * of this class. One sum of the 7%-items and one for
 * the 16%-items.
 */
public class GCshTaxedSumImpl {
    
	/**
	 * How much tax it is.
	 * 16%=0.16
	 */
	private Amount myTaxpercent;

	/**
	 * The sum of Paid taxes.
	 */
	private Amount taxsum;

	// -----------------------------------------------------------

	/**
	 * @param pTaxpercent how much tax it is
	 * @param pTaxsum the sum of Paid taxes
	 */
	public GCshTaxedSumImpl(final Amount pTaxpercent,
			final Amount pTaxsum) {
		super();
		myTaxpercent = pTaxpercent;
		taxsum = (Amount) pTaxsum.clone();
	}

	/**
	 * @param taxpercent How much tax it is.
	 */
	public GCshTaxedSumImpl(final Amount taxpercent) {
		super();
		myTaxpercent = taxpercent;
	}
	
	// -----------------------------------------------------------

	/**
	 *
	 * @return How much tax it is.
	 */
	public Amount getTaxpercent() {
		return myTaxpercent;
	}

	/**
	 *
	 * @param taxpercent How much tax it is.
	 */
	public void setTaxpercent(final Amount taxpercent) {
		if (taxpercent.doubleValue() < 0.0) {
			throw new IllegalArgumentException(
					"negative value '"
							+ taxpercent
							+ "' not allowed for field this.taxpercent");
		}

		myTaxpercent = taxpercent;
	}

	/**
	 *
	 * @return The sum of Paid taxes.
	 */
	public Amount getTaxsum() {
		return taxsum;
	}

	/**
	 *
	 * @param pTaxsum The sum of Paid taxes.
	 */
	public void setTaxsum(final Amount pTaxsum) {
		taxsum = pTaxsum;
	}
}

