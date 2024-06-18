package de.axeluhl.kostal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Reads from two files whose name are provided as arguments, produced by the {@code kostal-dumpCarChargeBehavior}
 * script; the first being {@code kostal-dumpCarChargeBehavior-pv.out} with the following expected format:
 * 
 * <pre>
 * time       Home own consumption    PV production     Total active power (powermeter)     Battery actual SOC      Battery Charge
 * </pre>
 * 
 * where the time stamp in the first column has nanosecond precision, the default for InfluxDB output. The second is
 * expected to be {@code kostal-dumpCarChargeBehavior-ebox.out}, expected in the following format:
 * 
 * <pre>
 * time CurrentPhase1    CurrentPhase2   CurrentPhase3  MaxCurrentPhase1    MaxCurrentPhase2   MaxCurrentPhase3  Socket1CableState    Socket1Mode3State
 * </pre>
 * 
 * All columns in these files are expected to be separated by tabs. The two files are not expected to be in matching
 * chronology, so time series in the two files may have different time resolutions, sampling rates, and even span
 * different time ranges.
 * <p>
 * 
 * TODO The {@link #main(String[])} method will try to figure out:
 * <ul>
 * <li>How much energy the car charged in total</li>
 * <li>How much the car charged from the grid</li>
 * <li>How much the car charged from the PV where this energy would otherwise have been ingested</li>
 * <li>How much energy was ingested because the car was still attached but the battery was already full</li>
 * </ul>
 * 
 * @author Axel Uhl
 */
public class AggregateCarCharge {
    private static final int DEFAULT_CAPACITY_IN_WATT_HOURS = 10240;

    private static final double DEFAULT_SOC_PERCENT_WHERE_REDUCED_CHARGE_POWER_STARTS = 99.5;

    private static final int DEFAULT_REDUCED_CHARGE_POWER_IN_WATTS = 3400;

    private static final int DEFAULT_MAX_CHARGE_POWER_IN_WATTS = 5600;

    private static final int DEFAULT_MIN_SOC_PERCENT = 5;
    
    private static final int DEFAULT_CAR_BATTERY_NET_CAPACITY_IN_WATT_HOURS = 10300;
    
    private static final double DEFAULT_CAR_MAXIMUM_CHARGE_POWER_IN_WATTS = 3510;

    public static void main(String[] args) throws IOException {
        final Option carBatteryNetCapacityInWattHours = Option.builder("k").longOpt("carCapacityInWattHours").hasArg().argName("carCapacityInWattHours")
                .type(Number.class)
                .desc("car battery net capacity in Wh; defaults to " + DEFAULT_CAR_BATTERY_NET_CAPACITY_IN_WATT_HOURS).build();
        final Option carMaximumChargePowerInWatts = Option.builder("m").longOpt("carMaximumChargePowerInWatts").hasArg().argName("carMaximumChargePowerInWatts")
                .type(Number.class)
                .desc("car maximum charging power in Watts; defaults to " + DEFAULT_CAR_BATTERY_NET_CAPACITY_IN_WATT_HOURS).build();
        final Option minSOCPercent = Option.builder("m").longOpt("minSOCPercent").hasArg().argName("minSOCPercent")
                .type(Number.class)
                .desc("minimum state of charge (SOC) in percent; defaults to " + DEFAULT_MIN_SOC_PERCENT).build();
        final Option maxChargePowerInWatts = Option.builder("x").longOpt("maxChargePowerInWatts").hasArg()
                .argName("maxChargePowerInWatts").type(Number.class)
                .desc("maximum charge power in Watts; defaults to " + DEFAULT_MAX_CHARGE_POWER_IN_WATTS).build();
        final Option capacityInWattHours = Option.builder("c").longOpt("capacityInWattHours").hasArg()
                .argName("capacityInWattHours").type(Number.class)
                .desc("capacity in Watt-Hours; defaults to " + DEFAULT_CAPACITY_IN_WATT_HOURS).build();
        final Option reducedChargePowerInWatts = Option.builder("r").longOpt("reducedChargePowerInWatts").hasArg()
                .argName("reducedChargePowerInWatts").type(Number.class)
                .desc("reduced charge power in Watts; defaults to " + DEFAULT_REDUCED_CHARGE_POWER_IN_WATTS).build();
        final Option socPercentWhereReducedChargePowerStarts = Option.builder("s")
                .longOpt("socPercentWhereReducedChargePowerStarts").hasArg()
                .argName("socPercentWhereReducedChargePowerStarts").type(Number.class)
                .desc("state of charge (SOC, in percent) where reduced charge power starts; defaults to "
                        + DEFAULT_SOC_PERCENT_WHERE_REDUCED_CHARGE_POWER_STARTS)
                .build();
        final Option inputFilePv = Option.builder("f").longOpt("pvfile").hasArg().argName("inputFilePv")
                .type(FileInputStream.class).desc("input file from which to read inverter states; defaults to stdin")
                .build();
        final Option inputFileEbox = Option.builder("e").longOpt("eboxfile").hasArg().argName("inputFileEbox")
                .type(FileInputStream.class).desc("input file from which to read wallbox states").build();
        final Option helpOption = Option.builder("h").longOpt("help").desc("display help message").build();
        final Option helpOption2 = Option.builder("?").desc("display help message").build();
        final Options options = new Options()
                .addOption(carBatteryNetCapacityInWattHours)
                .addOption(carMaximumChargePowerInWatts)
                .addOption(minSOCPercent)
                .addOption(maxChargePowerInWatts)
                .addOption(capacityInWattHours)
                .addOption(reducedChargePowerInWatts)
                .addOption(socPercentWhereReducedChargePowerStarts)
                .addOption(inputFilePv)
                .addOption(inputFileEbox)
                .addOption(helpOption)
                .addOption(helpOption2);
        final CommandLineParser commandLineParser = new DefaultParser();
        try {
            final CommandLine commandLine = commandLineParser.parse(options, args);
            if (commandLine.hasOption(helpOption) || commandLine.hasOption(helpOption2)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(AggregateCarCharge.class.getName(), options);
            } else {
                final Car virtualCar = new Car(carMaximumChargePowerInWatts.getArgs(), carBatteryNetCapacityInWattHours.getArgs());
                new AggregateCarCharge().aggregateCarCharging(virtualCar,
                        new InputStreamReader(commandLine.hasOption(inputFilePv)
                                ? (FileInputStream) commandLine.getParsedOptionValue(inputFilePv)
                                : System.in),
                        new InputStreamReader((FileInputStream) commandLine.getParsedOptionValue(inputFileEbox)));
//                System.out.println(String.format("Aggregated discharge savings in EUR: %1.2f", virtualCar.getSavingsInCents()/100.0));
            }
        } catch (ParseException e) {
            System.err.println("Parsing failed. Reason: " + e.getMessage());
        }
    }

    public Car aggregateCarCharging(Car virtualCar, Reader inPv, Reader inEbox) throws IOException {
        final BufferedReader readerPv = new BufferedReader(inPv);
        final BufferedReader readerEbox = new BufferedReader(inEbox);
        boolean virtualBatterySOCInitialized = false;
        Instant lastTimestamp = null;
        double lastPowerAvailableForChargingInWatts = 0.0;
        String linePv = readerPv.readLine();
        String nextLinePv = readerPv.readLine();
        BatteryUseReading readingPv = BatteryUseReading.parse(linePv);
        while (readingPv == null) do {
            linePv = nextLinePv;
            nextLinePv = readerPv.readLine();
            readingPv = BatteryUseReading.parse(linePv);
        } while (readingPv == null);
        String lineEbox = readerEbox.readLine();
        String nextLineEbox = readerEbox.readLine();
        WallboxReading readingEbox = WallboxReading.parse(lineEbox);
        WallboxReading nextReadingEbox = WallboxReading.parse(nextLineEbox);
        // skip PV readings until we reach the first eBox reading:
        while (linePv != null && (readingPv == null || readingPv.getTime().isBefore(readingEbox.getTime()))) {
            linePv = nextLinePv;
            nextLinePv = readerPv.readLine();
            readingPv = BatteryUseReading.parse(linePv);
        }
        // now readingPv is at or after readingEbox; read until PV reading's time point is at or after nextReadingEbox
        while (nextReadingEbox != null && readingPv != null) { // FIXME look at lines being null and skip unparsable null readings
            readingPv = BatteryUseReading.parse(linePv);
            if (!readingPv.getTime().isBefore(nextReadingEbox.getTime())) {
                lineEbox = nextLineEbox;
                readingEbox = nextReadingEbox;
                nextLineEbox = readerEbox.readLine();
                nextReadingEbox = WallboxReading.parse(nextLineEbox);
            }
            lastTimestamp = readingPv.getTime();
            lastPowerAvailableForChargingInWatts = readingPv.getPvProductionInWatts()
                    - readingPv.getHomeOwnConsumptionInWatts();
            virtualCar.update(readingPv, readingEbox);
            if (!virtualBatterySOCInitialized) {
//                    virtualBattery.setSOCPercent(reading.getBatterySOC());
//                    virtualBatterySOCInitialized = true;
            }
            if (lastTimestamp != null) {
//                    virtualBattery.charge(lastPowerAvailableForChargingInWatts, lastTimestamp,
//                            Duration.between(lastTimestamp, reading.getTime()));
            }
            linePv = nextLinePv;
            nextLinePv = readerPv.readLine();
        }
        return virtualCar;
    }
}
