package de.axeluhl.kostal;

public enum IngestionCompensation {
    COMPENSATION_2020(9.6);
    
    private double centsPerKilowattHour;

    IngestionCompensation(double centsPerKilowattHour) {
        this.centsPerKilowattHour = centsPerKilowattHour;
    }
    
    public double getCompensationInCents(double energyInWattHours) {
        return energyInWattHours / 1000.0 * centsPerKilowattHour;
    }
}
