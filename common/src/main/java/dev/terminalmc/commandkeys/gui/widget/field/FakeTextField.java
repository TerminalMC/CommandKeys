/*
 * Copyright 2026 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.commandkeys.gui.widget.field;

import net.minecraft.client.input.MouseButtonEvent;

/**
 * A {@link TextField} which renders like a normal editable field, but when clicked, runs a custom
 * {@link Runnable} instead of becoming selected.
 */
public class FakeTextField extends TextField {

    private final Runnable onClick;

    public FakeTextField(int x, int y, int width, int height, Runnable onClick) {
        super(x, y, width, height);
        this.onClick = onClick;
        this.active = false;
        this.setResponder((str) -> {
        });
        this.setTextColor(TEXT_COLOR_PREVIEW);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Skip the 'active' requirement
        return (visible
                && mouseX >= (double) getX()
                && mouseY >= (double) getY()
                && mouseX < (double) getRight()
                && mouseY < (double) getBottom());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isMouseOver(event.x(), event.y())) {
            onClick(event, doubleClick);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onClick.run();
    }

    @Override
    public boolean isFocused() {
        return false;
    }
}
