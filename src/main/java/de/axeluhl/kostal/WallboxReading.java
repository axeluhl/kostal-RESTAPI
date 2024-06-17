package de.axeluhl.kostal;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

class WallboxReading {
    private final Instant time;
    private final double currentPhase1InAmperes;

    private final double currentPhase2InAmperes;

    private final double currentPhase3InAmperes;
    
    private final double maxCurrentPhase1InAmperes;

    private final double maxCurrentPhase2InAmperes;

    private final double maxCurrentPhase3InAmperes;
    
    /**
     * Cable State (0=no cable; 1=cable w/o car; 2=unlocked cable w/o car; 3=locked cable w/ car)
     */
    static enum CableState {
        NO_CABLE(0),
        CABLE_WITHOUT_CAR(1),
        UNLOCKED_CABLE_WITHOUT_CAR(2),
        LOCKED_CABLE_WITH_CAR(3);

    	private static CableState[] statesByCode;
    	
    	private final int code;
        
        static CableState ofCode(int code) {
            return statesByCode[code];
        }
        
        private CableState(int code) {
            this.code = code;
            cache(this);
        }
        
        private void cache(CableState cableState) {
        	if (statesByCode == null) {
        		 statesByCode = new CableState[4];
        	}
            statesByCode[cableState.getCode()] = cableState;
        }
        
        public int getCode() {
            return code;
        }
    }
    
    private final CableState socket1CableState;

    /**
     * From <a href="https://www.toyota-tech.eu/aimuploads/f66893c3-0be7-447b-9b3a-5cd2efafe54b/1.Wallbox%20Training%20Toyota.pdf">
     * https://www.toyota-tech.eu/aimuploads/f66893c3-0be7-447b-9b3a-5cd2efafe54b/1.Wallbox%20Training%20Toyota.pdf</a>:
     * <p>
     * <ul>
     * <li>Status A1 No vehicle available, no communication with vehicle</li>
     * <li>Status A2 Vehicle unlocks plug connection, charging cable can be removed, no vehicle available</li>
     * <li>Status B1 Plug inserted, cable detected, plug connection locked, no communication with vehicle</li>
     * <li>Status B2 Vehicle connected, vehicle not ready for charging, plug connections locked/ interrupted by vehicle, vehicle still
     *  connected, plug connection locked</li>
     * <li>Status C2 Vehicle ready for charging, charging process starts</li>
     * <li>Status E Electric vehicle not available (fault, short circuit between PE and CP), PWM voltage= 0V (not shown)</li>
     * <li>Status F Error state (set by the charging station to signal an error condition), PWM voltage = -12V</li>
     * </ul>
     */
    static enum Mode3State {
        A1, A2, B1, B2, C2, E, F;
        
        private Mode3State() {
            cache(this);
        }
        
        static Mode3State ofName(String name) {
            return byName.get(name);
        }
        
        private void cache(Mode3State mode3State) {
        	if (byName == null) {
        		byName = new HashMap<>();
        	}
            byName.put(mode3State.name(), mode3State);
        }
        
        private static Map<String, Mode3State> byName;
    }
    
    private final Mode3State socket1Mode3State;

    static WallboxReading parse(String line) {
        final String[] fields = line.split("[ \t]+");
        return new WallboxReading(Instant.ofEpochMilli(Long.valueOf(fields[0]) / 1000000l), // convert from nanos to millis
                Double.valueOf(fields[1]), Double.valueOf(fields[2]), Double.valueOf(fields[3]),
                Double.valueOf(fields[4]), Double.valueOf(fields[5]), Double.valueOf(fields[6]),
                CableState.ofCode(Integer.valueOf(fields[7].trim())), Mode3State.ofName(fields[8].trim()));
    }

    public WallboxReading(Instant time, double currentPhase1InAmperes, double currentPhase2InAmperes,
            double currentPhase3InAmperes, double maxCurrentPhase1InAmperes, double maxCurrentPhase2InAmperes,
            double maxCurrentPhase3InAmperes, CableState socket1CableState, Mode3State socket1Mode3State) {
        super();
        this.time = time;
        this.currentPhase1InAmperes = currentPhase1InAmperes;
        this.currentPhase2InAmperes = currentPhase2InAmperes;
        this.currentPhase3InAmperes = currentPhase3InAmperes;
        this.maxCurrentPhase1InAmperes = maxCurrentPhase1InAmperes;
        this.maxCurrentPhase2InAmperes = maxCurrentPhase2InAmperes;
        this.maxCurrentPhase3InAmperes = maxCurrentPhase3InAmperes;
        this.socket1CableState = socket1CableState;
        this.socket1Mode3State = socket1Mode3State;
    }

    public Instant getTime() {
        return time;
    }

    public double getCurrentPhase1InAmperes() {
        return currentPhase1InAmperes;
    }

    public double getCurrentPhase2InAmperes() {
        return currentPhase2InAmperes;
    }

    public double getCurrentPhase3InAmperes() {
        return currentPhase3InAmperes;
    }

    public double getMaxCurrentPhase1InAmperes() {
		return maxCurrentPhase1InAmperes;
	}

	public double getMaxCurrentPhase2InAmperes() {
		return maxCurrentPhase2InAmperes;
	}

	public double getMaxCurrentPhase3InAmperes() {
		return maxCurrentPhase3InAmperes;
	}

	public CableState getSocket1CableState() {
		return socket1CableState;
	}
	
	public Mode3State getSocket1Mode3State() {
        return socket1Mode3State;
    }

    @Override
    public String toString() {
        return "WallboxReading [time=" + time + ", currentPhase1InAmperes=" + currentPhase1InAmperes
                + ", currentPhase2InAmperes=" + currentPhase2InAmperes + ", currentPhase3InAmperes="
                + currentPhase3InAmperes + ", socket1CableState=" + socket1CableState + ", socket1Mode3State="
                + socket1Mode3State + "]";
    }
}