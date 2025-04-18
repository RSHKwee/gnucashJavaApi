package org.gnucash.read.impl;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.gnucash.generated.GncV2;
import org.gnucash.generated.ObjectFactory;
import org.gnucash.numbers.FixedPointNumber;
import org.gnucash.read.GnucashCustomer;
import org.gnucash.read.GnucashFile;
import org.gnucash.read.GnucashGenerInvoice;
import org.gnucash.read.GnucashGenerJob;
import org.gnucash.read.auxiliary.GCshAddress;
import org.gnucash.read.auxiliary.GCshBillTerms;
import org.gnucash.read.auxiliary.GCshOwner;
import org.gnucash.read.auxiliary.GCshTaxTable;
import org.gnucash.read.impl.auxiliary.GCshAddressImpl;
import org.gnucash.read.impl.spec.GnucashCustomerJobImpl;
import org.gnucash.read.spec.GnucashCustomerInvoice;
import org.gnucash.read.spec.GnucashCustomerJob;
import org.gnucash.read.spec.GnucashJobInvoice;
import org.gnucash.read.spec.SpecInvoiceCommon;
import org.gnucash.read.spec.WrongInvoiceTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnucashCustomerImpl extends GnucashObjectImpl implements GnucashCustomer {
  private static final Logger LOGGER = LoggerFactory.getLogger(GnucashCustomerImpl.class);

  /**
   * the JWSDP-object we are facading.
   */
  private final GncV2.GncBook.GncGncCustomer jwsdpPeer;

  /**
   * The currencyFormat to use for default-formating.<br/>
   * Please access only using {@link #getCurrencyFormat()}.
   *
   * @see #getCurrencyFormat()
   */
  private NumberFormat currencyFormat = null;

  // ---------------------------------------------------------------

  /**
   * @param peer    the JWSDP-object we are facading.
   * @param gncFile the file to register under
   */
  protected GnucashCustomerImpl(final GncV2.GncBook.GncGncCustomer peer, final GnucashFile gncFile) {
    super((peer.getCustSlots() == null) ? new ObjectFactory().createSlotsType() : peer.getCustSlots(), gncFile);

    if (peer.getCustSlots() == null) {
      peer.setCustSlots(getSlots());
    }

    jwsdpPeer = peer;
  }

  // ---------------------------------------------------------------

  /**
   * @return the JWSDP-object we are wrapping.
   */
  @SuppressWarnings("exports")
  public GncV2.GncBook.GncGncCustomer getJwsdpPeer() {
    return jwsdpPeer;
  }

  // ---------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public String getId() {
    return jwsdpPeer.getCustGuid().getValue();
  }

  /**
   * {@inheritDoc}
   */
  public String getNumber() {
    return jwsdpPeer.getCustId();
  }

  /**
   * {@inheritDoc}
   */
  public String getName() {
    return jwsdpPeer.getCustName();
  }

  /**
   * {@inheritDoc}
   */
  public GCshAddress getAddress() {
    return new GCshAddressImpl(jwsdpPeer.getCustAddr());
  }

  /**
   * {@inheritDoc}
   */
  public GCshAddress getShippingAddress() {
    return new GCshAddressImpl(jwsdpPeer.getCustShipaddr());
  }

  /**
   * {@inheritDoc}
   */
  public FixedPointNumber getDiscount() {
    if (jwsdpPeer.getCustDiscount() == null) {
      return null;
    }

    return new FixedPointNumber(jwsdpPeer.getCustDiscount());
  }

  /**
   * {@inheritDoc}
   */
  public FixedPointNumber getCredit() {
    if (jwsdpPeer.getCustCredit() == null) {
      return null;
    }

    return new FixedPointNumber(jwsdpPeer.getCustCredit());
  }

  /**
   * {@inheritDoc}
   */
  public String getNotes() {
    return jwsdpPeer.getCustNotes();
  }

  // ---------------------------------------------------------------

  /**
   * @return the currency-format to use if no locale is given.
   */
  protected NumberFormat getCurrencyFormat() {
    if (currencyFormat == null) {
      currencyFormat = NumberFormat.getCurrencyInstance();
    }

    return currencyFormat;
  }

  // ---------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public String getTaxTableID() {
    GncV2.GncBook.GncGncCustomer.CustTaxtable custTaxtable = jwsdpPeer.getCustTaxtable();
    if (custTaxtable == null) {
      return null;
    }

    return custTaxtable.getValue();
  }

  /**
   * {@inheritDoc}
   */
  public GCshTaxTable getTaxTable() {
    String id = getTaxTableID();
    if (id == null) {
      return null;
    }
    return getGnucashFile().getTaxTableByID(id);
  }

  // ---------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public String getTermsID() {
    GncV2.GncBook.GncGncCustomer.CustTerms custTerms = jwsdpPeer.getCustTerms();
    if (custTerms == null) {
      return null;
    }

    return custTerms.getValue();
  }

  /**
   * {@inheritDoc}
   */
  public GCshBillTerms getTerms() {
    String id = getTermsID();
    if (id == null) {
      return null;
    }
    return getGnucashFile().getBillTermsByID(id);
  }

  // ---------------------------------------------------------------

  /**
   * date is not checked so invoiced that have entered payments in the future are considered Paid.
   *
   * @return the current number of Unpaid invoices
   */
  public int getNofOpenInvoices() throws WrongInvoiceTypeException {
    return getGnucashFile().getUnpaidInvoicesForCustomer_direct(this).size();
  }

  // -------------------------------------

  /**
   * @return the net sum of payments for invoices to this client
   */
  public FixedPointNumber getIncomeGenerated(GnucashGenerInvoice.ReadVariant readVar) {
    if (readVar == GnucashGenerInvoice.ReadVariant.DIRECT) {
      return getIncomeGenerated_direct();
    } else if (readVar == GnucashGenerInvoice.ReadVariant.VIA_JOB) {
      return getIncomeGenerated_viaAllJobs();
    }

    return null; // Compiler happy
  }

  /**
   * @return the net sum of payments for invoices to this client
   */
  public FixedPointNumber getIncomeGenerated_direct() {
    FixedPointNumber retval = new FixedPointNumber();

    try {
      for (GnucashCustomerInvoice invcSpec : getPaidInvoices_direct()) {
        // if ( invcGen.getType().equals(GnucashGenerInvoice.TYPE_CUSTOMER) ) {
        // GnucashCustomerInvoice invcSpec = new GnucashCustomerInvoiceImpl(invcGen);
        GnucashCustomer cust = invcSpec.getCustomer();
        if (cust.getId().equals(this.getId())) {
          retval.add(((SpecInvoiceCommon) invcSpec).getAmountWithoutTaxes());
        }
        // } // if invc type
      } // for
    } catch (WrongInvoiceTypeException e) {
      LOGGER.error("getIncomeGenerated_direct: Serious error", e);
    }

    return retval;
  }

  /**
   * @return the net sum of payments for invoices to this client
   */
  public FixedPointNumber getIncomeGenerated_viaAllJobs() {
    FixedPointNumber retval = new FixedPointNumber();

    try {
      for (GnucashJobInvoice invcSpec : getPaidInvoices_viaAllJobs()) {
        // if ( invcGen.getType().equals(GnucashGenerInvoice.TYPE_CUSTOMER) ) {
        // GnucashCustomerInvoice invcSpec = new GnucashCustomerInvoiceImpl(invcGen);
        GnucashCustomer cust = invcSpec.getCustomer();
        if (cust.getId().equals(this.getId())) {
          retval.add(((SpecInvoiceCommon) invcSpec).getAmountWithoutTaxes());
        }
        // } // if invc type
      } // for
    } catch (WrongInvoiceTypeException e) {
      LOGGER.error("getIncomeGenerated_viaAllJobs: Serious error", e);
    }

    return retval;
  }

  /**
   * @return formatted acording to the current locale's currency-format
   * @see #getIncomeGenerated()
   */
  public String getIncomeGeneratedFormatted(GnucashGenerInvoice.ReadVariant readVar) {
    return getCurrencyFormat().format(getIncomeGenerated(readVar));

  }

  /**
   * @param l the locale to format for
   * @return formatted acording to the given locale's currency-format
   * @see #getIncomeGenerated()
   */
  public String getIncomeGeneratedFormatted(GnucashGenerInvoice.ReadVariant readVar, final Locale l) {
    return NumberFormat.getCurrencyInstance(l).format(getIncomeGenerated(readVar));
  }

  // -------------------------------------

  /**
   * @return the sum of left to pay Unpaid invoiced
   */
  public FixedPointNumber getOutstandingValue(GnucashGenerInvoice.ReadVariant readVar) {
    if (readVar == GnucashGenerInvoice.ReadVariant.DIRECT) {
      return getOutstandingValue_direct();
    } else if (readVar == GnucashGenerInvoice.ReadVariant.VIA_JOB) {
      return getOutstandingValue_viaAllJobs();
    }

    return null; // Compiler happy
  }

  /**
   * @return the sum of left to pay Unpaid invoiced
   */
  public FixedPointNumber getOutstandingValue_direct() {
    FixedPointNumber retval = new FixedPointNumber();

    try {
      for (GnucashCustomerInvoice invcSpec : getUnpaidInvoices_direct()) {
        // if ( invcGen.getType().equals(GnucashGenerInvoice.TYPE_CUSTOMER) ) {
        // GnucashCustomerInvoice invcSpec = new GnucashCustomerInvoiceImpl(invcGen);
        GnucashCustomer cust = invcSpec.getCustomer();
        if (cust.getId().equals(this.getId())) {
          retval.add(((SpecInvoiceCommon) invcSpec).getAmountUnpaidWithTaxes());
        }
        // } // if invc type
      } // for
    } catch (WrongInvoiceTypeException e) {
      LOGGER.error("getOutstandingValue_direct: Serious error", e);
    }

    return retval;
  }

  /**
   * @return the sum of left to pay Unpaid invoiced
   */
  public FixedPointNumber getOutstandingValue_viaAllJobs() {
    FixedPointNumber retval = new FixedPointNumber();

    try {
      for (GnucashJobInvoice invcSpec : getUnpaidInvoices_viaAllJobs()) {
        // if ( invcGen.getType().equals(GnucashGenerInvoice.TYPE_CUSTOMER) ) {
        // GnucashCustomerInvoice invcSpec = new GnucashCustomerInvoiceImpl(invcGen);
        GnucashCustomer cust = invcSpec.getCustomer();
        if (cust.getId().equals(this.getId())) {
          retval.add(((SpecInvoiceCommon) invcSpec).getAmountUnpaidWithTaxes());
        }
        // } // if invc type
      } // for
    } catch (WrongInvoiceTypeException e) {
      LOGGER.error("getOutstandingValue_viaAllJobs: Serious error", e);
    }

    return retval;
  }

  /**
   * @return Formatted acording to the current locale's currency-format
   * @see #getOutstandingValue()
   */
  public String getOutstandingValueFormatted(GnucashGenerInvoice.ReadVariant readVar) {
    return getCurrencyFormat().format(getOutstandingValue(readVar));
  }

  /**
   * @see #getOutstandingValue() Formatted acording to the given locale's currency-format
   */
  public String getOutstandingValueFormatted(GnucashGenerInvoice.ReadVariant readVar, final Locale l) {
    return NumberFormat.getCurrencyInstance(l).format(getOutstandingValue(readVar));
  }

  // -----------------------------------------------------------------

  /**
   * @return the jobs that have this customer associated with them.
   * @see GnucashCustomer#getGenerJobs()
   */
  public java.util.Collection<GnucashCustomerJob> getJobs() throws WrongInvoiceTypeException {

    List<GnucashCustomerJob> retval = new LinkedList<GnucashCustomerJob>();

    for (GnucashGenerJob jobGener : getGnucashFile().getGenerJobs()) {
      if (jobGener.getOwnerType().equals(GCshOwner.TYPE_CUSTOMER)) {
        GnucashCustomerJob jobSpec = new GnucashCustomerJobImpl(jobGener);
        if (jobSpec.getCustomerId().equals(getId())) {
          retval.add(jobSpec);
        }
      }
    }

    return retval;
  }

  // -----------------------------------------------------------------

  @Override
  public Collection<GnucashGenerInvoice> getInvoices() throws WrongInvoiceTypeException {
    Collection<GnucashGenerInvoice> retval = new LinkedList<GnucashGenerInvoice>();

    for (GnucashCustomerInvoice invc : getGnucashFile().getInvoicesForCustomer_direct(this)) {
      retval.add(invc);
    }

    for (GnucashJobInvoice invc : getGnucashFile().getInvoicesForCustomer_viaAllJobs(this)) {
      retval.add(invc);
    }

    return retval;
  }

  @Override
  public Collection<GnucashCustomerInvoice> getPaidInvoices_direct() throws WrongInvoiceTypeException {
    return getGnucashFile().getPaidInvoicesForCustomer_direct(this);
  }

  @Override
  public Collection<GnucashJobInvoice> getPaidInvoices_viaAllJobs() throws WrongInvoiceTypeException {
    return getGnucashFile().getPaidInvoicesForCustomer_viaAllJobs(this);
  }

  @Override
  public Collection<GnucashCustomerInvoice> getUnpaidInvoices_direct() throws WrongInvoiceTypeException {
    return getGnucashFile().getUnpaidInvoicesForCustomer_direct(this);
  }

  @Override
  public Collection<GnucashJobInvoice> getUnpaidInvoices_viaAllJobs() throws WrongInvoiceTypeException {
    return getGnucashFile().getUnpaidInvoicesForCustomer_viaAllJobs(this);
  }

  // -----------------------------------------------------------------

  public static int getHighestNumber(GnucashCustomer cust) {
    return cust.getGnucashFile().getHighestCustomerNumber();
  }

}
