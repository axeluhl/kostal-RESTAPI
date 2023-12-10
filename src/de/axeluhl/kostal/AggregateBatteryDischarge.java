package de.axeluhl.kostal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;

/**
 * Expected format for standard input:
 * 
 * <pre>
 * time       Home own consumption    PV production     Total active power (powermeter)     Battery actual SOC      Battery Charge
 * </pre>
 * 
 * where the time stamp in the first column has nanosecond precision, the
 * default for InfluxDB output.
 * <p>
 * 
 * The {@link #main(String[])} method will dump an extended format to the
 * standard output, augmenting the input by additional columns for an inferred
 * battery SOC and an inferred battery charge/discharge power, based on an
 * assumption of how much capacity the battery has.
 */
public class AggregateBatteryDischarge {
	private static class Reading {
		private final Instant time;
		private final double homeOwnConsumptionInWatts;
		private final double pvProductionInWatts;
		private final double totalActivePowerInKW;
		private final int batterySOC;
		private final double batteryChargeInKW;

		static Reading parse(String line) {
			final String[] fields = line.split(" +");
			return new Reading(
					Instant.ofEpochMilli(Long.valueOf(fields[0])/1000000l), // convert from nanos to millis
					Double.valueOf(fields[1]),
					Double.valueOf(fields[2]),
					Double.valueOf(fields[3]),
					Integer.valueOf(fields[4]), Double.valueOf(fields[4]));
		}

		Reading(Instant time, double homeOwnConsumptionInWatts, double pvProductionInWatts, double totalActivePowerInKW, int batterySOC, double batteryChargeInKW) {
			super();
			this.time = time;
			this.homeOwnConsumptionInWatts = homeOwnConsumptionInWatts;
			this.pvProductionInWatts = pvProductionInWatts;
			this.totalActivePowerInKW = totalActivePowerInKW;
			this.batterySOC = batterySOC;
			this.batteryChargeInKW = batteryChargeInKW;
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

		public double getTotalActivePowerInKW() {
			return totalActivePowerInKW;
		}

		public int getBatterySOC() {
			return batterySOC;
		}

		public double getBatteryChargeInKW() {
			return batteryChargeInKW;
		}

		@Override
		public String toString() {
			return "Reading [time=" + time + ", homeOwnConsumptionInWatts=" + homeOwnConsumptionInWatts
					+ ", pvProductionInWatts=" + pvProductionInWatts + ", totalActivePowerInKW=" + totalActivePowerInKW
					+ ", batterySOC=" + batterySOC + ", batteryChargeInKW=" + batteryChargeInKW + "]";
		}
	}
	
	private static class ReadingWithAggregates extends Reading {
		private final int virtualBatterySOC;

		public ReadingWithAggregates(Instant time, double homeOwnConsumptionInWatts, double pvProductionInWatts, double totalActivePowerInKW,
				int batterySOC, double batteryChargeInKW, int virtualBatterySOC) {
			super(time, homeOwnConsumptionInWatts, pvProductionInWatts, totalActivePowerInKW, batterySOC, batteryChargeInKW);
			this.virtualBatterySOC = virtualBatterySOC;
		}

		public int getVirtualBatterySOC() {
			return virtualBatterySOC;
		}
	}

	public void main(String[] args) throws IOException {
		final Battery virtualBattery = new Battery(/* minSOCPercent */ 5, /* maxChargePowerInWatts */ 5600, /* capacityInWattHours */ 10600, /* energyContaied */ 0);
		new AggregateBatteryDischarge().aggregateBatteryDischarge(virtualBattery,
				new InputStreamReader(System.in));
	}

	public Battery aggregateBatteryDischarge(Battery virtualBattery, Reader in) throws IOException {
		final BufferedReader reader = new BufferedReader(in);
		boolean virtualBatterySOCInitialized = false;
		Instant lastTimestamp = null;
		double lastEnergyAvailableForCharging = 0.0;
		String line;
		while ((line = reader.readLine()) != null) {
			final Reading reading = Reading.parse(line);
			if (!virtualBatterySOCInitialized) {
				virtualBattery.setSOCPercent(reading.getBatterySOC());
				virtualBatterySOCInitialized = true;
			}
			if (lastTimestamp != null) {
				virtualBattery.charge(lastEnergyAvailableForCharging, Duration.between(lastTimestamp, reading.getTime()));
			}
			lastTimestamp = reading.getTime();
			lastEnergyAvailableForCharging = reading.getPvProductionInWatts() - reading.getHomeOwnConsumptionInWatts();
		}
		return virtualBattery;
	}
}
