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

import java.net.InetSocketAddress;
import java.time.Duration;

import static dev.terminalmc.commandkeys.util.Localization.localized;

public class ProfileSelectList extends OptionsList {
    Profile editingProfile;

    public ProfileSelectList(Minecraft minecraft, int width, int height, int y,
                             int itemHeight, int entryRelX, int entryWidth, int entryHeight,
                             int scrollWidth, @Nullable Profile editingProfile) {
        super(minecraft, width, height, y, itemHeight, entryRelX,
                entryWidth, entryHeight, scrollWidth);
        this.editingProfile = editingProfile;


        boolean inGame = CommandKeys.activeAddress() != null;

        addEntry(new OptionsList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Singleplayer Default Profile"), null, 500));

        Profile spDefaultProfile = Config.get().getSpDefaultProfile();
        addEntry(new Entry.ProfileEntry(entryX, entryWidth, entryHeight, this,
                spDefaultProfile, true, inGame));
        if (spDefaultProfile.equals(editingProfile)) {
            addEntry(new Entry.ProfileNameEntry(entryX, entryWidth, entryHeight, this, spDefaultProfile));
            for (String address : spDefaultProfile.getAddresses()) {
                addEntry(new Entry.ServerAddressEntry(entryX, entryWidth, entryHeight, this,
                        spDefaultProfile, address));
            }
        }

        addEntry(new OptionsList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Multiplayer Default Profile"), null, 500));

        Profile mpDefaultProfile = Config.get().getMpDefaultProfile();
        addEntry(new Entry.ProfileEntry(entryX, entryWidth, entryHeight, this,
                mpDefaultProfile, true, inGame));
        if (mpDefaultProfile.equals(editingProfile)) {
            addEntry(new Entry.ProfileNameEntry(entryX, entryWidth, entryHeight, this, mpDefaultProfile));
            for (String address : mpDefaultProfile.getAddresses()) {
                addEntry(new Entry.ServerAddressEntry(entryX, entryWidth, entryHeight, this,
                        mpDefaultProfile, address));
            }
        }

        addEntry(new OptionsList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Other Profiles"), null, 500));

        for (Profile profile : Config.get().profiles) {
            addEntry(new Entry.ProfileEntry(entryX, entryWidth, entryHeight, this,
                    profile, false, inGame));
            if (profile.equals(editingProfile)) {
                addEntry(new Entry.ProfileNameEntry(entryX, entryWidth, entryHeight, this, profile));
                for (String address : profile.getAddresses()) {
                    addEntry(new Entry.ServerAddressEntry(entryX, entryWidth, entryHeight, this,
                            profile, address));
                }
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
    public OptionsList resize(int width, int height, int y,
                                                                  int itemHeight, double scrollAmount) {
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
                localized("screen", "edit_profile", profile.name)
                        .append(profile.equals(CommandKeys.profile()) ? " [Active]" : " [Inactive]"),
                new ProfileEditList(minecraft, screen.width, screen.height, getY(),
                        itemHeight, -200, 400, entryHeight, 420,
                        profile, null)));
    }

    private abstract static class Entry extends OptionsList.Entry {

        private static class ProfileEntry extends Entry {
            ProfileSelectList listWidget;
            Profile profile;

            ProfileEntry(int x, int width, int height, ProfileSelectList listWidget,
                         Profile profile, boolean isDefault, boolean inGame) {
                super();
                this.listWidget = listWidget;
                this.profile = profile;

                int spacing = 5;
                int smallButtonWidth = 20;
                int mainButtonWidth = width - smallButtonWidth * 5 - spacing * 5;
                int mainButtonX = x;

                if (inGame) {
                    Checkbox selectBox = Checkbox.builder(Component.empty(),
                                    Minecraft.getInstance().font)
                            .pos(x, 0)
                            .selected(profile.equals(CommandKeys.profile()))
                            .onValueChange((checkbox, value) -> {
                                if (value) {
                                    if (CommandKeys.activeAddress() instanceof InetSocketAddress netAddress) {
                                        profile.forceAddAddress(netAddress.getHostName());
                                    }
                                    Config.get().setActiveProfile(profile);
                                    listWidget.reload();
                                }
                            })
                            .build();
                    selectBox.setTooltip(Tooltip.create(Component.literal("Use this profile")));
                    selectBox.setTooltipDelay(Duration.ofMillis(500));
                    elements.add(selectBox);
                    mainButtonWidth -= (smallButtonWidth + spacing);
                    mainButtonX += (smallButtonWidth + spacing);
                }

                String name = profile.name;
                if (name.isBlank()) {
                    name = profile.getAddresses().stream().findFirst().orElse("[No Name]");
                }
                String serverInfo = "";
                int numAddresses = profile.getAddresses().size();
                if (numAddresses != 0) {
                    serverInfo = " [" + numAddresses + (numAddresses == 1 ? " server]" : " servers]");
                }

                elements.add(Button.builder(Component.literal(name)
                                        .append(Component.literal(serverInfo)
                                                .withStyle(ChatFormatting.GRAY)),
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
                        })
                        .pos(mainButtonX, 0)
                        .size(mainButtonWidth, height)
                        .build());

                // Switch to right-justified
                int movingX = x + width - smallButtonWidth * 5 - spacing * 4;

                ImageButton configureButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        new WidgetSprites(CONFIGURE_ICON, CONFIGURE_DISABLED_ICON,
                                CONFIGURE_HIGHLIGHTED_ICON),
                        (button) -> listWidget.openProfileScreen(profile),
                        Component.empty());
                elements.add(configureButton);
                movingX += smallButtonWidth + spacing;

                Button setAsSpDefaultButton = Button.builder(Component.literal("S+"),
                        (button) -> {
                            Config.get().setSpDefaultProfile(profile);
                            listWidget.reload();
                        })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                setAsSpDefaultButton.setTooltip(Tooltip.create(
                        Component.literal("Set as Singleplayer Default")));
                setAsSpDefaultButton.setTooltipDelay(Duration.ofMillis(500));
                setAsSpDefaultButton.active = !isDefault;
                elements.add(setAsSpDefaultButton);
                movingX += smallButtonWidth + spacing;

                Button setAsMpDefaultButton = Button.builder(Component.literal("M+"),
                                (button) -> {
                                    Config.get().setMpDefaultProfile(profile);
                                    listWidget.reload();
                                })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                setAsMpDefaultButton.setTooltip(Tooltip.create(
                        Component.literal("Set as Multiplayer Default")));
                setAsMpDefaultButton.setTooltipDelay(Duration.ofMillis(500));
                setAsMpDefaultButton.active = !isDefault;
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
                deleteButton.setTooltip(Tooltip.create(Component.literal("Delete profile")));
                deleteButton.setTooltipDelay(Duration.ofMillis(500));
                deleteButton.active = !isDefault;
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

                Button label = Button.builder(Component.literal("Name:"), (button -> {}))
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

                Button label = Button.builder(Component.literal("Servers:"), (button -> {}))
                        .pos(x, 0)
                        .size(labelWidth, height)
                        .build();
                label.active = false;
                elements.add(label);

                EditBox addressBox = new EditBox(Minecraft.getInstance().font, x + labelWidth, 0,
                        addressBoxWidth, height, Component.literal("Server Address"));
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