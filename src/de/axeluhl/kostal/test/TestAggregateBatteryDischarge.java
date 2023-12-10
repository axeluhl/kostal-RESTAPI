package de.axeluhl.kostal.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;

import de.axeluhl.kostal.AggregateBatteryDischarge;

public class TestAggregateBatteryDischarge {
	@Test
	public void testBasicWordSplitting() throws IOException {
		final AggregateBatteryDischarge a = new AggregateBatteryDischarge();
		final StringWriter result = new StringWriter();
		a.aggregateBatteryDischarge(new StringReader("abc def"), result);
		assertEquals("abc\ndef\n", result.toString());
	}
}
