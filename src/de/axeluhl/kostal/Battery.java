package de.axeluhl.kostal;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class Battery {
	/**
	 * The share of energy lost when using an amount of energy to charge this
	 * battery
	 */
	private final double chargeLossFactor = 0.015;

	/**
	 * The share of energy lost when drawing an amount of energy from this battery
	 */
	private final double dischargeLossFactor = 0.015;
	private final int minSOCPercent;
	private final double maxChargePowerInWatts;
	private final double capacityInWattHours;

	private double energyContainedInWattHours;

	public Battery(int minSOCPercent, double maxChargePowerInWatts, double capacityInWattHours,
			double energyContainedInWattHours) {
		super();
		this.minSOCPercent = minSOCPercent;
		this.maxChargePowerInWatts = maxChargePowerInWatts;
		this.capacityInWattHours = capacityInWattHours;
		this.energyContainedInWattHours = energyContainedInWattHours;
	}

	public int getMinSOCPercent() {
		return minSOCPercent;
	}

	public double getMaxChargePowerInWatts() {
		return maxChargePowerInWatts;
	}

	public double getCapacityInWattHours() {
		return capacityInWattHours;
	}

	public double getEnergyContainedInWattHours() {
		return energyContainedInWattHours;
	}

	public int getSOCPercent() {
		return (int) Math.round(100.0 * energyContainedInWattHours / capacityInWattHours);
	}
	
	public void setSOCPercent(int batterySOC) {
		energyContainedInWattHours = (double) batterySOC * capacityInWattHours / 100.0;
	}

	/**
	 * Adjusts this battery's {@link #getEnergyContainedInWattHours()} based on the
	 * power used to charge/discharge and the duration for which
	 * charging/discharging at this power. Charging / discharging is limited by the
	 * {@link #getMaxChargePowerInWatts()}, assuming that charge and discharge power
	 * are limited by the same value; and it is limited by the
	 * {@link #getCapacityInWattHours() battery's capacity} as well as the
	 * {@link #getMinSOCPercent() minimum state of charge}.
	 * 
	 * @param powerInWatts use negative numbers to reflect discharging; wil be
	 *                     capped at +/- {@link #getMaxChargePowerInWatts()}.
	 */
	public void charge(double powerInWatts, Duration duration) {
		// don't discharge below min SOC
		if (powerInWatts > 0 || getSOCPercent() > getMinSOCPercent()) {
			final double effectivePowerInWattsAfterCapping = Math.signum(powerInWatts) *
					Math.max(Math.abs(powerInWatts), getMaxChargePowerInWatts());
			final double lossFactor = powerInWatts >= 0 ? chargeLossFactor : dischargeLossFactor;
			energyContainedInWattHours += effectivePowerInWattsAfterCapping * lossFactor * duration.get(ChronoUnit.HOURS);
			if (energyContainedInWattHours > getCapacityInWattHours()) {
				energyContainedInWattHours = getCapacityInWattHours();
			}
		}
	}

	@Override
	public String toString() {
		return "Battery [minSOCPercent=" + minSOCPercent + ", maxChargePowerKW=" + maxChargePowerInWatts
				+ ", capacityInKWh=" + capacityInWattHours + ", energyContainedInKWh=" + energyContainedInWattHours
				+ ", getSOCPercent()=" + getSOCPercent() + "]";
	}
}
