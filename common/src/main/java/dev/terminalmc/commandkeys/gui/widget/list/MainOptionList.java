/*
 * Copyright 2024 TerminalMC
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
import dev.terminalmc.commandkeys.gui.screen.OptionsScreen;
import dev.terminalmc.commandkeys.util.KeybindUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * Displays the list of {@link Profile} instances, with various widgets for
 * management.
 */
public class MainOptionList extends OptionList {
    private @Nullable Profile editingProfile;

    public MainOptionList(Minecraft mc, int width, int height, int y,
                          int itemHeight, int entryWidth, int entryHeight,
                          @Nullable Profile editingProfile) {
        super(mc, width, height, y, itemHeight, entryWidth, entryHeight);
        this.editingProfile = editingProfile;

        boolean inGame = CommandKeys.inGame();

        addEntry(new OptionList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                inGame ? localized("option", "main.activateProfile")
                        : localized("option", "main.profiles", "\u2139"),
                inGame ? null : Tooltip.create(localized("option", "main.profiles.tooltip")), 500));

        Config config = Config.get();
        int i = 0;
        for (Profile profile : config.getProfiles()) {
            addEntry(new Entry.ProfileEntry(entryX, entryWidth, entryHeight, this,
                    profile, i, i == config.getSpDefault(), i == config.getMpDefault(), inGame));
            if (profile.equals(editingProfile)) {
                addEntry(new Entry.ProfileNameEntry(entryX, entryWidth, entryHeight, profile));
                for (String address : profile.getLinks()) {
                    addEntry(new Entry.ServerAddressEntry(entryX, entryWidth, entryHeight, this,
                            profile, address));
                }
            }
            if (i == 0 && inGame) {
                addEntry(new OptionList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                        localized("option", "main.otherProfiles", "\u2139"),
                        Tooltip.create(localized("option", "main.profiles.tooltip")), 500));
            }
            i++;
        }

        addEntry(new OptionList.Entry.ActionButtonEntry(entryX, entryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> {
                    Profile newProfile = new Profile();
                    Config.get().addProfile(newProfile);
                    setEditingProfile(newProfile);
                    reload();
                }));

        addEntry(new OptionList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                localized("option", "main.default", "\u2139"),
                Tooltip.create(localized("option", "main.default.tooltip")), 500));
        addEntry(new Entry.DefaultOptionsEntry(entryX, entryWidth, entryHeight));

        addEntry(new OptionList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                localized("option", "main.ratelimit", "\u2139"),
                Tooltip.create(localized("option", "main.ratelimit.tooltip")), 500));
        addEntry(new Entry.RatelimitEntry(entryX, entryWidth, entryHeight));
    }

    private void setEditingProfile(@Nullable Profile profile) {
        editingProfile = profile;
    }

    @Override
    public MainOptionList reload(int width, int height, double scrollAmount) {
        MainOptionList newListWidget = new MainOptionList(minecraft, width, height,
                getY(), itemHeight, entryWidth, entryHeight, editingProfile);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

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

    public void openProfileOptionsScreen(Profile profile) {
        minecraft.setScreen(new OptionsScreen(screen, localized("option", "profile", profile.getDisplayName()),
                new ProfileOptionList(minecraft, screen.width, screen.height, getY(),
                        itemHeight, entryWidth, entryHeight, profile)));
    }

    private abstract static class Entry extends OptionList.Entry {

        private static class ProfileEntry extends Entry {
            MainOptionList list;
            Profile profile;

            ProfileEntry(int x, int width, int height, MainOptionList list, Profile profile,
                         int index, boolean spDefault, boolean mpDefault, boolean inGame) {
                super();
                this.list = list;
                this.profile = profile;

                int smallButtonWidth = list.smallButtonWidth;
                int mainButtonWidth = width - smallButtonWidth * 5 - SPACING * 5;
                int mainButtonX = x;

                if (inGame) {
                    if (index == 0) {
                        // Link button
                        ImageButton linkButton = new ImageButton(
                                x, 0, smallButtonWidth, height, LINK_SPRITES,
                                (button) -> {
                                    profile.forceAddLink(CommandKeys.lastConnection);
                                    list.reload();
                                });
                        if (profile.getLinks().contains(CommandKeys.lastConnection)) {
                            linkButton.setTooltip(Tooltip.create(
                                    localized("option", "main.linked.tooltip")));
                            linkButton.active = false;
                        } else {
                            linkButton.setTooltip(Tooltip.create(
                                    localized("option", "main.link.tooltip")));
                        }
                        linkButton.setTooltipDelay(Duration.ofMillis(500));
                        elements.add(linkButton);
                    }
                    else {
                        // Activate button
                        Button activateButton = Button.builder(Component.literal("\u2191"),
                                        (button) -> {
                                            Config.get().activateProfile(index);
                                            list.reload();
                                        })
                                .pos(x, 0)
                                .size(smallButtonWidth, smallButtonWidth)
                                .build();
                        activateButton.setTooltip(Tooltip.create(
                                localized("option", "main.activate.tooltip")));
                        activateButton.setTooltipDelay(Duration.ofMillis(500));
                        elements.add(activateButton);
                    }
                    mainButtonWidth -= (smallButtonWidth + SPACING);
                    mainButtonX += (smallButtonWidth + SPACING);
                }

                MutableComponent name = Component.literal(profile.getDisplayName());
                int numLinks = profile.getLinks().size();
                if (numLinks != 0) {
                    name.append(" ");
                    if (numLinks == 1) {
                        name.append(localized("option","main.links.one")
                                .withStyle(ChatFormatting.GRAY));
                    } else {
                        name.append(localized("option","main.links.many", numLinks)
                                .withStyle(ChatFormatting.GRAY));
                    }
                }

                // Edit profile button
                elements.add(Button.builder(name, (button) ->
                                list.openProfileOptionsScreen(profile))
                        .tooltip(Tooltip.create(
                                localized("option", "main.editProfile.tooltip")))
                        .pos(mainButtonX, 0)
                        .size(mainButtonWidth, height)
                        .build());

                // Switch to right-justified
                int movingX = x + width - smallButtonWidth * 5 - SPACING * 4;

                // Edit details button
                ImageButton configureButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        GEAR_SPRITES,
                        (button) -> {
                            if (list.editingProfile == null) {
                                list.editingProfile = profile;
                            }
                            else if (!list.editingProfile.equals(profile)) {
                                list.editingProfile = profile;
                            }
                            else {
                                list.editingProfile = null;
                            }
                            list.reload();
                        },
                        Component.empty());
                configureButton.setTooltip(Tooltip.create(
                        localized("option", "main.editDetails.tooltip")));
                configureButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(configureButton);
                movingX += smallButtonWidth + SPACING;

                // Singleplayer default button
                Button setAsSpDefaultButton = Button.builder(
                        localized("option", "main.defaultSingleplayer.set"),
                        (button) -> {
                            Config.get().setSpDefault(index);
                            list.reload();
                        })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                if (spDefault) {
                    setAsSpDefaultButton.setTooltip(Tooltip.create(
                            localized("option", "main.defaultSingleplayer.tooltip")));
                    setAsSpDefaultButton.setMessage(setAsSpDefaultButton.getMessage().copy()
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    setAsSpDefaultButton.setTooltip(Tooltip.create(
                            localized("option", "main.defaultSingleplayer.set.tooltip")));
                }
                setAsSpDefaultButton.setTooltipDelay(Duration.ofMillis(500));
                setAsSpDefaultButton.active = !spDefault;
                elements.add(setAsSpDefaultButton);
                movingX += smallButtonWidth + SPACING;

                // Multiplayer default button
                Button setAsMpDefaultButton = Button.builder(
                        localized("option", "main.defaultMultiplayer.set"),
                                (button) -> {
                                    Config.get().setMpDefault(index);
                                    list.reload();
                                })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                if (mpDefault) {
                    setAsMpDefaultButton.setTooltip(Tooltip.create(
                            localized("option", "main.defaultMultiplayer.tooltip")));
                    setAsMpDefaultButton.setMessage(setAsMpDefaultButton.getMessage().copy()
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    setAsMpDefaultButton.setTooltip(Tooltip.create(
                            localized("option", "main.defaultMultiplayer.set.tooltip")
                    ));
                }
                setAsMpDefaultButton.setTooltipDelay(Duration.ofMillis(500));
                setAsMpDefaultButton.active = !mpDefault;
                elements.add(setAsMpDefaultButton);
                movingX += smallButtonWidth + SPACING;

                // Copy button
                ImageButton copyButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        COPY_SPRITES,
                        (button) -> {
                            Config.get().copyProfile(profile);
                            list.reload();
                        },
                        Component.empty());
                copyButton.setTooltip(Tooltip.create(
                        localized("option", "main.copy.tooltip")));
                copyButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(copyButton);
                movingX += smallButtonWidth + SPACING;

                // Delete button
                Button deleteButton = Button.builder(Component.literal("\u274C"),
                                (button) -> {
                                    Config.get().removeProfile(index);
                                    list.reload();
                                })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                if (spDefault || mpDefault) {
                    deleteButton.active = false;
                    deleteButton.setTooltip(Tooltip.create(
                            localized("option", "main.delete.disabled.tooltip")
                    ));
                } else {
                    deleteButton.setMessage(deleteButton.getMessage().copy()
                            .withStyle(ChatFormatting.RED));
                    deleteButton.setTooltip(Tooltip.create(
                            localized("option", "main.delete.tooltip")
                    ));
                }
                deleteButton.setTooltipDelay(Duration.ofMillis(500));

                elements.add(deleteButton);
            }
        }

        private static class ProfileNameEntry extends Entry {
            ProfileNameEntry(int x, int width, int height, Profile profile) {
                super();
                int labelWidth = 50;
                int nameBoxWidth = width - labelWidth - SPACING;

                Button label = Button.builder(localized("option", "main.name"), (button -> {}))
                        .pos(x, 0)
                        .size(labelWidth, height)
                        .build();
                label.active = false;
                elements.add(label);

                EditBox nameBox = new EditBox(Minecraft.getInstance().font, x + labelWidth, 0,
                        nameBoxWidth, height, Component.empty());
                nameBox.setMaxLength(64);
                nameBox.setValue(profile.name);
                nameBox.setResponder((value) -> profile.name = value.strip());
                elements.add(nameBox);
            }
        }

        private static class ServerAddressEntry extends Entry {
            ServerAddressEntry(int x, int width, int height, MainOptionList list,
                               Profile profile, String address) {
                super();
                int labelWidth = 50;
                int addressBoxWidth = width - labelWidth - list.smallButtonWidth - SPACING;

                Button label = Button.builder(localized("option", "main.link"), (button -> {}))
                        .pos(x, 0)
                        .size(labelWidth, height)
                        .build();
                label.active = false;
                elements.add(label);

                EditBox addressBox = new EditBox(Minecraft.getInstance().font, x + labelWidth, 0,
                        addressBoxWidth, height, Component.empty());
                addressBox.setMaxLength(64);
                addressBox.setValue(address);
                addressBox.active = false;
                elements.add(addressBox);

                Button removeButton = Button.builder(Component.literal("\u274C"),
                        (button) -> {
                            profile.removeLink(address);
                            list.reload();
                        })
                        .pos(x + width - list.smallButtonWidth, 0)
                        .size(list.smallButtonWidth, height)
                        .build();
                removeButton.setTooltip(Tooltip.create(
                        localized("option", "main.removeLink.tooltip")));
                removeButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(removeButton);
            }
        }

        private static class DefaultOptionsEntry extends Entry {
            DefaultOptionsEntry(int x, int width, int height) {
                super();
                int buttonWidth = (width - SPACING) / 2;

                // Conflict strategy button
                elements.add(CycleButton.builder(KeybindUtil::localizeStrategy)
                        .withValues(Macro.ConflictStrategy.values())
                        .withInitialValue(Config.get().defaultConflictStrategy)
                        .withTooltip((status) -> Tooltip.create(
                                KeybindUtil.localizeStrategyTooltip(status)))
                        .create(x, 0, buttonWidth, height,
                                localized("option", "main.default.conflictStrategy"),
                                (button, status) ->
                                        Config.get().defaultConflictStrategy = status));

                // Send mode button
                elements.add(CycleButton.builder(KeybindUtil::localizeMode)
                        .withValues(Macro.SendMode.values())
                        .withInitialValue(Config.get().defaultSendMode)
                        .withTooltip((status) -> Tooltip.create(
                                KeybindUtil.localizeModeTooltip(status)))
                        .create(x + width - buttonWidth, 0, buttonWidth, height,
                                localized("option", "main.default.sendMode"),
                                (button, status) ->
                                        Config.get().defaultSendMode = status));
            }
        }

        private static class RatelimitEntry extends Entry {
            RatelimitEntry(int x, int width, int height) {
                super();
                int buttonWidth = (width - SPACING * 2) / 3;
                int fieldWidth = (buttonWidth - SPACING) / 2;
                int movingX = x;

                // Message count field
                EditBox countField = new EditBox(Minecraft.getInstance().font,
                        movingX, 0, fieldWidth, height, Component.empty());
                countField.setMaxLength(6);
                countField.setResponder((val) -> {
                    try {
                        int space = Integer.parseInt(val.strip());
                        if (space < 1) throw new NumberFormatException();
                        Config.get().setRatelimitCount(space);
                        countField.setTextColor(16777215);
                    } catch (NumberFormatException ignored) {
                        countField.setTextColor(16711680);
                    }
                });
                countField.setValue(String.valueOf(Config.get().getRatelimitCount()));
                countField.setTooltip(Tooltip.create(
                        localized("option", "main.ratelimit.count.tooltip")));
                elements.add(countField);
                movingX += fieldWidth + SPACING;

                // Time window field
                EditBox ticksField = new EditBox(Minecraft.getInstance().font,
                        movingX, 0, fieldWidth, height, Component.empty());
                ticksField.setMaxLength(6);
                ticksField.setResponder((val) -> {
                    try {
                        int space = Integer.parseInt(val.strip());
                        if (space < 1) throw new NumberFormatException();
                        Config.get().setRatelimitTicks(space);
                        ticksField.setTextColor(16777215);
                    } catch (NumberFormatException ignored) {
                        ticksField.setTextColor(16711680);
                    }
                });
                ticksField.setValue(String.valueOf(Config.get().getRatelimitTicks()));
                ticksField.setTooltip(Tooltip.create(
                        localized("option", "main.ratelimit.ticks.tooltip")));
                elements.add(ticksField);
                movingX = x + width - buttonWidth * 2 - SPACING;

                CycleButton<Boolean> strictButton = CycleButton.booleanBuilder(
                                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .withInitialValue(Config.get().ratelimitStrict)
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "main.ratelimit.strict.tooltip")))
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "main.ratelimit.strict"),
                                (button, status) -> Config.get().ratelimitStrict = status);
                strictButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(strictButton);
                movingX = x + width - buttonWidth;

                CycleButton<Boolean> spButton = CycleButton.booleanBuilder(
                                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .withInitialValue(Config.get().ratelimitSp)
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "main.ratelimit.sp.tooltip")))
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "main.ratelimit.sp"),
                                (button, status) -> Config.get().ratelimitSp = status);
                spButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(spButton);
            }
        }
    }
}
