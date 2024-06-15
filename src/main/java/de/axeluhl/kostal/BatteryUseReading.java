package de.axeluhl.kostal;

import java.time.Instant;

class BatteryUseReading {
    private final Instant time;

    private final double homeOwnConsumptionInWatts;

    private final double pvProductionInWatts;

    private final double totalActivePowerInWatts;

    private final int batterySOC;

    private final double batteryChargeInWatts;

    static BatteryUseReading parse(String line) {
        final String[] fields = line.split("[ \t]+");
        return new BatteryUseReading(Instant.ofEpochMilli(Long.valueOf(fields[0]) / 1000000l), // convert from nanos to millis
                Double.valueOf(fields[1]), Double.valueOf(fields[2]), Double.valueOf(fields[3]),
                Integer.valueOf(fields[4]), Double.valueOf(fields[4]));
    }

    BatteryUseReading(Instant time, double homeOwnConsumptionInWatts, double pvProductionInWatts, double totalActivePowerInWatts,
            int batterySOC, double batteryChargeInWatts) {
        super();
        this.time = time;
        this.homeOwnConsumptionInWatts = homeOwnConsumptionInWatts;
        this.pvProductionInWatts = pvProductionInWatts;
        this.totalActivePowerInWatts = totalActivePowerInWatts;
        this.batterySOC = batterySOC;
        this.batteryChargeInWatts = batteryChargeInWatts;
    }

    public Instant getTime() {
        return time;
    }

    public double getHomeOwnConsumptionInWatts() {
        return homeOwnConsumptionInWatts;
    }

    public double getPvProductionInWatts() {
        return pvProductionInWatts;
    }

    public double getTotalActivePowerInWatts() {
        return totalActivePowerInWatts;
    }

    public int getBatterySOC() {
        return batterySOC;
    }

    public double getBatteryChargeInWatts() {
        return batteryChargeInWatts;
    }

    @Override
    public String toString() {
        return "Reading [time=" + time + ", homeOwnConsumptionInWatts=" + homeOwnConsumptionInWatts
                + ", pvProductionInWatts=" + pvProductionInWatts + ", totalActivePowerInWatts=" + getTotalActivePowerInWatts()
                + ", batterySOC=" + batterySOC + ", batteryChargeInWatts=" + getBatteryChargeInWatts() + "]";
    }
}