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

    private final Instant startsAt;

    private final double centsPerKWh;
}
