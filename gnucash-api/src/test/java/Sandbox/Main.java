package Sandbox;

import nl.gnucash.model.Budget;
import nl.gnucash.model.BudgetAmount;
import nl.gnucash.parser.GnuCashBudgetParser;

import java.io.File;
import java.util.List;

/**
 * Eenvoudig voorbeeld van gebruik van de GnuCashBudgetParser.
 *
 * Uitvoeren:
 *   javac -cp . nl/gnucash/**&#47;*.java nl/gnucash/Main.java
 *   java nl.gnucash.Main pad/naar/boekhouding.gnucash
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "boekhouding.gnucash";
        path = "D:\\Users\\René\\SynologyDrive\\Documenten\\Administraties\\Bewindvoering.gnucash";
        
        GnuCashBudgetParser parser = new GnuCashBudgetParser();
        List<Budget> budgets = parser.parse(new File(path));

        if (budgets.isEmpty()) {
            System.out.println("Geen begrotingen gevonden.");
            return;
        }

        for (Budget b : budgets) {
            System.out.println("=== Begroting: " + b.getName() + " ===");
            System.out.println("  ID         : " + b.getId());
            System.out.println("  Omschrijving: " + b.getDescription());
            System.out.println("  Periodes   : " + b.getNumPeriods()
                    + " x " + b.getRecurrenceMult() + " " + b.getPeriodType());
            System.out.println("  Startdatum : " + b.getStartDate());
            System.out.println("  Bedragen   : " + b.getAmounts().size());
            System.out.println();

            // Toon de eerste 20 bedragen als voorbeeld
            b.getAmounts().stream()
             .limit(20)
             .forEach(a -> System.out.printf(
                     "    Account %-36s  periode %2d  bedrag %s%n",
                     a.getAccountId(), a.getPeriodNum(), a.getAmount()));

            if (b.getAmounts().size() > 20) {
                System.out.println("    ... (" + (b.getAmounts().size() - 20) + " meer)");
            }
            System.out.println();
        }
    }
}
