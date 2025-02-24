package de.axeluhl.kostal;

import java.io.BufferedReader;
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
 * where the time stamp in the first column has nanosecond precision, the default for InfluxDB output. This output can
 * be obtained using the script <tt>kostal-dumpBatteryUse</tt> from this project's root folder. "Home own consumption"
 * refers to the power currently consumed (including wallbox and/or heat pump). "Total active power" refers to the
 * grid connection, so what is consumed from or ingested into the grid.
 * <p>
 * 
 * The {@link #main(String[])} method will show the savings by the battery over the time range covered by the
 * data provided through the standard input, computed under consideration of the battery characteristics such as
 * maximum charge/discharge current, capacity, losses, and the tariffs that apply at each respective time point.
 */
public class AggregateBatteryDischarge {
    private static final int DEFAULT_CAPACITY_IN_WATT_HOURS = 10240;
    private static final double DEFAULT_SOC_PERCENT_WHERE_REDUCED_CHARGE_POWER_STARTS = 99.5;
    private static final int DEFAULT_REDUCED_CHARGE_POWER_IN_WATTS = 3400;
    private static final int DEFAULT_MAX_CHARGE_POWER_IN_WATTS = 5600;
    private static final int DEFAULT_MIN_SOC_PERCENT = 5;

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
                final BatteryUseReading reading = BatteryUseReading.parse(line);
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
