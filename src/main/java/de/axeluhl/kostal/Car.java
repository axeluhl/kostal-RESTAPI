package de.axeluhl.kostal;

/**
 * Models an electric vehicle that can be charged on a wallbox. It has a capacity, a maximum charge
 * current, and a specific charging curve from which we can determine where in the charging cycle
 * the car is depending on the charging current, assuming that the wallbox grants the car maximum
 * power.
 * 
 * @author Axel Uhl
 *
 */
public class Car {
	/**
	 * Updates the car battery state depending on current approximated state of
	 * charge and the wallbox set-up
	 * 
	 * @param readingPv   allows us to compute the current excess energy that can be
	 *                    ingested into the car
	 * @param readingEbox has the power restrictions and current power per phase;
	 *                    this allows us to tell about the charging cycle (e.g.,
	 *                    when less than the maximum car charging power is drawn by
	 *                    the car although the wallbox would allow for it, then we
	 *                    know that the car is about to reach the end of its
	 *                    charging cycle)
	 */
	public void update(BatteryUseReading readingPv, WallboxReading readingEbox) {
		// TODO Auto-generated method stub
		
	}
}
