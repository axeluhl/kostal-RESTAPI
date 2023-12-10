package de.axeluhl.kostal;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;

/**
 * Simulates a battery to which energy can be charged, and from which energy can be consumed. The battery assumes a loss
 * factor for energy charged, and a separate loss factor for energy discharged. The battery stops charging when full. It
 * models a very basic charging curve that reduces to a different, lower maximum charging power starting from a certain
 * charge state on.
 * <p>
 * 
 * Discharge energy is mapped to cost saved by assuming a constant compensation per energy ingested and a time-dependent
 * cost of power obtained from the grid. The savings can then be computed by multiplying the energy discharged by the
 * cost equivalent of that energy at the time of discharge, subtracting the ingestion compensation for that energy.
 * These cost savings are aggregated across the life time of this {@link Battery} object across all its
 * {@link #charge(double, Duration)} invocations.
 * 
 * @author Axel Uhl
 *
 */
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
    
    private final double minEnergyContainedInWattHours;

    private final double maxChargePowerInWatts;
    
    private final double reducedChargePowerInWatts;
    
    private final double socPercentWhereReducedChargePowerStarts;

    private final double capacityInWattHours;
    
    private final BiFunction<Instant, Double, Double> savingsFunctionInCentsPerWattHourDischarged;

    private double energyContainedInWattHours;
    
    private double savingsInCents;

    public Battery(int minSOCPercent, double maxChargePowerInWatts, double reducedChargePowerInWatts, double socPercentWhereReducedChargePowerStarts, double capacityInWattHours,
            double energyContainedInWattHours, BiFunction<Instant, Double, Double> savingsFunctionInCentsPerWattHourDischarged) {
        super();
        this.minEnergyContainedInWattHours = capacityInWattHours * minSOCPercent / 100.0;
        this.maxChargePowerInWatts = maxChargePowerInWatts;
        this.reducedChargePowerInWatts = reducedChargePowerInWatts;
        this.socPercentWhereReducedChargePowerStarts = socPercentWhereReducedChargePowerStarts;
        this.capacityInWattHours = capacityInWattHours;
        this.energyContainedInWattHours = energyContainedInWattHours;
        this.savingsFunctionInCentsPerWattHourDischarged = savingsFunctionInCentsPerWattHourDischarged;
        this.savingsInCents = 0.0;
    }

    public int getMinSOCPercent() {
        return (int) (minEnergyContainedInWattHours / capacityInWattHours * 100.0);
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
    public void charge(double powerInWatts, Instant when, Duration duration) {
        final double effectivePowerInWattsAfterCapping = Math.signum(powerInWatts)
                * Math.min(Math.abs(powerInWatts), energyContainedInWattHours / capacityInWattHours * 100.0 > socPercentWhereReducedChargePowerStarts ? reducedChargePowerInWatts : maxChargePowerInWatts);
        final double lossFactor = powerInWatts >= 0 ? CHARGE_LOSS_FACTOR : DISCHARGE_LOSS_FACTOR;
        final double energyToAddAfterCappingInWattHours = effectivePowerInWattsAfterCapping * lossFactor / 3600.0
                * duration.get(ChronoUnit.SECONDS);
        final double effectiveEnergyToAddInWattHours;
        if (powerInWatts < 0) {
            // don't discharge below min SOC
            effectiveEnergyToAddInWattHours = -Math.min(-energyToAddAfterCappingInWattHours, energyContainedInWattHours-minEnergyContainedInWattHours);
            savingsInCents += savingsFunctionInCentsPerWattHourDischarged.apply(when, effectiveEnergyToAddInWattHours);
        } else {
            // don't charge beyond capacity
            effectiveEnergyToAddInWattHours = Math.min(energyToAddAfterCappingInWattHours, capacityInWattHours-energyContainedInWattHours);
        }
        energyContainedInWattHours += effectiveEnergyToAddInWattHours;
    }
    
    public double getSavingsInCents() {
        return savingsInCents;
    }

    @Override
    public String toString() {
        return "Battery [minSOCPercent=" + getMinSOCPercent() + ", maxChargePowerInWatts=" + maxChargePowerInWatts
                + ", capacityInWattHours=" + capacityInWattHours + ", energyContainedInWattHours="
                + energyContainedInWattHours + ", getSOCPercent()=" + getSOCPercent()
                + "savingsInCents=" + savingsInCents + "]";
    }
}
