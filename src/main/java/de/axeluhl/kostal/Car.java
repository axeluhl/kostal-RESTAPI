package de.axeluhl.kostal;

/**
 * Models an electric vehicle that can be charged on a wallbox. It has a capacity, a maximum charge current, and a
 * specific charging curve from which we can determine where in the charging cycle the car is, depending on the charging
 * current, assuming that the wallbox grants the car maximum power. Here, we assume that towards the end of the charging
 * cycle the car will reduce its charging power approximately along a linear function from its maximum charging power
 * down to zero. Therefore, if the car charges with less than the wallbox-allowed power and less than its maximum
 * charging power, then we can approximate its state of charge (SOC).<p>
 * 
 * Example: if the car charges with a maximum of 3.7kW, and the wallbox is set to allow 11kW, and yet the car draws
 * only 1.85kW from the wallbox, then we can assume that it will reach full charge in about as much time as has passed
 * since the point in time when charging power began to sink below the maximum charging power (and not throttled to that
 * very level by the wallbox). From the gradient we can approximate the energy that will still go into the car until full,
 * and with the {@link #batteryNetCapacityInKiloWattHours} we can then know the SOC.
 * <p>
 * 
 * The wallbox may also be on a restricted scheme, e.g., when controlled such that only excess solar energy is used
 * to charge the car. In this case we may see less than maximum charging power even if the car battery is not in
 * the final phase of its charging curve.
 * 
 * @author Axel Uhl
 *
 */
public class Car {
    /**
     * Anything below this power per phase will be considered baseline consumption for the wallbox
     * itself. We typically see an (erroneous) alleged output of 20-30W per phase after charging has
     * finished, even if the car if unplugged. We will simply ignore such power and the resulting
     * energy and assume that this energy is not used to charge the car battery.
     */
    private final static double MINIMUM_CHARGE_POWER_PER_PHASE_IN_WATTS = 900;
    
    private final static double POWER_MEASUREMENT_ACCURACY_IN_WATTS = 100;
    
    private final double maximumChargePowerInWatts;
    
    private final double batteryNetCapacityInKiloWattHours;
    
    private WallboxReading lastWallboxReading;
    
    /**
     * The state of charge (SOC) in percent
     */
    private double socInPercent;
    
    public Car(double maximumChargePowerInWatts, double batteryNetCapacityInKiloWattHours) {
        super();
        this.maximumChargePowerInWatts = maximumChargePowerInWatts;
        this.batteryNetCapacityInKiloWattHours = batteryNetCapacityInKiloWattHours;
        this.socInPercent = 0.0;
    }

    /**
     * Updates the car battery state depending on current approximated state of charge and the wallbox set-up
     * 
     * @param readingPv
     *            allows us to compute the current excess energy that can be ingested into the car
     * @param readingEbox
     *            has the power restrictions and current power per phase; this allows us to tell about the charging
     *            cycle (e.g., when less than the maximum car charging power is drawn by the car although the wallbox
     *            would allow for it, then we know that the car is about to reach the end of its charging cycle)
     */
    public void update(BatteryUseReading readingPv, WallboxReading readingEbox) {
        if (lastWallboxReading != null) {
            updateChargingStateBasedOnPowerReduction(readingPv, readingEbox);
            if (socInPercent < 100) {
                // TODO we're missing the interval duration between the previous and the current reading, so how to update the SOC appropriately?
            }
        }
        lastWallboxReading = readingEbox;
        // TODO Auto-generated method stub
    }

    private void updateChargingStateBasedOnPowerReduction(BatteryUseReading readingPv, WallboxReading readingEbox) {
        final double excessPVEnergyInWatts = readingPv.getExcessPVPowerInWatts();
        final double chargePowerDifferenceToMaximumPossibleInWatts = Math.min(readingEbox.getMaxPowerInWatts(), maximumChargePowerInWatts) - readingEbox.getPowerInWatts();
        if (chargePowerDifferenceToMaximumPossibleInWatts > POWER_MEASUREMENT_ACCURACY_IN_WATTS) {
            // the car is charging less than it would at its full charge power, considering any wallbox throttling;
            // therefore, we assume it is in the declining end of its charging curve, and we should look at the shape of
            // the declining curve
            // TODO check that we actually see an equal or declining charging power, considering POWER_MEASUREMENT_ACCURACY_IN_WATTS as a tolerance level
            // TODO record the reading identified to be part of the declining part of the charging curve
        }
        // TODO Auto-generated method stub
    }
}
