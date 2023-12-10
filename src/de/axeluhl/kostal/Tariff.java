package de.axeluhl.kostal;

import java.time.Instant;

/**
 * Models the tariff structure, giving electrical power prices per time range
 */
public enum Tariff {
	MAINGAU_OLD(Instant.ofEpochSecond(         0), 31),
	MAINGAU_WAR(Instant.ofEpochSecond(1672560003), 71.4),
	MAINGAU_NEW(Instant.ofEpochSecond(1672560003), 54.09),
	GRUENWELT  (Instant.ofEpochSecond(1701244803), 36.77);
	
	private Tariff(Instant startsAt, double centsPerKWh) {
		this.startsAt = startsAt;
		this.centsPerKWh = centsPerKWh;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public void setStartsAt(Instant startsAt) {
		this.startsAt = startsAt;
	}

	public double getCentsPerKWh() {
		return centsPerKWh;
	}

	public void setCentsPerKWh(double centsPerKWh) {
		this.centsPerKWh = centsPerKWh;
	}

	private Instant startsAt;
	private double centsPerKWh;
}
