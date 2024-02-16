package com.notryken.commandkeys.config;

/**
 * Represents a control with three states, arbitrarily ZERO, ONE, TWO.
 */
public class TriState {

    public enum State {
        ZERO,
        ONE,
        TWO
    }

    public State state;

    public TriState() {
        this.state = State.ZERO;
    }

    public TriState(State state) {
        this.state = state;
    }
}
