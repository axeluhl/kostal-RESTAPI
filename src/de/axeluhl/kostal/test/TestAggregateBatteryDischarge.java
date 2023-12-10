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
    public void testBasicAggregation() throws IOException {
        final AggregateBatteryDischarge a = new AggregateBatteryDischarge();
        final Battery virtualBattery = new Battery(/* minSOCPercent */ 5, /* maxChargePowerInWatts */ 5600,
                /* reducedChargePowerInWatts */ 3000, /* socPercentWhereReducedChargePowerStarts */ 99.5,
                /* capacityInWattHours */ 10600, /* energyContainedWh */ 1000, Tariff::getCents);
        // now look at 5s at 544.8W of consumption; this is
        a.aggregateBatteryDischarge(virtualBattery, new StringReader(
                "1702236023000000000 545                  0             544.8                           9                  0\n" +
                "1702236028000000000 545                  0             544.8                           9                  0\n"));
        assertEquals(9.0 / 100.0 * 10600.0 - 5.0 / 3600.0 * 545.0 * Battery.DISCHARGE_LOSS_FACTOR,
                virtualBattery.getEnergyContainedInWattHours(), 0.0000001);
    }
    
    @Test
    public void testBatteryMinSOCPercent() {
        final Battery virtualBattery = new Battery(/* minSOCPercent */ 5, /* maxChargePowerInWatts */ 5600,
                /* reducedChargePowerInWatts */ 3000, /* socPercentWhereReducedChargePowerStarts */ 99.5,
                /* capacityInWattHours */ 10600, /* energyContainedWh */ 1000, Tariff::getCents);
        assertEquals(5, virtualBattery.getMinSOCPercent());
    }

    @Test
    public void testBatterySOC() {
        final Battery battery = new Battery(/* minSOCPercent */ 5, /* maxChargePowerInWatts */ 5600,
                /* reducedChargePowerInWatts */ 3000, /* socPercentWhereReducedChargePowerStarts */ 99.5,
                /* capacityInWattHours */ 10600, /* energyContained */ 0, Tariff::getCents);
        battery.setSOCPercent(100);
        assertEquals(100, battery.getSOCPercent(), 0.000001);
    }

    @Test
    public void testBatteryMinSOC() {
        final Battery battery = new Battery(/* minSOCPercent */ 5, /* maxChargePowerInWatts */ 5600,
                /* reducedChargePowerInWatts */ 3000, /* socPercentWhereReducedChargePowerStarts */ 99.5,
                /* capacityInWattHours */ 10600, /* energyContainedInWh */ 0, Tariff::getCents);
        battery.setSOCPercent(battery.getMinSOCPercent());
        battery.charge(-5000, Instant.now(), Duration.ofMinutes(10));
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
