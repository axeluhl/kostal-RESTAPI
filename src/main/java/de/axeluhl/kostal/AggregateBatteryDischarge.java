package de.axeluhl.kostal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import java.time.Instant;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
    private static final int DEFAULT_CAPACITY_IN_WATT_HOURS = 10240;
    private static final double DEFAULT_SOC_PERCENT_WHERE_REDUCED_CHARGE_POWER_STARTS = 99.5;
    private static final int DEFAULT_REDUCED_CHARGE_POWER_IN_WATTS = 3400;
    private static final int DEFAULT_MAX_CHARGE_POWER_IN_WATTS = 5600;
    private static final int DEFAULT_MIN_SOC_PERCENT = 5;

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
        final Option minSOCPercent = Option.builder("m")
                .longOpt("minSOCPercent")
                .hasArg()
                .argName("minSOCPercent")
                .type(Number.class)
                .desc("minimum state of charge (SOC) in percent; defaults to "+DEFAULT_MIN_SOC_PERCENT)
                .build();
        final Option maxChargePowerInWatts = Option.builder("x")
                .longOpt("maxChargePowerInWatts")
                .hasArg()
                .argName("maxChargePowerInWatts")
                .type(Number.class)
                .desc("maximum charge power in Watts; defaults to "+DEFAULT_MAX_CHARGE_POWER_IN_WATTS)
                .build();
        final Option capacityInWattHours = Option.builder("c")
                .longOpt("capacityInWattHours")
                .hasArg()
                .argName("capacityInWattHours")
                .type(Number.class)
                .desc("capacity in Watt-Hours; defaults to "+DEFAULT_CAPACITY_IN_WATT_HOURS)
                .build();
        final Option reducedChargePowerInWatts = Option.builder("r")
                .longOpt("reducedChargePowerInWatts")
                .hasArg()
                .argName("reducedChargePowerInWatts")
                .type(Number.class)
                .desc("reduced charge power in Watts; defaults to "+DEFAULT_REDUCED_CHARGE_POWER_IN_WATTS)
                .build();
        final Option socPercentWhereReducedChargePowerStarts = Option.builder("s")
                .longOpt("socPercentWhereReducedChargePowerStarts")
                .hasArg()
                .argName("socPercentWhereReducedChargePowerStarts")
                .type(Number.class)
                .desc("state of charge (SOC, in percent) where reduced charge power starts; defaults to "+DEFAULT_SOC_PERCENT_WHERE_REDUCED_CHARGE_POWER_STARTS)
                .build();
        final Option inputFile = Option.builder("f")
                .longOpt("file")
                .hasArg()
                .argName("inputFile")
                .type(FileInputStream.class)
                .desc("input file from which to read inverter states; defaults to stdin")
                .build();
        final Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("display help message")
                .build();
        final Option helpOption2 = Option.builder("?")
                .desc("display help message")
                .build();
        final Options options = new Options()
                .addOption(minSOCPercent)
                .addOption(maxChargePowerInWatts)
                .addOption(capacityInWattHours)
                .addOption(reducedChargePowerInWatts)
                .addOption(socPercentWhereReducedChargePowerStarts)
                .addOption(inputFile)
                .addOption(helpOption)
                .addOption(helpOption2);
        final CommandLineParser commandLineParser = new DefaultParser();
        try {
            final CommandLine commandLine = commandLineParser.parse(options, args);
            if (commandLine.hasOption(helpOption) || commandLine.hasOption(helpOption2)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(AggregateBatteryDischarge.class.getName(), options);
            } else {
                final Battery virtualBattery = new Battery(
                        commandLine.hasOption(minSOCPercent) ? ((Number) commandLine.getParsedOptionValue(minSOCPercent)).intValue() : DEFAULT_MIN_SOC_PERCENT,
                        commandLine.hasOption(maxChargePowerInWatts) ? ((Number) commandLine.getParsedOptionValue(maxChargePowerInWatts)).doubleValue() : DEFAULT_MAX_CHARGE_POWER_IN_WATTS,
                        commandLine.hasOption(reducedChargePowerInWatts) ? ((Number) commandLine.getParsedOptionValue(reducedChargePowerInWatts)).doubleValue() : DEFAULT_REDUCED_CHARGE_POWER_IN_WATTS,
                        commandLine.hasOption(socPercentWhereReducedChargePowerStarts) ? ((Number) commandLine.getParsedOptionValue(socPercentWhereReducedChargePowerStarts)).doubleValue() : DEFAULT_SOC_PERCENT_WHERE_REDUCED_CHARGE_POWER_STARTS,
                        commandLine.hasOption(capacityInWattHours) ? ((Number) commandLine.getParsedOptionValue(capacityInWattHours)).doubleValue() : DEFAULT_CAPACITY_IN_WATT_HOURS,
                        /* energyContained */ 0, SavingsPerDischarge.FUNCTION);
                new AggregateBatteryDischarge().aggregateBatteryDischarge(virtualBattery,
                        new InputStreamReader(commandLine.hasOption(inputFile) ? (FileInputStream) commandLine.getParsedOptionValue(inputFile) : System.in));
                System.out.println(String.format("Aggregated discharge savings in EUR: %1.2f", virtualBattery.getSavingsInCents()/100.0));
            }
        } catch (ParseException e) {
            System.err.println("Parsing failed. Reason: " + e.getMessage());
        }
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
