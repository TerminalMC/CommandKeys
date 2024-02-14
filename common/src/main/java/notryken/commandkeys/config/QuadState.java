package notryken.commandkeys.config;

/**
 * Represents a control with four states, arbitrarily ZERO, ONE, TWO, THREE.
 */
public class QuadState {

    public enum State {
        ZERO,
        ONE,
        TWO,
        THREE
    }

    public State state;

    public QuadState() {
        this.state = State.ZERO;
    }

    public QuadState(State state) {
        this.state = state;
    }
}
