package token.ring.message;

import org.apache.commons.math3.fraction.BigFraction;
import sender.main.RequestMessage;
import token.ring.NetworkMap;

public class AcceptToken extends RequestMessage<AcceptTokenResponse> {
    public final BigFraction piValue;
    public final int progress;
    public final NetworkMap netmap;

    public AcceptToken(BigFraction piValue, int progress, NetworkMap netmap) {
        this.piValue = piValue;
        this.progress = progress;
        this.netmap = netmap;
    }

}
