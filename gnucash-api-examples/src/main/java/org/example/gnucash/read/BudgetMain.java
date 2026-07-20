package org.example.gnucash.read;

import org.gnucash.read.GnucashAccount;
import org.gnucash.read.GnucashBudget;
import org.gnucash.read.GnucashFile;
import org.gnucash.read.impl.GnucashFileImpl;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;

/**
 * Voorbeeldprogramma dat laat zien hoe de nieuwe GnucashBudget-routines
 * gebruikt worden in combinatie met de bestaande GnucashFile API.
 *
 * Uitvoeren:
 *   mvn compile exec:java \
 *     -Dexec.mainClass="org.gnucash.examples.BudgetMain" \
 *     -Dexec.args="pad/naar/boekhouding.gnucash"
 */
public class BudgetMain {

    public static void main(String[] args) throws Exception {

        // ---------------------------------------------------------------
        // 1. GnuCash bestand laden -- zelfde als altijd
        // ---------------------------------------------------------------
        String path = args.length > 0 ? args[0] : "boekhouding.gnucash";
        path = "D:\\Users\\René\\SynologyDrive\\Documenten\\Administraties\\Bewindvoering.gnucash";

        File gcFile = new File(path);

        if (!gcFile.exists()) {
            System.err.println("Bestand niet gevonden: " + gcFile.getAbsolutePath());
            System.exit(1);
        }

        System.out.println("Laden: " + gcFile.getAbsolutePath());
        GnucashFile file = new GnucashFileImpl(gcFile);
        System.out.println("Geladen.");
        System.out.println();

        // ---------------------------------------------------------------
        // 2. Alle begrotingen tonen
        // ---------------------------------------------------------------
        Collection<GnucashBudget> budgets = file.getBudgets();
        System.out.println("Aantal begrotingen: " + budgets.size());
        System.out.println();

        if (budgets.isEmpty()) {
            System.out.println("Geen begrotingen gevonden in dit bestand.");
            return;
        }

        for (GnucashBudget budget : budgets) {
            printBudgetSummary(budget);
        }

        // ---------------------------------------------------------------
        // 3. Een specifieke begroting opzoeken op naam
        // ---------------------------------------------------------------
        // Pas "Jaarbudget" aan naar de naam die in jouw bestand staat.
        String zoekNaam = budgets.iterator().next().getName(); // neem de eerste als voorbeeld
        System.out.println("=".repeat(60));
        System.out.println("Detail voor begroting: '" + zoekNaam + "'");
        System.out.println("=".repeat(60));

        GnucashBudget budget = file.getBudgetByName(zoekNaam);
        if (budget == null) {
            System.out.println("Niet gevonden: " + zoekNaam);
            return;
        }

        // ---------------------------------------------------------------
        // 4. Bedragen tonen met accountnamen via de bestaande API
        // ---------------------------------------------------------------
        printBudgetDetail(budget, file);

        // ---------------------------------------------------------------
        // 5. Totaal per periode berekenen
        // ---------------------------------------------------------------
        printPeriodTotals(budget, file);

        // ---------------------------------------------------------------
        // 6. Bedrag opvragen voor een specifieke account en periode
        // ---------------------------------------------------------------
        printSingleAmount(budget, file);
    }

    // ---------------------------------------------------------------
    // Samenvattingsregel per begroting
    // ---------------------------------------------------------------

    private static void printBudgetSummary(GnucashBudget budget) {
        System.out.printf("Begroting : %s%n", budget.getName());
        System.out.printf("  ID      : %s%n", budget.getId());
        System.out.printf("  Omschr. : %s%n",
                budget.getDescription() != null ? budget.getDescription() : "(geen)");
        System.out.printf("  Periodes: %d x %d %s (start: %s)%n",
                budget.getNumPeriods(),
                budget.getRecurrenceMult(),
                budget.getPeriodType(),
                budget.getStartDate());
        System.out.printf("  Bedragen: %d%n", budget.getAmounts().size());
        System.out.println();
    }

    // ---------------------------------------------------------------
    // Gedetailleerde tabel: account + periode + bedrag
    // ---------------------------------------------------------------

    private static void printBudgetDetail(GnucashBudget budget, GnucashFile file) {
        System.out.println();
        System.out.printf("%-45s  %7s  %15s%n", "Account", "Periode", "Bedrag");
        System.out.println("-".repeat(72));

        for (GnucashBudget.BudgetAmount amount : budget.getAmounts()) {

            // Accountnaam opzoeken via de bestaande GnucashFile API
            GnucashAccount account = file.getAccountByID(amount.getAccountId());
            String accountName = account != null
                    ? account.getQualifiedName()   // bijv. "Uitgaven:Boodschappen"
                    : "(onbekend: " + amount.getAccountId() + ")";

            System.out.printf("%-45s  %7d  %15s%n",
                    truncate(accountName, 45),
                    amount.getPeriodNum(),
                    amount.getAmount().toPlainString());
        }
        System.out.println();
    }

    // ---------------------------------------------------------------
    // Totaal per periode (som van alle accounts)
    // ---------------------------------------------------------------

    private static void printPeriodTotals(GnucashBudget budget, GnucashFile file) {
        System.out.println("Totalen per periode:");
        System.out.println("-".repeat(30));

        BigDecimal[] totals = new BigDecimal[budget.getNumPeriods()];
        for (int i = 0; i < totals.length; i++) {
            totals[i] = BigDecimal.ZERO;
        }

        for (GnucashBudget.BudgetAmount amount : budget.getAmounts()) {
            int p = amount.getPeriodNum();
            if (p >= 0 && p < totals.length) {
                totals[p] = totals[p].add(amount.getAmount());
            }
        }

        for (int i = 0; i < totals.length; i++) {
            System.out.printf("  Periode %2d : %15s%n", i, totals[i].toPlainString());
        }
        System.out.println();
    }

    // ---------------------------------------------------------------
    // Bedrag voor één specifieke account en periode
    // ---------------------------------------------------------------

    private static void printSingleAmount(GnucashBudget budget, GnucashFile file) {
        // Neem de eerste account uit de begroting als voorbeeld
        if (budget.getAmounts().isEmpty()) return;

        GnucashBudget.BudgetAmount first = budget.getAmounts().iterator().next();
        String accountId = first.getAccountId();
        int    periode   = 0;  // januari (of eerste periode)

        GnucashAccount account = file.getAccountByID(accountId);
        String accountName = account != null ? account.getQualifiedName() : accountId;

        BigDecimal bedrag = budget.getAmountForAccountAndPeriod(accountId, periode);

        System.out.println("Specifieke opvraging:");
        System.out.println("-".repeat(40));
        System.out.printf("  Begroting : %s%n", budget.getName());
        System.out.printf("  Account   : %s%n", accountName);
        System.out.printf("  Periode   : %d%n", periode);
        System.out.printf("  Bedrag    : %s%n",
                bedrag != null ? bedrag.toPlainString() : "(niet ingevuld)");
        System.out.println();

        // Alle periodes voor dezelfde account
        System.out.printf("Alle periodes voor '%s':%n", accountName);
        System.out.println("-".repeat(40));
        for (GnucashBudget.BudgetAmount a : budget.getAmountsForAccount(accountId)) {
            System.out.printf("  Periode %2d : %s%n",
                    a.getPeriodNum(), a.getAmount().toPlainString());
        }
    }

    // ---------------------------------------------------------------
    // Hulpmethode
    // ---------------------------------------------------------------

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}