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

package dev.terminalmc.commandkeys.gui.widget.list;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import dev.terminalmc.commandkeys.config.Macro;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.gui.screen.OptionScreen;
import dev.terminalmc.commandkeys.gui.widget.field.FakeTextField;
import dev.terminalmc.commandkeys.gui.widget.field.TextField;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * Displays the list of {@link Profile} instances with various widgets for management, as well as
 * global control widgets.
 */
@SuppressWarnings("UnnecessaryUnicodeEscape")
public class MainOptionList extends OptionList {

    private @Nullable Profile editingProfile;

    public MainOptionList(
            Minecraft mc,
            int width,
            int height,
            int y,
            int entryWidth,
            int entryHeight,
            int entrySpace,
            @Nullable Profile editingProfile
    ) {
        super(mc, width, height, y, entryWidth, entryHeight, entrySpace);
        this.editingProfile = editingProfile;
    }

    @Override
    protected void addEntries() {
        boolean inGame = CommandKeys.inGame();

        // List title
        addEntry(new OptionList.Entry.Text(
                dynEntryX,
                dynEntryWidth,
                entryHeight,
                inGame
                        ? localized("option", "main.profiles.activate")
                        : localized("option", "main.profiles", "\u2139"),
                inGame ? null : Tooltip.create(localized("option", "main.profiles.tooltip")),
                500
        ));

        // Profile list
        int i = 0;
        for (Profile profile : Config.get().getProfiles()) {
            addEntry(new Entry.ProfileOptions(
                    dynEntryX,
                    dynEntryWidth,
                    entryHeight,
                    this,
                    profile,
                    i,
                    inGame
            ));
            if (profile.equals(editingProfile)) {
                // Show name field and list of links
                addEntry(new Entry.ProfileName(dynEntryX, dynEntryWidth, entryHeight, profile));
                for (String address : profile.getLinks()) {
                    addEntry(new Entry.ProfileLink(
                            dynEntryX,
                            dynEntryWidth,
                            entryHeight,
                            this,
                            profile,
                            address
                    ));
                }
            }
            if (i++ == 0 && inGame) {
                // Separate active profile and other profiles
                addEntry(new OptionList.Entry.Text(
                        dynEntryX,
                        dynEntryWidth,
                        entryHeight,
                        localized("option", "main.profiles.other", "\u2139"),
                        Tooltip.create(localized("option", "main.profiles.tooltip")),
                        500
                ));
            }
        }
        addEntry(new OptionList.Entry.ActionButton(
                dynEntryX,
                dynEntryWidth,
                entryHeight,
                Component.literal("+"),
                null,
                -1,
                (button) -> {
                    editingProfile = Config.get().addNewProfile();
                    init();
                }
        ));

        // Default options
        addEntry(new OptionList.Entry.Text(
                dynEntryX,
                dynEntryWidth,
                entryHeight,
                localized("option", "main.default", "\u2139"),
                Tooltip.create(localized("option", "main.default.tooltip")),
                500
        ));
        addEntry(new Entry.DefaultOptions1(dynEntryX, dynEntryWidth, entryHeight));
        addEntry(new Entry.DefaultOptions2(dynEntryX, dynEntryWidth, entryHeight));

        // Ratelimit options
        addEntry(new OptionList.Entry.Text(
                dynEntryX,
                dynEntryWidth,
                entryHeight,
                localized("option", "main.ratelimit", "\u2139"),
                Tooltip.create(localized("option", "main.ratelimit.tooltip")),
                500
        ));
        addEntry(new Entry.Ratelimit(dynEntryX, dynEntryWidth, entryHeight));

        // Length limit options
        addEntry(new OptionList.Entry.Text(
                dynEntryX,
                dynEntryWidth,
                entryHeight,
                localized("option", "main.lengthlimit", "\u2139"),
                Tooltip.create(localized("option", "main.lengthlimit.tooltip")),
                500
        ));
        addEntry(new Entry.LengthLimit(dynEntryX, dynEntryWidth, entryHeight));
    }

