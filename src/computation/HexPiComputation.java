package computation;

import org.apache.commons.math3.fraction.BigFraction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Iterator;

public class HexPiComputation implements Iterator<BigFraction>, Serializable {
    private int iteration = 0;

    private BigFraction currentValue = new BigFraction(0);
    private final int precisionStep;
    private BigFraction powerOf16 = new BigFraction(16);

    public HexPiComputation(int precisionStep) {
        this.precisionStep = precisionStep;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public BigFraction next() {
        for (int i = 0; i < precisionStep; i++) {
            BigFraction additive = countAdditive(iteration);
            currentValue = currentValue.add(additive);
            iteration++;
        }
        return currentValue;
    }

    private BigFraction countAdditive(int i) {
        powerOf16 = powerOf16.divide(16);
        return powerOf16.multiply(
                new BigFraction(4).divide(new BigFraction(8 * i + 1))
                        .subtract(new BigFraction(2).divide(new BigFraction(8 * i + 4)))
                        .subtract(new BigFraction(1).divide(new BigFraction(8 * i + 5)))
                        .subtract(new BigFraction(1).divide(new BigFraction(8 * i + 6)))
        );
    }

    public BigFraction getCurrentValue() {
        return currentValue;
    }

    public int getCurrentPrecision() {
        return iteration;
    }

    public int getPrecisionStep() {
        return precisionStep;
    }

    public BigDecimal bigDecimalValue() {
        return currentValue.bigDecimalValue(iteration + 5, BigDecimal.ROUND_HALF_EVEN);
    }
}
