# GnuCash Budget Parser (Java)

Lichtgewicht Java-parser voor het uitlezen van **begrotingen** uit GnuCash `.gnucash` bestanden.
Geen externe dependencies nodig — alleen standaard Java SE.

## Projectstructuur

```
src/main/java/nl/gnucash/
├── Main.java                          ← voorbeeldgebruik
├── model/
│   ├── Budget.java                    ← begroting-model
│   └── BudgetAmount.java              ← bedrag per account/periode
└── parser/
    └── GnuCashBudgetParser.java       ← kern-parser
```

## Gebruik

```java
GnuCashBudgetParser parser = new GnuCashBudgetParser();
List<Budget> budgets = parser.parse(new File("boekhouding.gnucash"));

for (Budget b : budgets) {
    System.out.println(b.getName() + " (" + b.getNumPeriods() + " periodes)");

    for (BudgetAmount a : b.getAmounts()) {
        System.out.printf("Account %s, periode %d: %s%n",
            a.getAccountId(), a.getPeriodNum(), a.getAmount());
    }
}
```

## Compileren en uitvoeren

```bash
# Compileren
find src -name "*.java" | xargs javac -d out

# Uitvoeren
java -cp out nl.gnucash.Main pad/naar/boekhouding.gnucash
```

## Wat wordt geparsed?

| Veld              | XML-element                        |
|-------------------|------------------------------------|
| `id`              | `<bgt:id>`                         |
| `name`            | `<bgt:name>`                       |
| `description`     | `<bgt:description>`                |
| `numPeriods`      | `<bgt:num-periods>`                |
| `periodType`      | `<recurrence:period_type>`         |
| `recurrenceMult`  | `<recurrence:mult>`                |
| `startDate`       | `<recurrence:start><gdate>`        |
| `amounts`         | `<bgt:slots>` (geneste slot-boom)  |

## GnuCash bedragen

GnuCash slaat bedragen op als breuk: `"15000/100"` = € 150,00.
De parser converteert dit automatisch naar `BigDecimal`.

## Bestands-ondersteuning

- ✅ Gzip-gecomprimeerde `.gnucash` bestanden (standaard)
- ✅ Ongecomprimeerde XML-bestanden

## Combineren met jgnucashlib

Voor accounts, transacties en andere data kun je jgnucashlib gebruiken.
Voor begrotingen gebruik je deze parser. Combineer ze via het account-GUID:

```java
// jgnucashlib voor accounts
GnucashFile gcFile = new GnucashFileImpl(file);
GnucashAccount account = gcFile.getAccountByID(budgetAmount.getAccountId());

// Deze parser voor begrotingen
List<Budget> budgets = new GnuCashBudgetParser().parse(file);
```
