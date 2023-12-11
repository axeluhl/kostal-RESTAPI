package de.axeluhl.kostal;

import java.time.Instant;
import java.util.function.BiFunction;

public interface SavingsPerDischarge {
    BiFunction<Instant, Double, Double> FUNCTION = (when, energyInWattHours) -> {
        return Tariff.getCents(when, energyInWattHours) - IngestionCompensation.COMPENSATION_2020.getCompensationInCents(energyInWattHours);
    };
}