    // Sub-screen opening

    public void openProfileOptions(Profile profile) {
        mc.setScreen(new OptionScreen(
                screen,
                localized("option", "profile", profile.getDisplayName()),
                new ProfileOptionList(
                        mc,
                        width,
                        height,
                        getY(),
                        entryWidth,
                        entryHeight,
                        entrySpacing,
                        profile
                )
        ));
    }

    // Input handling

    @Override
    public boolean keyPressed(InputConstants.Key key) {
        return false;
    }

    @Override
    public boolean keyReleased(InputConstants.Key key) {
        return false;
    }

    @Override
    public boolean mouseClicked(InputConstants.Key key) {
        return false;
    }

    @Override
    public boolean mouseReleased(InputConstants.Key key) {
        return false;
    }

    // Custom entries

    private abstract static class Entry extends OptionList.Entry {

        private static class ProfileOptions extends Entry {

            MainOptionList list;
            Profile profile;

            ProfileOptions(
                    int x,
                    int width,
                    int height,
                    MainOptionList list,
                    Profile profile,
                    int index,
                    boolean inGame
            ) {
                super();
                this.list = list;
                this.profile = profile;

                boolean spDefault = index == Config.get().getSpDefault();
                boolean mpDefault = index == Config.get().getMpDefault();

                int smallWidgetWidth = list.smallWidgetWidth;
                int mainButtonWidth = width - smallWidgetWidth * 5 - SPACE * 5;
                int mainButtonX = x;

                if (inGame) {
                    if (index == 0) {
                        // Link button
                        ImageButton linkButton = new ImageButton(
                                x, 0, smallWidgetWidth, height, LINK_SPRITES, (button) -> {
                            profile.forceAddLink(CommandKeys.lastConnection);
                            list.init();
                        }
                        );
                        if (profile.getLinks().contains(CommandKeys.lastConnection)) {
                            linkButton.setTooltip(Tooltip.create(localized(
                                    "option",
                                    "main.profiles.linked.tooltip"
                            )));
                            linkButton.active = false;
                        } else {
                            linkButton.setTooltip(Tooltip.create(localized(
                                    "option",
                                    "main.profiles.link.tooltip"
                            )));
                        }
                        linkButton.setTooltipDelay(Duration.ofMillis(500));
                        elements.add(linkButton);
                    } else {
                        // Activate button
                        Button activateButton = Button.builder(
                                Component.literal("\u2191"), (button) -> {
                                    Config.get().activateProfile(index);
                                    list.init();
                                }
                        ).pos(x, 0).size(smallWidgetWidth, height).build();
                        activateButton.setTooltip(Tooltip.create(localized(
                                "option",
                                "main.profiles.activate.tooltip"
                        )));
                        activateButton.setTooltipDelay(Duration.ofMillis(500));
                        elements.add(activateButton);
                    }
                    mainButtonWidth -= (smallWidgetWidth + SPACE);
                    mainButtonX += (smallWidgetWidth + SPACE);
                }

                MutableComponent name = Component.literal(profile.getDisplayName());
                int numLinks = profile.getLinks().size();
                if (numLinks != 0) {
                    name.append(" ");
                    if (numLinks == 1) {
                        name.append(localized("option", "main.profiles.links.one").withStyle(
                                ChatFormatting.GRAY));
                    } else {
                        name.append(localized(
                                "option",
                                "main.profiles.links.many",
                                numLinks
                        ).withStyle(ChatFormatting.GRAY));
                    }
                }

                // Edit profile button
                elements.add(Button.builder(name, (button) -> list.openProfileOptions(profile))
                        .tooltip(Tooltip.create(localized("option", "main.profile.edit.tooltip")))
                        .pos(mainButtonX, 0)
                        .size(mainButtonWidth, height)
                        .build());

                // Switch to right-justified
                int movingX = x + width - smallWidgetWidth * 5 - SPACE * 4;

                // Edit details button
                ImageButton configureButton = new ImageButton(
                        movingX, 0, smallWidgetWidth, height, OPTION_SPRITES, (button) -> {
                    if (list.editingProfile == null) {
                        list.editingProfile = profile;
                    } else if (!list.editingProfile.equals(profile)) {
                        list.editingProfile = profile;
                    } else {
                        list.editingProfile = null;
                    }
                    list.init();
                }, Component.empty()
                );
                configureButton.setTooltip(Tooltip.create(localized(
                        "option",
                        "main.profile.details.tooltip"
                )));
                configureButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(configureButton);
                movingX += smallWidgetWidth + SPACE;

                // Singleplayer default button
                Button setAsSpDefaultButton = Button.builder(
                        localized("option", "main.profiles.default.singleplayer.set"), (button) -> {
                            Config.get().setSpDefault(index);
                            list.init();
                        }
                ).pos(movingX, 0).size(smallWidgetWidth, height).build();
                if (spDefault) {
                    setAsSpDefaultButton.setTooltip(Tooltip.create(localized(
                            "option",
                            "main.profiles.default.singleplayer.tooltip"
                    )));
                    setAsSpDefaultButton.setMessage(setAsSpDefaultButton.getMessage()
                            .copy()
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    setAsSpDefaultButton.setTooltip(Tooltip.create(localized(
                            "option",
                            "main.profiles.default.singleplayer.set.tooltip"
                    )));
                }
                setAsSpDefaultButton.setTooltipDelay(Duration.ofMillis(500));
                setAsSpDefaultButton.active = !spDefault;
                elements.add(setAsSpDefaultButton);
                movingX += smallWidgetWidth + SPACE;

                // Multiplayer default button
                Button setAsMpDefaultButton = Button.builder(
                        localized("option", "main.profiles.default.multiplayer.set"), (button) -> {
                            Config.get().setMpDefault(index);
                            list.init();
                        }
                ).pos(movingX, 0).size(smallWidgetWidth, height).build();
                if (mpDefault) {
                    setAsMpDefaultButton.setTooltip(Tooltip.create(localized(
                            "option",
                            "main.profiles.default.multiplayer.tooltip"
                    )));
                    setAsMpDefaultButton.setMessage(setAsMpDefaultButton.getMessage()
                            .copy()
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    setAsMpDefaultButton.setTooltip(Tooltip.create(localized(
                            "option",
                            "main.profiles.default.multiplayer.set.tooltip"
                    )));
                }
                setAsMpDefaultButton.setTooltipDelay(Duration.ofMillis(500));
                setAsMpDefaultButton.active = !mpDefault;
                elements.add(setAsMpDefaultButton);
                movingX += smallWidgetWidth + SPACE;

                // Copy button
                ImageButton copyButton = new ImageButton(
                        movingX, 0, smallWidgetWidth, height, COPY_SPRITES, (button) -> {
                    Config.get().addCopyProfile(profile);
                    list.init();
                }, Component.empty()
                );
                copyButton.setTooltip(Tooltip.create(localized(
                        "option",
                        "main.profiles.copy.tooltip"
                )));
                copyButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(copyButton);
                movingX += smallWidgetWidth + SPACE;

                // Delete button
                Button deleteButton = Button.builder(
                        Component.literal("\u274C"), (button) -> {
                            Config.get().removeProfile(index);
                            list.init();
                        }
                ).pos(movingX, 0).size(smallWidgetWidth, height).build();
                if (spDefault || mpDefault) {
                    deleteButton.active = false;
                    deleteButton.setTooltip(Tooltip.create(localized(
                            "option",
                            "main.profiles.delete.disabled.tooltip"
                    )));
                } else {
                    deleteButton.setMessage(deleteButton.getMessage()
                            .copy()
                            .withStyle(ChatFormatting.RED));
                    deleteButton.setTooltip(Tooltip.create(localized(
                            "option",
                            "main.profiles.delete.tooltip"
                    )));
                }
                deleteButton.setTooltipDelay(Duration.ofMillis(500));

                elements.add(deleteButton);
            }
        }

        private static class ProfileName extends Entry {

            ProfileName(int x, int width, int height, Profile profile) {
                super();
                int labelWidth = 50;
                int nameBoxWidth = width - labelWidth - SPACE;

                Button label = Button.builder(
                        localized("option", "main.profile.name"), (button -> {
                        })
                ).pos(x, 0).size(labelWidth, height).build();
                label.active = false;
                elements.add(label);

                TextField nameBox = new TextField(x + labelWidth, 0, nameBoxWidth, height);
                nameBox.setMaxLength(64);
                nameBox.setResponder((value) -> profile.name = value.strip());
                nameBox.setValue(profile.name);
                elements.add(nameBox);
            }
        }

        private static class ProfileLink extends Entry {

            ProfileLink(
                    int x,
                    int width,
                    int height,
                    MainOptionList list,
                    Profile profile,
                    String address
            ) {
                super();
                int labelWidth = 50;
                int linkFieldWidth = width - labelWidth - list.smallWidgetWidth - SPACE;

                Button label = Button.builder(
                        localized("option", "main.profiles.link"), (button -> {
                        })
                ).pos(x, 0).size(labelWidth, height).build();
                label.active = false;
                elements.add(label);

                TextField linkField = new FakeTextField(
                        x + labelWidth, 0, linkFieldWidth, height, () -> {
                }
                );
                linkField.setMaxLength(64);
                linkField.setValue(address);
                linkField.active = false;
                elements.add(linkField);

                Button removeButton = Button.builder(
                                Component.literal("\u274C"), (button) -> {
                                    profile.removeLink(address);
                                    list.init();
                                }
                        )
                        .pos(x + width - list.smallWidgetWidth, 0)
                        .size(list.smallWidgetWidth, height)
                        .build();
                removeButton.setTooltip(Tooltip.create(localized(
                        "option",
                        "main.profile.link.remove.tooltip"
                )));
                removeButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(removeButton);
            }
        }

        private static class DefaultOptions1 extends Entry {

            DefaultOptions1(int x, int width, int height) {
                super();
                int buttonWidth = (width - SPACE) / 2;

                // Conflict strategy button
                elements.add(CycleButton.builder(Macro.ConflictStrategy::title)
                        .withValues(Macro.ConflictStrategy.values())
                        .withInitialValue(Config.get().defaultConflictStrategy)
                        .withTooltip((status) -> Tooltip.create(status.tooltip()))
                        .create(
                                x,
                                0,
                                buttonWidth,
                                height,
                                localized("option", "macro.conflict"),
                                (button, status) -> Config.get().defaultConflictStrategy = status
                        ));

                // Send mode button
                elements.add(CycleButton.builder(Macro.SendMode::title)
                        .withValues(Macro.SendMode.values())
                        .withInitialValue(Config.get().defaultSendMode)
                        .withTooltip((status) -> Tooltip.create(status.tooltip()))
                        .create(
                                x + width - buttonWidth,
                                0,
                                buttonWidth,
                                height,
                                localized("option", "macro.mode"),
                                (button, status) -> Config.get().defaultSendMode = status
                        ));
            }
        }

        private static class DefaultOptions2 extends Entry {

            DefaultOptions2(int x, int width, int height) {
                super();
                int buttonWidth = (width - SPACE) / 2;

                // Activation type button
                elements.add(CycleButton.builder(Macro.ActivationType::title)
                        .withValues(Macro.ActivationType.values())
                        .withInitialValue(Config.get().defaultActivationType)
                        .withTooltip((status) -> Tooltip.create(status.tooltip()))
                        .create(
                                x,
                                0,
                                buttonWidth,
                                height,
                                localized("option", "macro.activation"),
                                (button, status) -> Config.get().defaultActivationType = status
                        ));
            }
        }

        private static class Ratelimit extends Entry {

            Ratelimit(int x, int width, int height) {
                super();
                int buttonWidth = (width - SPACE * 2) / 3;
                int fieldWidth = (buttonWidth - SPACE) / 2;
                int movingX = x;

                // Message count field
                TextField countField = new TextField(movingX, 0, fieldWidth, height);
                countField.posIntValidator().strict();
                countField.setMaxLength(6);
                countField.setResponder((val) -> Config.get()
                        .setRatelimitCount(Integer.parseInt(val.strip())));
                countField.setValue(String.valueOf(Config.get().getRatelimitCount()));
                countField.setTooltip(Tooltip.create(localized(
                        "option",
                        "main.ratelimit.count.tooltip"
                )));
                elements.add(countField);
                movingX += fieldWidth + SPACE;

                // Time window field
                TextField ticksField = new TextField(movingX, 0, fieldWidth, height);
                ticksField.posIntValidator().strict();
                ticksField.setMaxLength(6);
                ticksField.setResponder((val) -> {
                    try {
                        int space = Integer.parseInt(val.strip());
                        if (space < 1)
                            throw new NumberFormatException();
                        Config.get().setRatelimitTicks(space);
                        ticksField.setTextColor(16777215);
                    } catch (NumberFormatException ignored) {
                        ticksField.setTextColor(16711680);
                    }
                });
                ticksField.setValue(String.valueOf(Config.get().getRatelimitTicks()));
                ticksField.setTooltip(Tooltip.create(localized(
                        "option",
                        "main.ratelimit.ticks.tooltip"
                )));
                elements.add(ticksField);
                movingX = x + width - buttonWidth * 2 - SPACE;

                CycleButton<Boolean> strictButton = CycleButton.booleanBuilder(
                                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED)
                        )
                        .withInitialValue(Config.get().ratelimitStrict)
                        .withTooltip((status) -> Tooltip.create(localized(
                                "option",
                                "main.ratelimit.strict.tooltip"
                        )))
                        .create(
                                movingX,
                                0,
                                buttonWidth,
                                height,
                                localized("option", "main.ratelimit.strict"),
                                (button, status) -> Config.get().ratelimitStrict = status
                        );
                strictButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(strictButton);
                movingX = x + width - buttonWidth;

                CycleButton<Boolean> spButton = CycleButton.booleanBuilder(
                                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED)
                        )
                        .withInitialValue(Config.get().ratelimitSp)
                        .withTooltip((status) -> Tooltip.create(localized(
                                "option",
                                "main.ratelimit.sp.tooltip"
                        )))
                        .create(
                                movingX,
                                0,
                                buttonWidth,
                                height,
                                localized("option", "main.ratelimit.sp"),
                                (button, status) -> Config.get().ratelimitSp = status
                        );
                spButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(spButton);
            }
        }

        private static class LengthLimit extends Entry {

            LengthLimit(int x, int width, int height) {
                super();

                // Max length field
                TextField lengthField = new TextField(x, 0, width, height);
                lengthField.posIntValidator().strict();
                lengthField.setMaxLength(6);
                lengthField.setResponder((val) -> {
                    try {
                        int space = Integer.parseInt(val.strip());
                        if (space < 1)
                            throw new NumberFormatException();
                        Config.get().setLengthLimitLength(space);
                        lengthField.setTextColor(16777215);
                    } catch (NumberFormatException ignored) {
                        lengthField.setTextColor(16711680);
                    }
                });
                lengthField.setValue(String.valueOf(Config.get().getLengthLimitLength()));
                lengthField.setTooltip(Tooltip.create(localized(
                        "option",
                        "main.lengthlimit.length.tooltip"
                ).append("\n")
                        .append(localized(
                                "option",
                                "main.lengthlimit.length.tooltip.warning"
                        ).withStyle(ChatFormatting.RED))));
                elements.add(lengthField);
            }
        }
    }
}
