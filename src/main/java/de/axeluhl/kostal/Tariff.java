package de.axeluhl.kostal;

import java.time.Instant;

/**
 * Models the tariff structure, giving electrical power prices per time range. Literals are ascending in their
 * {@link #getStartsAt()} time points.
 */
public enum Tariff {
    MAINGAU_OLD(Instant.ofEpochSecond(0), 31),
    MAINGAU_WAR(Instant.ofEpochSecond(1672560003), 71.4),
    MAINGAU_NEW(Instant.ofEpochSecond(1677657603), 54.09),
    GRUENWELT(Instant.ofEpochSecond(1701244803), 36.77);

    private Tariff(Instant startsAt, double centsPerKWh) {
        this.startsAt = startsAt;
        this.centsPerKWh = centsPerKWh;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public double getCentsPerKWh() {
        return centsPerKWh;
    }
    
    public static double getCents(Instant when, double energyInWattHours) {
        for (final Tariff tariff : Tariff.values()) {
            if (!tariff.getStartsAt().isAfter(when)) {
                return energyInWattHours / 1000.0 * tariff.getCentsPerKWh();
            }
        }
        throw new InternalError("Couldn't find tariff for time point "+when);
    }

    private final Instant startsAt;

    private final double centsPerKWh;
}
