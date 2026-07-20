package org.gnucash.read;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

/**
 * Een GnuCash begroting (gnc:budget).
 *
 * Volgt het patroon van GnucashAccount, GnucashGenerInvoice, etc.
 * in de RSHKwee/gnucashJavaApi library.
 */
public interface GnucashBudget {

    /**
     * @return het interne GUID van de begroting
     */
    String getId();

    /**
     * @return de naam van de begroting, zoals zichtbaar in GnuCash
     */
    String getName();

    /**
     * @return de optionele beschrijving, of null
     */
    String getDescription();

    /**
     * @return het aantal periodes (bijv. 12 voor een jaarbudget per maand)
     */
    int getNumPeriods();

    /**
     * @return het recurrentie-type: "month", "year", "week", ...
     */
    String getPeriodType();

    /**
     * @return de recurrentie-multiplicator (meestal 1)
     */
    int getRecurrenceMult();

    /**
     * @return de startdatum van de eerste periode, of null
     */
    LocalDate getStartDate();

    /**
     * @return alle begrotingsbedragen (alle accounts, alle periodes)
     */
    Collection<BudgetAmount> getAmounts();

    /**
     * @param accountId het GUID van de account
     * @return alle bedragen voor die account, of lege lijst
     */
    Collection<BudgetAmount> getAmountsForAccount(String accountId);

    /**
     * @param accountId het GUID van de account
     * @param periodNum het 0-gebaseerde periodenummer
     * @return het begrote bedrag, of null indien niet aanwezig
     */
    BigDecimal getAmountForAccountAndPeriod(String accountId, int periodNum);

    // ---------------------------------------------------------------

    /**
     * Één begrotingsbedrag voor een specifieke account en periode.
     */
    interface BudgetAmount {
        String     getAccountId();
        int        getPeriodNum();
        BigDecimal getAmount();
    }
}