/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
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

    public MainOptionList(Minecraft mc, int width, int height, int top, int bottom,
                          int itemHeight, int entryWidth, int entryHeight,
                          @Nullable Profile editingProfile) {
        super(mc, width, height, top, bottom, itemHeight, entryWidth, entryHeight);
        this.editingProfile = editingProfile;

        boolean inGame = CommandKeys.inGame();

        addEntry(new OptionList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                localized("option", "main.default", "\u2139"),
                Tooltip.create(localized("option", "main.default.tooltip")), 500));

        addEntry(new Entry.DefaultOptionsEntry(entryX, entryWidth, entryHeight));

        addEntry(new OptionList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                inGame ? localized("option", "main.active_profile")
                        : localized("option", "main.profiles", "\u2139"),
                inGame ? null : Tooltip.create(localized("option", "main.profiles.tooltip")), 500));

        Config config = Config.get();
        int i = 0;
        for (Profile profile : config.getProfiles()) {
            addEntry(new Entry.ProfileEntry(entryX, entryWidth, entryHeight, this,
                    profile, i, i == config.spDefault, i == config.mpDefault, inGame));
            if (profile.equals(editingProfile)) {
                addEntry(new Entry.ProfileNameEntry(entryX, entryWidth, entryHeight, profile));
                for (String address : profile.getLinks()) {
                    addEntry(new Entry.ServerAddressEntry(entryX, entryWidth, entryHeight, this,
                            profile, address));
                }
            }
            if (i == 0 && inGame) {
                addEntry(new OptionList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                        localized("option", "main.other_profiles", "\u2139"),
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
    }

    private void setEditingProfile(@Nullable Profile profile) {
        editingProfile = profile;
    }

    @Override
    public MainOptionList reload(int width, int height, int top, int bottom, double scrollAmount) {
        MainOptionList newListWidget = new MainOptionList(minecraft, width, height,
                top, bottom, itemHeight, entryWidth, entryHeight, editingProfile);
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
                new ProfileOptionList(minecraft, screen.width, screen.height, screen.listTop, screen.listBottom.get(),
                        itemHeight, entryWidth, entryHeight, profile)));
    }

    private abstract static class Entry extends OptionList.Entry {

        private static class DefaultOptionsEntry extends Entry {
            DefaultOptionsEntry(int x, int width, int height) {
                super();
                int buttonWidth = (width - SPACING) / 2;

                // Conflict strategy button
                elements.add(CycleButton.builder(KeybindUtil::localizeStrat)
                        .withValues(Macro.ConflictStrategy.values())
                        .withInitialValue(Config.get().defaultConflictStrategy)
                        .withTooltip((status) -> Tooltip.create(
                                KeybindUtil.localizeStratTooltip(status)))
                        .create(x, 0, buttonWidth, height,
                                localized("option", "main.default.conflict_strategy"),
                                (button, status) ->
                                        Config.get().defaultConflictStrategy = status));

                // Send mode button
                elements.add(CycleButton.builder(KeybindUtil::localizeMode)
                        .withValues(Macro.SendMode.values())
                        .withInitialValue(Config.get().defaultSendMode)
                        .withTooltip((status) -> Tooltip.create(
                                KeybindUtil.localizeModeTooltip(status)))
                        .create(x + width - buttonWidth, 0, buttonWidth, height,
                                localized("option", "main.default.send_mode"),
                                (button, status) ->
                                        Config.get().defaultSendMode = status));
            }
        }

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
                        ImageButton linkButton = new ImageButton(x, 0, smallButtonWidth, height,
                                0, 0, 20, OptionList.Entry.LINK_ICON, 32, 64,
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
                        linkButton.setTooltipDelay(500);
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
                        activateButton.setTooltipDelay(500);
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
                                localized("option", "main.edit_profile.tooltip")))
                        .pos(mainButtonX, 0)
                        .size(mainButtonWidth, height)
                        .build());

                // Switch to right-justified
                int movingX = x + width - smallButtonWidth * 5 - SPACING * 4;

                // Edit details button
                ImageButton configureButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        0, 0, 20, OptionList.Entry.GEAR_ICON, 32, 64,
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
                        localized("option", "main.edit_details.tooltip")));
                configureButton.setTooltipDelay(500);
                elements.add(configureButton);
                movingX += smallButtonWidth + SPACING;

                // Singleplayer default button
                Button setAsSpDefaultButton = Button.builder(
                        localized("option", "main.default_singleplayer.set"),
                        (button) -> {
                            Config.get().spDefault = index;
                            list.reload();
                        })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                if (spDefault) {
                    setAsSpDefaultButton.setTooltip(Tooltip.create(
                            localized("option", "main.default_singleplayer.tooltip")));
                    setAsSpDefaultButton.setMessage(setAsSpDefaultButton.getMessage().copy()
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    setAsSpDefaultButton.setTooltip(Tooltip.create(
                            localized("option", "main.default_singleplayer.set.tooltip")));
                }
                setAsSpDefaultButton.setTooltipDelay(500);
                setAsSpDefaultButton.active = !spDefault;
                elements.add(setAsSpDefaultButton);
                movingX += smallButtonWidth + SPACING;

                // Multiplayer default button
                Button setAsMpDefaultButton = Button.builder(
                        localized("option", "main.default_multiplayer.set"),
                                (button) -> {
                                    Config.get().mpDefault = index;
                                    list.reload();
                                })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                if (mpDefault) {
                    setAsMpDefaultButton.setTooltip(Tooltip.create(
                            localized("option", "main.default_multiplayer.tooltip")));
                    setAsMpDefaultButton.setMessage(setAsMpDefaultButton.getMessage().copy()
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    setAsMpDefaultButton.setTooltip(Tooltip.create(
                            localized("option", "main.default_multiplayer.set.tooltip")
                    ));
                }
                setAsMpDefaultButton.setTooltipDelay(500);
                setAsMpDefaultButton.active = !mpDefault;
                elements.add(setAsMpDefaultButton);
                movingX += smallButtonWidth + SPACING;

                // Copy button
                ImageButton copyButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        0, 0, 20, OptionList.Entry.COPY_ICON, 32, 64,
                        (button) -> {
                            Config.get().copyProfile(profile);
                            list.reload();
                        },
                        Component.empty());
                copyButton.setTooltip(Tooltip.create(
                        localized("option", "main.copy.tooltip")));
                copyButton.setTooltipDelay(500);
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
                deleteButton.setTooltipDelay(500);

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
                        localized("option", "main.remove_link.tooltip")));
                removeButton.setTooltipDelay(500);
                elements.add(removeButton);
            }
        }
    }
}
