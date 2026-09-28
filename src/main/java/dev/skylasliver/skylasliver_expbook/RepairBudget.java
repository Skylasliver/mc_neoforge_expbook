package dev.skylasliver.skylasliver_expbook;

/** Unused purchased durability is retained between repair ticks. */
public record RepairBudget(int repaired, int charged, int credit) {
    public static RepairBudget spend(int damage, int points, int credit, int rate) {
        if (rate <= 0) return new RepairBudget(0, 0, credit);
        int repaired = (int) Math.min(damage, (long) points * rate + credit);
        int unpaid = Math.max(0, repaired - credit);
        int charged = (int) (((long) unpaid + rate - 1) / rate);
        return new RepairBudget(repaired, charged,
                (int) ((long) credit + (long) charged * rate - repaired));
    }
}
