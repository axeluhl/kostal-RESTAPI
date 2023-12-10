package de.axeluhl.kostal;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class Battery {
    /**
     * The fraction of energy actually stored by the battery when sending energy to it. Example: when sending 1kWh of
     * energy to the battery, it stores 1kWh * {@link #CHARGE_LOSS_FACTOR} of it.
     */
    public static final double CHARGE_LOSS_FACTOR = 0.985;

    /**
     * The factor of energy depletion in the battery per energy effectively extracted from it. Example: to consume 1kWh
     * of energy from the battery, the battery SOC decreases by 1kWh * {@link #DISCHARGE_LOSS_FACTOR}.
     */
    public static final double DISCHARGE_LOSS_FACTOR = 1.015;

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
        energyContainedInWattHours = batterySOC * capacityInWattHours / 100.0;
    }

    /**
     * Adjusts this battery's {@link #getEnergyContainedInWattHours()} based on the power used to charge/discharge and
     * the duration for which charging/discharging at this power. Charging / discharging is limited by the
     * {@link #getMaxChargePowerInWatts()}, assuming that charge and discharge power are limited by the same value; and
     * it is limited by the {@link #getCapacityInWattHours() battery's capacity} as well as the
     * {@link #getMinSOCPercent() minimum state of charge}.
     * 
     * @param powerInWatts
     *            use negative numbers to reflect discharging; wil be capped at +/- {@link #getMaxChargePowerInWatts()}.
     */
    public void charge(double powerInWatts, Duration duration) {
        // don't discharge below min SOC
        if (powerInWatts > 0 || getSOCPercent() > getMinSOCPercent()) {
            final double effectivePowerInWattsAfterCapping = Math.signum(powerInWatts)
                    * Math.min(Math.abs(powerInWatts), getMaxChargePowerInWatts());
            final double lossFactor = powerInWatts >= 0 ? CHARGE_LOSS_FACTOR : DISCHARGE_LOSS_FACTOR;
            energyContainedInWattHours += effectivePowerInWattsAfterCapping * lossFactor / 3600.0
                    * duration.get(ChronoUnit.SECONDS);
            if (energyContainedInWattHours > getCapacityInWattHours()) {
                energyContainedInWattHours = getCapacityInWattHours();
            }
        }
    }

    @Override
    public String toString() {
        return "Battery [minSOCPercent=" + minSOCPercent + ", maxChargePowerInWatts=" + maxChargePowerInWatts
                + ", capacityInWattHours=" + capacityInWattHours + ", energyContainedInWattHours="
                + energyContainedInWattHours + ", getSOCPercent()=" + getSOCPercent() + "]";
    }
}
