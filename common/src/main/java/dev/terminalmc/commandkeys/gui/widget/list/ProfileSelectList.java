/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.gui.widget.list;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.gui.screen.OptionsScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

import static dev.terminalmc.commandkeys.util.Localization.localized;

public class ProfileSelectList extends OptionsList {
    @Nullable Profile editingProfile;

    public ProfileSelectList(Minecraft minecraft, int width, int height, int y,
                             int itemHeight, int entryRelX, int entryWidth, int entryHeight,
                             int scrollWidth, @Nullable Profile editingProfile) {
        super(minecraft, width, height, y, itemHeight, entryRelX,
                entryWidth, entryHeight, scrollWidth);
        this.editingProfile = editingProfile;


        boolean inGame = CommandKeys.inGame();


        addEntry(new OptionsList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal(inGame ? "Active Profile" : "Profiles \u2139"),
                inGame ? null : Tooltip.create(Component.literal("Profiles are automatically " +
                        "activated when you join a world/server linked to a profile.\n" +
                        "The default profiles are used when there is no linked profile.")), 500));


        Config config = Config.get();
        for (int i = 0; i < config.profiles.size(); i++) {
            Profile profile = config.profiles.get(i);
            addEntry(new Entry.ProfileEntry(entryX, entryWidth, entryHeight, this,
                    profile, i, i == config.spDefault, i == config.mpDefault, inGame));
            if (profile.equals(editingProfile)) {
                addEntry(new Entry.ProfileNameEntry(entryX, entryWidth, entryHeight, this, profile));
                for (String address : profile.getLinks()) {
                    addEntry(new Entry.ServerAddressEntry(entryX, entryWidth, entryHeight, this,
                            profile, address));
                }
            }
            if (i == 0 && inGame) {
                addEntry(new OptionsList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                        Component.literal("Other Profiles"), null, -1));
            }
        }

        addEntry(new OptionsList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.empty(), null, -1));
        addEntry(new OptionsList.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> {
                    Config.get().profiles.add(new Profile());
                    reload();
                }));
    }


    @Override
    public OptionsList resize(int width, int height, int y, int itemHeight, double scrollAmount) {
        ProfileSelectList newListWidget = new ProfileSelectList(
                minecraft, width, height, y, itemHeight, entryRelX,
                entryWidth, entryHeight, scrollWidth, editingProfile);
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

    public void openProfileScreen(Profile profile) {
        minecraft.setScreen(new OptionsScreen(screen,
                localized("screen", "edit_profile", profile.getDisplayName()),
                new ProfileEditList(minecraft, screen.width, screen.height, getY(),
                        itemHeight, -200, 400, entryHeight, 420,
                        profile, null)));
    }

    private abstract static class Entry extends OptionsList.Entry {

        private static class ProfileEntry extends Entry {
            ProfileSelectList listWidget;
            Profile profile;

            ProfileEntry(int x, int width, int height, ProfileSelectList listWidget, Profile profile,
                         int index, boolean spDefault, boolean mpDefault, boolean inGame) {
                super();
                this.listWidget = listWidget;
                this.profile = profile;

                int spacing = 5;
                int smallButtonWidth = 20;
                int mainButtonWidth = width - smallButtonWidth * 5 - spacing * 5;
                int mainButtonX = x;

                if (inGame) {
                    if (index == 0) {
                        Button linkButton = Button.builder(Component.literal("\uD83D\uDD17"),
                                        (button) -> {
                                            profile.forceAddAddress(CommandKeys.lastConnection);
                                            listWidget.reload();
                                        })
                                .pos(x, 0)
                                .size(smallButtonWidth, smallButtonWidth)
                                .build();
                        if (profile.getLinks().contains(CommandKeys.lastConnection)) {
                            linkButton.setTooltip(Tooltip.create(Component.literal(
                                    "Already linked to this world/server")));
                            linkButton.active = false;
                        } else {
                            linkButton.setTooltip(Tooltip.create(Component.literal(
                                    "Link to this world/server")));
                        }
                        linkButton.setTooltipDelay(Duration.ofMillis(500));
                        elements.add(linkButton);
                    }
                    else {
                        Button activateButton = Button.builder(Component.literal("\u2191"),
                                        (button) -> {
                                            if (CommandKeys.inGame()) {
                                                profile.forceAddAddress(CommandKeys.lastConnection);
                                            }
                                            Config.get().activateProfile(index);
                                            listWidget.reload();
                                        })
                                .pos(x, 0)
                                .size(smallButtonWidth, smallButtonWidth)
                                .build();
                        activateButton.setTooltip(Tooltip.create(Component.literal(
                                "Activate this profile")));
                        activateButton.setTooltipDelay(Duration.ofMillis(500));
                        elements.add(activateButton);
                    }
                    mainButtonWidth -= (smallButtonWidth + spacing);
                    mainButtonX += (smallButtonWidth + spacing);
                }

                String name = profile.getDisplayName();
                String serverInfo = "";
                int numAddresses = profile.getLinks().size();
                if (numAddresses != 0) {
                    serverInfo = " [" + numAddresses + (numAddresses == 1 ? " Link]" : " Links]");
                }

                elements.add(Button.builder(Component.literal(name).append(Component.literal(serverInfo)
                                        .withStyle(ChatFormatting.GRAY)),
                        (button) -> listWidget.openProfileScreen(profile))
                        .tooltip(Tooltip.create(Component.literal("Edit Profile")))
                        .pos(mainButtonX, 0)
                        .size(mainButtonWidth, height)
                        .build());

                // Switch to right-justified
                int movingX = x + width - smallButtonWidth * 5 - spacing * 4;

                ImageButton configureButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        new WidgetSprites(CONFIGURE_ICON, CONFIGURE_DISABLED_ICON,
                                CONFIGURE_HIGHLIGHTED_ICON),
                        (button) -> {
                            if (listWidget.editingProfile == null) {
                                listWidget.editingProfile = profile;
                            }
                            else if (!listWidget.editingProfile.equals(profile)) {
                                listWidget.editingProfile = profile;
                            }
                            else {
                                listWidget.editingProfile = null;
                            }
                            listWidget.reload();
                        },
                        Component.empty());
                configureButton.setTooltip(Tooltip.create(Component.literal("Edit Details")));
                configureButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(configureButton);
                movingX += smallButtonWidth + spacing;

                Button setAsSpDefaultButton = Button.builder(Component.literal("S"),
                        (button) -> {
                            Config.get().setSpDefaultProfile(index);
                            listWidget.reload();
                        })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                if (spDefault) {
                    setAsSpDefaultButton.setTooltip(Tooltip.create(
                            Component.literal("This is the default profile for singleplayer")));
                } else {
                    setAsSpDefaultButton.setTooltip(Tooltip.create(
                            Component.literal("Set as singleplayer default")));
                }
                setAsSpDefaultButton.setTooltipDelay(Duration.ofMillis(500));
                setAsSpDefaultButton.active = !spDefault;
                elements.add(setAsSpDefaultButton);
                movingX += smallButtonWidth + spacing;

                Button setAsMpDefaultButton = Button.builder(Component.literal("M"),
                                (button) -> {
                                    Config.get().setMpDefaultProfile(index);
                                    listWidget.reload();
                                })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                if (mpDefault) {
                    setAsMpDefaultButton.setTooltip(Tooltip.create(Component.literal(
                            "This is the default profile for multiplayer")));
                } else {
                    setAsMpDefaultButton.setTooltip(Tooltip.create(Component.literal(
                            "Set as multiplayer default")));
                }
                setAsMpDefaultButton.setTooltipDelay(Duration.ofMillis(500));
                setAsMpDefaultButton.active = !mpDefault;
                elements.add(setAsMpDefaultButton);
                movingX += smallButtonWidth + spacing;

                ImageButton copyButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        new WidgetSprites(COPY_ICON, COPY_DISABLED_ICON,
                                COPY_HIGHLIGHTED_ICON),
                        (button) -> {
                            Config.get().copyProfile(profile);
                            listWidget.reload();
                        },
                        Component.empty());
                copyButton.setTooltip(Tooltip.create(Component.literal("Copy profile")));
                copyButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(copyButton);
                movingX += smallButtonWidth + spacing;

                Button deleteButton = Button.builder(Component.literal("\u274C"),
                                (button) -> {
                                    Config.get().profiles.remove(profile);
                                    listWidget.reload();
                                })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                if (spDefault || mpDefault) {
                    deleteButton.active = false;
                    deleteButton.setTooltip(Tooltip.create(Component.literal(
                            "Can't delete a default profile")));
                } else {
                    deleteButton.setTooltip(Tooltip.create(Component.literal(
                            "Delete profile")));
                }
                deleteButton.setTooltipDelay(Duration.ofMillis(500));

                elements.add(deleteButton);
            }
        }

        private static class ProfileNameEntry extends Entry {
            ProfileNameEntry(int x, int width, int height, ProfileSelectList listWidget,
                             Profile profile) {
                super();
                int spacing = 5;
                int smallButtonWidth = 20;
                int labelWidth = 50;
                int nameBoxWidth = width - labelWidth - smallButtonWidth - spacing;

                Button label = Button.builder(Component.literal("Name"), (button -> {}))
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

                Button refreshButton = Button.builder(Component.literal("\ud83d\uddd8"),
                                (button) -> {
                                    listWidget.editingProfile = null;
                                    listWidget.reload();
                                })
                        .pos(x + width - smallButtonWidth, 0)
                        .size(smallButtonWidth, height)
                        .build();
                refreshButton.setTooltip(Tooltip.create(Component.literal("Refresh name")));
                refreshButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(refreshButton);
            }
        }

        private static class ServerAddressEntry extends Entry {
            ServerAddressEntry(int x, int width, int height, ProfileSelectList listWidget,
                               Profile profile, String address) {
                super();
                int spacing = 5;
                int smallButtonWidth = 20;
                int labelWidth = 50;
                int addressBoxWidth = width - labelWidth - smallButtonWidth - spacing;

                Button label = Button.builder(Component.literal("Link"), (button -> {}))
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
                            profile.removeAddress(address);
                            listWidget.reload();
                        })
                        .pos(x + width - smallButtonWidth, 0)
                        .size(smallButtonWidth, height)
                        .build();
                removeButton.setTooltip(Tooltip.create(Component.literal("Remove server")));
                removeButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(removeButton);
            }
        }
    }
}
