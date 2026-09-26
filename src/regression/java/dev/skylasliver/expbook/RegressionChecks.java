package dev.skylasliver.expbook;

public final class RegressionChecks {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void checkBookNumbers() {
        var a = new java.util.UUID(0, 1);
        var b = new java.util.UUID(0, 2);
        var replacement = new java.util.UUID(0, 3);
        var sequence = new dev.skylasliver.expbook.util.BookNumberSequence(java.util.Map.of(), 0);
        check(sequence.numberFor(a) == 1 && sequence.numberFor(b) == 2, "IDs start at one and increment");
        check(sequence.numberFor(a) == 1 && sequence.lastNumber() == 2, "reopening does not consume an ID");
        var reloaded = new dev.skylasliver.expbook.util.BookNumberSequence(sequence.snapshot(), sequence.lastNumber());
        check(reloaded.numberFor(a) == 1 && reloaded.numberFor(b) == 2, "existing IDs survive reload");
        check(reloaded.numberFor(replacement) == 3, "replacement gets the next ID after reload");
        check(reloaded.numberFor(a) == 1, "old ID remains reserved after replacement");
        var staleCounter = new dev.skylasliver.expbook.util.BookNumberSequence(java.util.Map.of(a, 99L), 0);
        check(staleCounter.numberFor(b) == 100, "restored counter cannot collide with existing IDs");
        var reserved = new dev.skylasliver.expbook.util.BookNumberSequence(java.util.Map.of(), 500);
        check(reserved.numberFor(a) == 501, "high-water mark is never reused");
        var large = new dev.skylasliver.expbook.util.BookNumberSequence(java.util.Map.of(a, (long) Integer.MAX_VALUE), 0);
        check(large.numberFor(b) == 2147483648L, "IDs do not wrap at integer limit");
    }

    public static void main(String[] args) {
        check(ExperienceMath.totalPointsForLevel(30) == 1395, "30-level cost");
        check(ExperienceMath.levelForPoints(1394) == 29, "insufficient XP boundary");
        for (int level : new int[]{0, 1, 15, 16, 30, 31, 1000, 1001, 20000, 21863}) {
            int points = ExperienceMath.totalPointsForLevel(level);
            check(ExperienceMath.levelForPoints(points) == level, "level round trip " + level);
            if (level > 0) check(ExperienceMath.levelForPoints(points - 1) == level - 1, "level floor");
        }
        for (int rate : new int[]{2, 3, 7}) {
            int balance = 1000, credit = 0, spent = 0;
            for (int i = 0; i < 101; i++) {
                RepairBudget result = RepairBudget.spend(1, balance, credit, rate);
                check(result.repaired() == 1, "single durability repair");
                balance -= result.charged();
                spent += result.charged();
                credit = result.credit();
            }
            RepairBudget batch = RepairBudget.spend(101, 1000, 0, rate);
            check(spent == batch.charged() && credit == batch.credit(), "batch independence");
        }
        RepairBudget first = RepairBudget.spend(1, 1, 0, 2);
        RepairBudget second = RepairBudget.spend(1, 0, first.credit(), 2);
        check(first.charged() == 1 && second.charged() == 0 && second.repaired() == 1, "empty book credit");
        RepairBudget empty = RepairBudget.spend(10, 0, 0, 2);
        check(empty.repaired() == 0 && empty.charged() == 0, "no free repair");
        RepairBudget large = RepairBudget.spend(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 2);
        check(large.repaired() == Integer.MAX_VALUE && large.charged() == 1073741824, "overflow");
        checkBookNumbers();
        System.out.println("Experience, repair and book numbering regression checks passed.");
    }
}
