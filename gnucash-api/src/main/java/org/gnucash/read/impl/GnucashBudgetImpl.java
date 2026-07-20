package org.gnucash.read.impl;

import org.gnucash.generated.GncBudget;
import org.gnucash.generated.Slot;
import org.gnucash.generated.SlotValue;
import org.gnucash.generated.SlotsType;
import org.gnucash.read.GnucashBudget;
import org.gnucash.read.GnucashFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementatie van {@link GnucashBudget} voor RSHKwee/gnucashJavaApi.
 *
 * Gebaseerd op de exacte gegenereerde JAXB-klassen:
 *   GncBudget, SlotsType, Slot, SlotValue, RecurrenceStartType
 *
 * SlotValue gebruikt @XmlMixed: getContent() geeft List<Object> terug
 * met daarin een mix van Slot, String (whitespace), en JAXBElement.
 * Voor type="frame" zitten de child-Slots in getContent().
 * Voor type="numeric" zit het bedrag als String in getContent().
 */
public class GnucashBudgetImpl implements GnucashBudget {

    private static final Logger LOGGER = LoggerFactory.getLogger(GnucashBudgetImpl.class);

    protected final GncBudget   jwsdpPeer;
    protected final GnucashFile file;

    private List<BudgetAmountImpl> amounts = null;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    public GnucashBudgetImpl(final GncBudget jwsdpPeer, final GnucashFile file) {
        if (jwsdpPeer == null) throw new IllegalArgumentException("jwsdpPeer is null");
        if (file == null)      throw new IllegalArgumentException("file is null");
        this.jwsdpPeer = jwsdpPeer;
        this.file      = file;
    }

    // ---------------------------------------------------------------
    // GnucashBudget -- Identificatie
    // ---------------------------------------------------------------

    @Override
    public String getId() {
        if (jwsdpPeer.getBgtId() == null) return null;
        return jwsdpPeer.getBgtId().getValue();
    }

    @Override
    public String getName() {
        return jwsdpPeer.getBgtName();
    }

    /**
     * bgt_description is optioneel en opgeslagen als JAXBElement<String>.
     */
    @Override
    public String getDescription() {
        if (jwsdpPeer.getBgtDescription() == null) return null;
        return jwsdpPeer.getBgtDescription().getValue();
    }

    // ---------------------------------------------------------------
    // GnucashBudget -- Periode-informatie
    // ---------------------------------------------------------------

    @Override
    public int getNumPeriods() {
        return jwsdpPeer.getBgtNumPeriods();
    }

    @Override
    public String getPeriodType() {
        if (jwsdpPeer.getBgtRecurrence() == null) return null;
        return jwsdpPeer.getBgtRecurrence().getRecurrencePeriodType();
    }

    @Override
    public int getRecurrenceMult() {
        if (jwsdpPeer.getBgtRecurrence() == null) return 1;
        return jwsdpPeer.getBgtRecurrence().getRecurrenceMult();
    }

    /**
     * getRecurrenceStart().getGdate() retourneert XMLGregorianCalendar.
     * Converteren naar LocalDate via jaar/maand/dag velden.
     */
    @Override
    public LocalDate getStartDate() {
        try {
            if (jwsdpPeer.getBgtRecurrence() == null) return null;
            if (jwsdpPeer.getBgtRecurrence().getRecurrenceStart() == null) return null;
            javax.xml.datatype.XMLGregorianCalendar cal =
                    jwsdpPeer.getBgtRecurrence().getRecurrenceStart().getGdate();
            if (cal == null) return null;
            return LocalDate.of(cal.getYear(), cal.getMonth(), cal.getDay());
        } catch (Exception e) {
            LOGGER.error("Fout bij parsen startdatum voor budget '"
                    + getName() + "': " + e.getMessage());
            return null;
        }
    }

    // ---------------------------------------------------------------
    // GnucashBudget -- Bedragen
    // ---------------------------------------------------------------

    @Override
    public Collection<BudgetAmount> getAmounts() {
        return Collections.unmodifiableList(loadAmounts());
    }

