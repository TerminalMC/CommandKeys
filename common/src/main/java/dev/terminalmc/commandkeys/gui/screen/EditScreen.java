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

package dev.terminalmc.commandkeys.gui.screen;

import dev.terminalmc.commandkeys.gui.widget.field.MultiLineTextField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class EditScreen extends Screen {

    private final Consumer<String> callback;

    public EditScreen(Consumer<String> callback) {
        super(Component.empty());
        this.callback = callback;
    }

    @Override
    public void init() {
        MultiLineTextField field = new MultiLineTextField(
                width / 2 - 120,
                height / 2 - 10,
                240,
                20,
                Component.empty()
        );
        field.setValueListener((str) -> {
            if (str.contains("\n")) {
                onClose();
                callback.accept(str.replaceAll("\\n", ""));
            }
        });
        addRenderableWidget(field);
        setFocused(true);
        setFocused(field);
    }
}
