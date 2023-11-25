package notryken.commandkeys.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class MsgKeyMapping {
    public transient MutableComponent label;
    public transient Tooltip tooltip;

    public InputConstants.Key keyCode;
    public String msg;
    private KeyMapping keyMapping;


    public MsgKeyMapping() {
        this.keyCode = InputConstants.UNKNOWN;
        this.keyMapping = new KeyMapping("CK" + keyCode.getValue(), keyCode.getValue(),
                "keygroup.commandkeys.mono");
        this.msg = "";
    }

    public boolean isBound() {
        return !keyMapping.isUnbound();
    }

    public boolean isDuplicate() {
        return this.label != null;
    }

    public KeyMapping getKeyMapping() {
        return this.keyMapping;
    }

    public void setKeyCode(InputConstants.Key keyCode) {
        InputConstants.Key oldKeyCode = this.keyCode;
        this.keyCode = keyCode;
        checkDuplicated(oldKeyCode);
    }

    public void checkDuplicated(InputConstants.Key oldKeyCode) {
        if (keyCode == InputConstants.UNKNOWN) {
            if (oldKeyCode != InputConstants.UNKNOWN) {
                label = null;
                tooltip = null;
                KeyMapping.ALL.remove(keyMapping.getName());
                KeyMapping.resetMapping();
                this.keyMapping = new KeyMapping("CK" + InputConstants.UNKNOWN.getValue(),
                        InputConstants.UNKNOWN.getValue(), "keygroup.commandkeys.mono");
            }
        }
        else {
            boolean duplicate = false;

            KeyMapping[] allKeys = Minecraft.getInstance().options.keyMappings;
            MutableComponent duplicates = Component.empty();
            for (KeyMapping mcKey : allKeys) {
                if (keyCode == mcKey.key) {
                    if (duplicate) {
                        duplicates.append(", ");
                    }
                    duplicates.append(Component.translatable(mcKey.getName()));
                    duplicate = true;
                }
            }

            if (duplicate) {
                label = Component.literal("[ ")
                        .append(keyCode.getDisplayName().copy().withStyle(ChatFormatting.WHITE))
                        .append(" ]").withStyle(ChatFormatting.RED);
                tooltip = Tooltip.create(Component.translatable(
                        "controls.keybinds.duplicateKeybinds", duplicates));
                if (isBound()) {
                    KeyMapping.ALL.remove(keyMapping.getName());
                    KeyMapping.resetMapping();
                    this.keyMapping = new KeyMapping("CK" + InputConstants.UNKNOWN.getValue(),
                            InputConstants.UNKNOWN.getValue(), "keygroup.commandkeys.mono");
                }
            }
            else {
                this.label = null;
                this.tooltip = null;
                KeyMapping.ALL.remove(keyMapping.getName());
                KeyMapping.resetMapping();
                this.keyMapping = new KeyMapping("CK" + keyCode.getValue(),
                        keyCode.getValue(), "keygroup.commandkeys.mono");
            }
        }
    }
}
