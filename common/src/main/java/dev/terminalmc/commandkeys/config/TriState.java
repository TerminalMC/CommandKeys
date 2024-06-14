/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.config;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a control with three states, arbitrarily ZERO, ONE, TWO.
 */
public class TriState {

    public enum State {
        ZERO,
        ONE,
        TWO
    }

    public @NotNull State state;

    public TriState() {
        this.state = State.ZERO;
    }

    public TriState(@NotNull State state) {
        this.state = state;
    }

    public TriState(String state) {
        this.state = State.valueOf(state);
    }
}