    @Override
    public Collection<BudgetAmount> getAmountsForAccount(final String accountId) {
        if (accountId == null) return Collections.emptyList();
        return loadAmounts().stream()
                .filter(a -> accountId.equals(a.getAccountId()))
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getAmountForAccountAndPeriod(final String accountId,
                                                    final int periodNum) {
        return loadAmounts().stream()
                .filter(a -> accountId.equals(a.getAccountId())
                          && a.getPeriodNum() == periodNum)
                .map(BudgetAmount::getAmount)
                .findFirst()
                .orElse(null);
    }

    // ---------------------------------------------------------------
    // Lazy-loading van bedragen
    //
    // SlotValue is @XmlMixed: getContent() = List<Object> met daarin:
    //   - Slot        voor type="frame"   (kind-slots per periode)
    //   - String      voor type="numeric" (het bedrag "teller/noemer")
    //                 en whitespace-tekst (negeren)
    //   - JAXBElement voor ts_date / gdate (niet relevant hier)
    //
    // Structuur:
    //   getBgtSlots()                    SlotsType
    //     .getSlot()                     List<Slot>   -- niveau 1: per account
    //       .getSlotKey()                account GUID
    //       .getSlotValue()              SlotValue  type="frame"
    //         .getContent()              List<Object>
    //           [Slot]                   -- niveau 2: per periode
    //             .getSlotKey()          periodenummer ("0","1",...)
    //             .getSlotValue()        SlotValue  type="numeric"
    //               .getContent()        List<Object>
    //                 [String]           bedrag "teller/noemer"
    // ---------------------------------------------------------------

    private List<BudgetAmountImpl> loadAmounts() {
        if (amounts != null) return amounts;
        amounts = new ArrayList<>();

        SlotsType slotsType = jwsdpPeer.getBgtSlots();
        if (slotsType == null) return amounts;

        // SlotsType.getSlot() = List<Slot>  (geen cast nodig)
        for (Slot accountSlot : slotsType.getSlot()) {

            String accountId = accountSlot.getSlotKey();
            if (accountId == null) continue;
            accountId = accountId.trim();

            SlotValue accountSlotValue = accountSlot.getSlotValue();
            if (accountSlotValue == null) continue;
            // type="frame": de periode-slots zitten in getContent() als Slot-objecten
            if (!"frame".equals(accountSlotValue.getType())) continue;

            for (Object contentItem : accountSlotValue.getContent()) {
                // Whitespace-strings en andere elementen overslaan
                if (!(contentItem instanceof Slot)) continue;
                Slot periodSlot = (Slot) contentItem;

                String keyText = periodSlot.getSlotKey();
                if (keyText == null) continue;

                SlotValue periodSlotValue = periodSlot.getSlotValue();
                if (periodSlotValue == null) continue;
                // type="numeric": het bedrag zit als String in getContent()
                if (!"numeric".equals(periodSlotValue.getType())) continue;

                String valText = extractStringFromContent(periodSlotValue.getContent());
                if (valText == null) continue;

                try {
                    int        periodNum = Integer.parseInt(keyText.trim());
                    BigDecimal amount    = parseNumeric(valText.trim());
                    amounts.add(new BudgetAmountImpl(accountId, periodNum, amount));
                } catch (NumberFormatException e) {
                    LOGGER.warn("Ongeldige waarde in budget '" + getName()
                            + "': key='" + keyText + "', value='" + valText + "'");
                }
            }
        }

        LOGGER.debug("Budget '" + getName() + "': "
                + amounts.size() + " bedragen geladen.");
        return amounts;
    }

    /**
     * Haalt de eerste niet-lege String op uit een @XmlMixed content-lijst.
     * Voor type="numeric" is dit de waarde "teller/noemer", bijv. "15000/100".
     * Whitespace-only strings worden overgeslagen.
     */
    private String extractStringFromContent(final List<Object> content) {
        if (content == null) return null;
        for (Object item : content) {
            if (item instanceof String) {
                String s = ((String) item).trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Hulpmethode getallen
    // ---------------------------------------------------------------

    /**
     * Converteert GnuCash numeriek formaat "teller/noemer" naar BigDecimal.
     *   "15000/100"  --> 150.00
     *   "-5000/100"  --> -50.00
     *   "0/1"        --> 0
     */
    protected BigDecimal parseNumeric(final String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        if (value.contains("/")) {
            String[]   parts = value.split("/", 2);
            BigDecimal num   = new BigDecimal(parts[0].trim());
            BigDecimal den   = new BigDecimal(parts[1].trim());
            if (den.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
            return num.divide(den, 10, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        return new BigDecimal(value);
    }

    // ---------------------------------------------------------------
    // toString / equals / hashCode
    // ---------------------------------------------------------------

    @Override
    public String toString() {
        return "GnucashBudgetImpl{"
             + "id='" + getId() + '\''
             + ", name='" + getName() + '\''
             + ", numPeriods=" + getNumPeriods()
             + ", periodType='" + getPeriodType() + '\''
             + ", start=" + getStartDate()
             + ", nofAmounts=" + loadAmounts().size()
             + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof GnucashBudget)) return false;
        GnucashBudget other = (GnucashBudget) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    // ---------------------------------------------------------------
    // Geneste implementatie BudgetAmountImpl
    // ---------------------------------------------------------------

    public static class BudgetAmountImpl implements GnucashBudget.BudgetAmount {

        private final String     accountId;
        private final int        periodNum;
        private final BigDecimal amount;

        public BudgetAmountImpl(final String accountId,
                                final int periodNum,
                                final BigDecimal amount) {
            this.accountId = accountId;
            this.periodNum = periodNum;
            this.amount    = amount;
        }

        @Override public String     getAccountId() { return accountId; }
        @Override public int        getPeriodNum() { return periodNum; }
        @Override public BigDecimal getAmount()    { return amount; }

        @Override
        public String toString() {
            return String.format(
                    "BudgetAmount{account='%s', periode=%d, bedrag=%s}",
                    accountId, periodNum, amount.toPlainString());
        }
    }
}
