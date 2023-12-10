package de.axeluhl.kostal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import java.time.Instant;

/**
 * Expected format for standard input:
 * 
 * <pre>
 * time       Home own consumption    PV production     Total active power (powermeter)     Battery actual SOC      Battery Charge
 * </pre>
 * 
 * where the time stamp in the first column has nanosecond precision, the default for InfluxDB output.
 * <p>
 * 
 * The {@link #main(String[])} method will dump an extended format to the standard output, augmenting the input by
 * additional columns for an inferred battery SOC and an inferred battery charge/discharge power, based on an assumption
 * of how much capacity the battery has.
 */
public class AggregateBatteryDischarge {
    private static class Reading {
        private final Instant time;

        private final double homeOwnConsumptionInWatts;

        private final double pvProductionInWatts;

        private final double totalActivePowerInWatts;

        private final int batterySOC;

        private final double batteryChargeInWatts;

        static Reading parse(String line) {
            final String[] fields = line.split("[ \t]+");
            return new Reading(Instant.ofEpochMilli(Long.valueOf(fields[0]) / 1000000l), // convert from nanos to millis
                    Double.valueOf(fields[1]), Double.valueOf(fields[2]), Double.valueOf(fields[3]),
                    Integer.valueOf(fields[4]), Double.valueOf(fields[4]));
        }

        Reading(Instant time, double homeOwnConsumptionInWatts, double pvProductionInWatts, double totalActivePowerInWatts,
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

    public static void main(String[] args) throws IOException {
        final Battery virtualBattery = new Battery(/* minSOCPercent */ 5, /* maxChargePowerInWatts */ 5600,
                /* reducedChargePowerInWatts */ 3000, /* socPercentWhereReducedChargePowerStarts */ 99.5,
                /* capacityInWattHours */ 10600, /* energyContaied */ 0, SavingsPerDischarge.FUNCTION);
        new AggregateBatteryDischarge().aggregateBatteryDischarge(virtualBattery,
                args.length == 0 ? new InputStreamReader(System.in) : new FileReader(args[0]));
        System.out.println(String.format("Aggregated discharge savings in EUR: %1.2f", virtualBattery.getSavingsInCents()/100.0));
    }

    public Battery aggregateBatteryDischarge(Battery virtualBattery, Reader in) throws IOException {
        final BufferedReader reader = new BufferedReader(in);
        boolean virtualBatterySOCInitialized = false;
        Instant lastTimestamp = null;
        double lastPowerAvailableForChargingInWatts = 0.0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                final Reading reading = Reading.parse(line);
                if (!virtualBatterySOCInitialized) {
                    virtualBattery.setSOCPercent(reading.getBatterySOC());
                    virtualBatterySOCInitialized = true;
                }
                if (lastTimestamp != null) {
                    virtualBattery.charge(lastPowerAvailableForChargingInWatts, lastTimestamp,
                            Duration.between(lastTimestamp, reading.getTime()));
                }
                lastTimestamp = reading.getTime();
                lastPowerAvailableForChargingInWatts = reading.getPvProductionInWatts()
                        - reading.getHomeOwnConsumptionInWatts();
            }
        }
        return virtualBattery;
    }
}
