package token.ring.message;

import computation.HexPiComputation;
import sender.main.RequestMessage;
import token.ring.NetworkMap;

public class AcceptToken extends RequestMessage<AcceptTokenResponse> {
    public final HexPiComputation computation;
    public final NetworkMap netmap;

    public AcceptToken(HexPiComputation computation, NetworkMap netmap) {
        this.computation = computation;
        this.netmap = netmap;
    }
}
