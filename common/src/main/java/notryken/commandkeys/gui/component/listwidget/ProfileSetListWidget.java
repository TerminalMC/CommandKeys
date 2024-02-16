package notryken.commandkeys.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import notryken.commandkeys.CommandKeys;
import notryken.commandkeys.config.Profile;
import notryken.commandkeys.gui.screen.ConfigScreen;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import static notryken.commandkeys.CommandKeys.config;

public class ProfileSetListWidget extends ConfigListWidget {
    boolean singleplayer;
    Profile editingProfile;

    public ProfileSetListWidget(Minecraft minecraft, int width, int height, int top, int bottom,
                                int itemHeight, int entryRelX, int entryWidth, int entryHeight,
                                int scrollWidth, boolean singleplayer, @Nullable Profile editingProfile) {
        super(minecraft, width, height, top, bottom, itemHeight, entryRelX,
                entryWidth, entryHeight, scrollWidth);
        this.singleplayer = singleplayer;
        this.editingProfile = editingProfile;

        addEntry(new Entry.ProfileListSelectionEntry(entryX, entryWidth, entryHeight, this));

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal(singleplayer ? "Singleplayer Profiles" : "Multiplayer Profiles"),
                null, 500));

        List<Profile> profileList;

        boolean inGame = CommandKeys.activeAddress() != null;

        if (singleplayer) {
            profileList = config().getSpProfiles();
            Profile profile = config().getSpDefaultProfile();
            addEntry(new Entry.ProfileEntry(entryX, entryWidth, entryHeight, this,
                    profile, true, inGame));
            if (profile.equals(editingProfile)) {
                addEntry(new Entry.ProfileNameEntry(entryX, entryWidth, entryHeight, this, profile));
                for (String address : profile.getAddresses()) {
                    addEntry(new Entry.ServerAddressEntry(entryX, entryWidth, entryHeight, this,
                            profile, address));
                }
            }
        }
        else {
            profileList = config().getMpProfiles();
            Profile profile = config().getMpDefaultProfile();
            addEntry(new Entry.ProfileEntry(entryX, entryWidth, entryHeight, this,
                    profile, true, inGame));
            if (profile.equals(editingProfile)) {
                addEntry(new Entry.ProfileNameEntry(entryX, entryWidth, entryHeight, this, profile));
                for (String address : profile.getAddresses()) {
                    addEntry(new Entry.ServerAddressEntry(entryX, entryWidth, entryHeight, this,
                            profile, address));
                }
            }
        }
        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("------------------------------"), null, 500));
        for (Profile profile : profileList) {
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
        addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> addNewProfile(singleplayer)));
    }

    // Utility method since lambdas don't like non-final vars
    private void addNewProfile(boolean singleplayer) {
        Profile newProfile = new Profile(singleplayer);
        editingProfile = newProfile;
        if (singleplayer) config().addSpProfile(newProfile);
        else config().addMpProfile(newProfile);
        reload();
    }


    @Override
    public ConfigListWidget resize(int width, int height, int top, int bottom,
                                   int itemHeight, double scrollAmount) {
        ProfileSetListWidget newListWidget = new ProfileSetListWidget(
                minecraft, width, height, top, bottom, itemHeight, entryRelX,
                entryWidth, entryHeight, scrollWidth, singleplayer, editingProfile);
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
        minecraft.setScreen(new ConfigScreen(screen,
                Component.translatable("screen.commandkeys.title.profile")
                        .append(Component.literal(profile.name))
                        .append(profile.equals(CommandKeys.profile()) ? " [Active]" : " [Inactive]"),
                new ProfileListWidget(minecraft, screen.width, screen.height, y0, y1,
                        itemHeight, -200, 400, entryHeight, 420,
                        profile, null)));
    }

    private abstract static class Entry extends ConfigListWidget.Entry {

        private static class ProfileListSelectionEntry extends Entry {
            ProfileListSelectionEntry(int x, int width, int height, ProfileSetListWidget listWidget) {
                super();
                int spacing = 5;
                int buttonWidth = (width - spacing) / 2;
                Button spButton = Button.builder(Component.literal("Singleplayer Profiles"),
                                (button) -> {
                                    listWidget.singleplayer = true;
                                    listWidget.reload();
                                })
                        .pos(x, 0)
                        .size(buttonWidth, height)
                        .build();
                if (listWidget.singleplayer) spButton.active = false;
                elements.add(spButton);
                // Switch to right-justified
                Button mpButton = Button.builder(Component.literal("Multiplayer Profiles"),
                                (button) -> {
                                    listWidget.singleplayer = false;
                                    listWidget.reload();
                                })
                        .pos(x + width - buttonWidth, 0)
                        .size(buttonWidth, height)
                        .build();
                if (!listWidget.singleplayer) mpButton.active = false;
                elements.add(mpButton);
            }
        }

        private static class ProfileEntry extends Entry {
            ProfileSetListWidget listWidget;
            Profile profile;

            ProfileEntry(int x, int width, int height, ProfileSetListWidget listWidget,
                         Profile profile, boolean isDefault, boolean inGame) {
                super();
                this.listWidget = listWidget;
                this.profile = profile;

                int spacing = 5;
                int smallButtonWidth = 20;
                int mainButtonWidth = width - smallButtonWidth * 5 - spacing * 5;

                ImageButton configureButton = new ImageButton(x, 0, smallButtonWidth, height,
                        0, 0, 20, ConfigListWidget.Entry.CONFIGURATION_ICON, 32, 64,
                        (button) -> listWidget.openProfileScreen(profile),
                        Component.empty());
                elements.add(configureButton);

                String name = profile.name;
                if (isDefault) name = name + " [Default]";
                elements.add(Button.builder(Component.literal(name),
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
                        .pos(x + smallButtonWidth + spacing, 0)
                        .size(mainButtonWidth, height)
                        .build());

                // Switch to right-justified
                int movingX = x + width - smallButtonWidth * 4 - spacing * 3;

                if (inGame) {
                    Checkbox selectBox = new SelectBox(movingX, 0,
                            smallButtonWidth, height, Component.literal("Select"),
                            profile.equals(CommandKeys.profile()));
                    selectBox.setTooltip(Tooltip.create(Component.literal("Use this profile")));
                    selectBox.setTooltipDelay(500);
                    elements.add(selectBox);
                }
                movingX += smallButtonWidth + spacing;

                ImageButton setAsDefaultButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        0, 0, 20, ConfigListWidget.Entry.SET_DEFAULT_ICON, 32, 64,
                        (button) -> {
                            config().setAsDefault(profile);
                            listWidget.reload();
                        },
                        Component.empty());
                setAsDefaultButton.setTooltip(Tooltip.create(Component.literal("Set as default")));
                setAsDefaultButton.setTooltipDelay(500);
                setAsDefaultButton.active = !isDefault;
                elements.add(setAsDefaultButton);
                movingX += smallButtonWidth + spacing;

                ImageButton copyButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        0, 0, 20, ConfigListWidget.Entry.COPY_ICON, 32, 64,
                        (button) -> {
                            config().copyProfile(profile);
                            listWidget.reload();
                        },
                        Component.empty());
                copyButton.setTooltip(Tooltip.create(Component.literal("Copy profile")));
                copyButton.setTooltipDelay(500);
                elements.add(copyButton);
                movingX += smallButtonWidth + spacing;

                Button deleteButton = Button.builder(Component.literal("\u274C"),
                                (button) -> {
                                    config().removeProfile(profile);
                                    listWidget.reload();
                                })
                        .pos(movingX, 0)
                        .size(smallButtonWidth, height)
                        .build();
                deleteButton.setTooltip(Tooltip.create(Component.literal("Delete profile")));
                deleteButton.setTooltipDelay(500);
                deleteButton.active = !isDefault;
                elements.add(deleteButton);
            }

            private class SelectBox extends Checkbox {
                public SelectBox(int x, int y, int width, int height, Component message, boolean selected) {
                    super(x, y, width, height, message, selected, false);
                }

                @Override
                public void onPress() {
                    // Can assume in-game
                    SocketAddress address = CommandKeys.activeAddress();
                    if (address instanceof InetSocketAddress netAddress) {
                        if (profile.singleplayer) {
                            Profile copyProfile = new Profile(profile);
                            copyProfile.forceAddAddress(netAddress.getHostName());
                            config().addMpProfile(copyProfile);
                            config().setActiveProfile(copyProfile);
                            listWidget.singleplayer = false;
                        }
                        else {
                            profile.forceAddAddress(netAddress.getHostName());
                            config().setActiveProfile(profile);
                        }
                    }
                    else {
                        if (profile.singleplayer) {
                            config().setActiveProfile(profile);
                        }
                        else {
                            Profile copyProfile = new Profile(profile);
                            copyProfile.singleplayer = true;
                            config().addSpProfile(copyProfile);
                            config().setActiveProfile(copyProfile);
                            listWidget.singleplayer = true;
                        }
                    }
                    listWidget.reload();
                }
            }
        }

        private static class ProfileNameEntry extends Entry {
            ProfileNameEntry(int x, int width, int height, ProfileSetListWidget listWidget,
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
                        nameBoxWidth, height, Component.literal("Profile Name"));
                nameBox.setMaxLength(64);
                nameBox.setValue(profile.name);
                nameBox.setResponder((value) -> profile.name = value);
                elements.add(nameBox);

                Button refreshButton = Button.builder(Component.literal("\ud83d\uddd8"),
                                (button) -> listWidget.reload())
                        .pos(x + width - smallButtonWidth, 0)
                        .size(smallButtonWidth, height)
                        .build();
                refreshButton.setTooltip(Tooltip.create(Component.literal("Refresh name")));
                refreshButton.setTooltipDelay(500);
                elements.add(refreshButton);
            }
        }

        private static class ServerAddressEntry extends Entry {
            ServerAddressEntry(int x, int width, int height, ProfileSetListWidget listWidget,
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
                removeButton.setTooltipDelay(500);
                elements.add(removeButton);
            }
        }
    }
}
