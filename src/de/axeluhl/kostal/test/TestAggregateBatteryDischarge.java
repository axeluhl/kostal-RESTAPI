package de.axeluhl.kostal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

import de.axeluhl.kostal.AggregateBatteryDischarge;
import de.axeluhl.kostal.Battery;
import de.axeluhl.kostal.Tariff;

public class TestAggregateBatteryDischarge {
	@Test
	public void testBasicWordSplitting() throws IOException {
		final AggregateBatteryDischarge a = new AggregateBatteryDischarge();
		final Battery virtualBattery = new Battery(/* minSOCPercent */ 5, /* maxChargePowerInWatts */ 5600, /* capacityInWattHours */ 10600, /* energyContaiedWh */ 1000);
		a.aggregateBatteryDischarge(virtualBattery, new StringReader(
				"1702236023000000000 545                  0             544.8                           5                  0\n"+
				"1702236028000000000 545                  0             544.8                           5                  0\n"
				));
		assertEquals(999.0, virtualBattery.getEnergyContainedInWattHours(), 0.0000001);
	}
	
	@Test
	public void testBatterySOC() {
		final Battery battery = new Battery(/* minSOCPercent */ 5, /* maxChargePowerInWatts */ 5600, /* capacityInWattHours */ 10600, /* energyContained */ 0);
		battery.setSOCPercent(100);
		assertEquals(100, battery.getSOCPercent(), 0.000001);
	}
	
	@Test
	public void testBatteryMinSOC() {
		final Battery battery = new Battery(/* minSOCPercent */ 5, /* maxChargePowerInWatts */ 5600, /* capacityInWattHours */ 10600, /* energyContainedInWh */ 0);
		battery.setSOCPercent(battery.getMinSOCPercent());
		battery.charge(-5000, Duration.ofMinutes(10));
		assertEquals(battery.getMinSOCPercent(), battery.getSOCPercent(), 0.000001);
	}
	
	@Test
	public void testTariffsAscendingInTimepoints() {
		Instant i = null;
		for (final Tariff t : Tariff.values()) {
			if (i != null) {
				assertTrue(i.compareTo(t.getStartsAt()) < 0);
			}
			i = t.getStartsAt();
		}
	}
}
