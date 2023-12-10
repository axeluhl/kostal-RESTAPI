package de.axeluhl.kostal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class AggregateBatteryDischarge {
	public void main(String[] args) throws IOException {
		new AggregateBatteryDischarge().aggregateBatteryDischarge(new InputStreamReader(System.in),
				new OutputStreamWriter(System.out)); 
	}

	public void aggregateBatteryDischarge(Reader in, Writer out) throws IOException {
		final BufferedReader reader = new BufferedReader(in);
		String line;
		while ((line = reader.readLine()) != null) {
			final String[] fields = line.split(" +");
			for (final String field : fields) {
				out.write(field);
				out.write('\n');
			}
		}
	}
}
