/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.config;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a control with three states, arbitrarily ZERO, ONE, TWO, THREE.
 */
public class QuadState {

    public enum State {
        ZERO,
        ONE,
        TWO,
        THREE,
    }

    public @NotNull State state;

    public QuadState() {
        this.state = State.ZERO;
    }

    public QuadState(@NotNull State state) {
        this.state = state;
    }

    public QuadState(String state) {
        this.state = State.valueOf(state);
    }
}
