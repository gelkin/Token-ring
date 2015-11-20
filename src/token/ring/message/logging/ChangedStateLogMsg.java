package token.ring.message.logging;

import token.ring.Visualizer;

public class ChangedStateLogMsg extends LogMessage {
    public final Visualizer.State newState;

    public ChangedStateLogMsg(Visualizer.State newState) {
        this.newState = newState;
    }

}
