package de.axeluhl.kostal;

import java.time.Duration;

public class Battery {
	private final int minSOCPercent;
	private final double maxChargePowerInWatts;
	private final double capacityInWattHours;
	private final double energyContainedInWattHours;

	public Battery(int minSOCPercent, double maxChargePowerInWatts, double capacityInWattHours, double energyContainedInWattHours) {
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
		return (int) (energyContainedInWattHours / capacityInWattHours);
	}
	
	public void charge(double powerInW, Duration duration) {
		
	}

	@Override
	public String toString() {
		return "Battery [minSOCPercent=" + minSOCPercent + ", maxChargePowerKW=" + maxChargePowerInWatts + ", capacityInKWh="
				+ capacityInWattHours + ", energyContainedInKWh=" + energyContainedInWattHours + ", getSOCPercent()="
				+ getSOCPercent() + "]";
	}
}
