package computation;

import org.apache.commons.math3.fraction.BigFraction;

import java.util.Iterator;

public class NicolantPiComputator implements Iterator<BigFraction> {
    private BigFraction pi = new BigFraction(3);
    int iteration = 0;

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public BigFraction next() {
        BigFraction additive = new BigFraction(iteration % 2 == 0 ? 4 : -4);
        for (int i = 0; i < 3; i++) {
            additive = additive.divide((iteration + 1) * 2 + i);
        }
        iteration++;
        return pi = pi.add(additive);
    }
}
