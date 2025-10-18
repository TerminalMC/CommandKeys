/*
 * Copyright 2025 TerminalMC
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

package dev.terminalmc.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.util.JsonUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static dev.terminalmc.commandkeys.CommandKeys.canTrigger;
import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * Consists of behavioral controls, a primary and alternate {@link Keybind}, and a list of
 * {@link Message} instances.
 */
public class Macro {

    public static final int VERSION = 6;
    public final int version = VERSION;

    public static final Random RANDOM = new Random();

    // Managed controls

    boolean addToHistory;
    public static final boolean addToHistoryDefault = false;
    transient boolean addToHistoryStatus;

    boolean showHudMessage;
    public static final boolean showHudMessageDefault = false;
    transient boolean showHudMessageStatus;

    boolean resumeRepeating;
    public static final boolean resumeRepeatingDefault = false;
    transient boolean resumeRepeatingStatus;

    boolean useRatelimit;
    public static final boolean useRatelimitDefault = true;
    transient boolean useRatelimitStatus;

    // Local controls

    ConflictStrategy conflictStrategy;
    public static final ConflictStrategy conflictStrategyDefault = ConflictStrategy.SUBMIT;

    public enum ConflictStrategy {
        SUBMIT(ChatFormatting.GREEN),
        ASSERT(ChatFormatting.GOLD),
        VETO(ChatFormatting.RED),
        AVOID(ChatFormatting.AQUA);

        private final ChatFormatting style;

        ConflictStrategy(ChatFormatting style) {
            this.style = style;
        }

        public Component title() {
            return localized("option", "macro.conflict." + name()).withStyle(style);
        }

        public Component tooltip() {
            return localized("option", "macro.conflict." + name() + ".tooltip");
        }
    }

    SendMode sendMode;
    public static final SendMode sendModeDefault = SendMode.SEND;

    public enum SendMode {
        SEND(ChatFormatting.GREEN),
        TYPE(ChatFormatting.GOLD),
        EDIT(ChatFormatting.BLUE),
        CYCLE(ChatFormatting.AQUA),
        RANDOM(ChatFormatting.LIGHT_PURPLE),
        REPEAT(ChatFormatting.RED);

        private final ChatFormatting style;

        SendMode(ChatFormatting style) {
            this.style = style;
        }

        public Component title() {
            return localized("option", "macro.send." + name()).withStyle(style);
        }

        public Component tooltip() {
            return localized("option", "macro.send." + name() + ".tooltip");
        }
    }

    ActivationType activationType;
    public static final ActivationType activationTypeDefault = ActivationType.HOLD;

    public enum ActivationType {
        HOLD(ChatFormatting.GREEN),
        VANILLA(ChatFormatting.GOLD);

        private final ChatFormatting style;

        ActivationType(ChatFormatting style) {
            this.style = style;
        }

        public Component title() {
            return localized("option", "macro.activation." + name()).withStyle(style);
        }

        public Component tooltip() {
            return localized("option", "macro.activation." + name() + ".tooltip");
        }
    }

    /**
     * Standard delay between messages when sending.
     */
    public int spaceTicks;
    public static final int spaceTicksDefault = 0;

    /**
     * Maximum number of repetitions when repeating.
     */
    public int maxRepeats;
    public static final int maxRepeatsDefault = 0;

    /**
     * Index of next message forwards when cycling.
     */
    public transient int cycleIndex;
    public static final int cycleIndexDefault = 0;

    public int mcKeybind;
    public static final int mcKeybindDefault = 0;

    // Keybinds

    /**
     * Primary keybind used for activation.
     */
    Keybind keybind;
    public static final Supplier<Keybind> keybindDefault = Keybind::new;

    /**
     * Alternate keybind used for activation of special functions.
     */
    Keybind altKeybind;
    public static final Supplier<Keybind> altKeybindDefault = Keybind::new;

    // Messages

    final List<Message> messages;
    public static final Supplier<List<Message>> messagesDefault = ArrayList::new;

    /**
     * Creates a default instance with no {@link Message}s.
     */
    public Macro() {
        this(
                addToHistoryDefault,
                showHudMessageDefault,
                resumeRepeatingDefault,
                useRatelimitDefault,
                Config.get().defaultConflictStrategy,
                Config.get().defaultSendMode,
                Config.get().defaultActivationType,
                spaceTicksDefault,
                maxRepeatsDefault,
                cycleIndexDefault,
                mcKeybindDefault,
                keybindDefault.get(),
                altKeybindDefault.get(),
                messagesDefault.get()
        );
    }

    /**
     * Not validated, only for use by default constructor and self-validating deserializer.
     */
    Macro(
            boolean addToHistory,
            boolean showHudMessage,
            boolean resumeRepeating,
            boolean useRatelimit,
            ConflictStrategy conflictStrategy,
            SendMode sendMode,
            ActivationType activationType,
            int spaceTicks,
            int maxRepeats,
            int cycleIndex,
            int mcKeybind,
            Keybind keybind,
            Keybind altKeybind,
            List<Message> messages
    ) {
        this.addToHistory = addToHistory;
        this.showHudMessage = showHudMessage;
        this.resumeRepeating = resumeRepeating;
        this.useRatelimit = useRatelimit;
        this.conflictStrategy = conflictStrategy;
        this.sendMode = sendMode;
        this.activationType = activationType;
        this.spaceTicks = spaceTicks;
        this.maxRepeats = maxRepeats;
        this.cycleIndex = cycleIndex;
        this.mcKeybind = mcKeybind;
        this.keybind = keybind;
        this.altKeybind = altKeybind;
        this.messages = messages;
    }

    /**
     * Copy constructor.
     */
    Macro(Macro macro) {
        this.addToHistory = macro.addToHistory;
        this.addToHistoryStatus = macro.addToHistoryStatus;
        this.showHudMessage = macro.showHudMessage;
        this.showHudMessageStatus = macro.showHudMessageStatus;
        this.resumeRepeating = macro.resumeRepeating;
        this.resumeRepeatingStatus = macro.resumeRepeatingStatus;
        this.useRatelimit = macro.useRatelimit;
        this.useRatelimitStatus = macro.useRatelimitStatus;
        this.conflictStrategy = macro.conflictStrategy;
        this.sendMode = macro.sendMode;
        this.activationType = macro.activationType;
        this.spaceTicks = macro.spaceTicks;
        this.maxRepeats = macro.maxRepeats;
        this.cycleIndex = macro.cycleIndex;
        this.mcKeybind = macro.mcKeybind;
        this.keybind = new Keybind(macro.keybind);
        this.altKeybind = new Keybind(macro.altKeybind);
        this.messages = macro.messages.stream()
                .map(Message::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // Managed control accessors

    public boolean getAddToHistory() {
        return addToHistory;
    }

    public boolean getAddToHistoryStatus() {
        return addToHistoryStatus;
    }

    public boolean getShowHudMessage() {
        return showHudMessage;
    }

    public boolean getShowHudMessageStatus() {
        return showHudMessageStatus;
    }

    public boolean getResumeRepeating() {
        return resumeRepeating;
    }

    public boolean getResumeRepeatingStatus() {
        return resumeRepeatingStatus;
    }

    public boolean getUseRatelimit() {
        return useRatelimit;
    }

    public boolean getUseRatelimitStatus() {
        return useRatelimitStatus;
    }

    // Other accessors

    public ConflictStrategy getStrategy() {
        return conflictStrategy;
    }

    public SendMode getMode() {
        return sendMode;
    }

    public ActivationType getActivationType() {
        return activationType;
    }

    public Keybind getKeybind() {
        return keybind;
    }

    public Keybind getAltKeybind() {
        return altKeybind;
    }

    // Keybind utils

    /**
     * @return {@code true} if {@code keybind} belongs to this macro.
     */
    public boolean ownsKeybind(Keybind keybind) {
        return (keybind == this.keybind) || (keybind == this.altKeybind);
    }

    /**
     * @return {@code true} if this macro is using its alternate keybind.
     */
    public boolean usesAltKeybind() {
        return sendMode.equals(SendMode.CYCLE);
    }

    // Message management

    /**
     * @return an unmodifiable view of the messages list.
     */
    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public void setMessage(int index, String str) {
        this.messages.get(index).string = str;
    }

    public void removeMessage(int index) {
        this.messages.remove(index);
    }

    /**
     * Moves the {@link Message} at the source index to the destination index.
     *
     * @param sourceIndex the index of the element to move.
     * @param destIndex   the desired final index of the element.
     * @return {@code true} if the list was modified.
     */
    public boolean moveMessage(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            messages.add(destIndex, messages.remove(sourceIndex));
            return true;
        }
        return false;
    }

    // Activation

    /**
     * A macro is considered to be 'active' when it should perform its action on an ongoing basis,
     * if it is capable of doing so.
     *
     * <p>{@link ActivationType#HOLD} requires that macros are activated when
     * initially triggered, and deactivated only when the {@link Macro#tick} method finds the
     * keybind to be released. Attempting to trigger this type of macro when active does
     * nothing.</p>
     *
     * <p>{@link ActivationType#VANILLA} requires that macros are activated when
     * initially triggered, but instead of remaining active until the keybind is released, they are
     * either immediately deactivated if they have no ongoing action, or remain active until
     * triggered again otherwise.</p>
     */
    private transient boolean active = false;
    private transient int activeTicks = 0;
    private transient int repetitions = 0;

    public void tick() {
        // Tick scheduled messages, removing those that have finished
        scheduledMessages.removeIf(ScheduledMessage::tick);

        if (active) {
            // Deactivate if key has been released
            if (activationType == ActivationType.HOLD && !keybind.isKeyDown()) {
                deactivate();
            }
            // Tick ongoing actions
            else {
                //noinspection SwitchStatementWithTooFewBranches
                switch (sendMode) {
                    case REPEAT -> {
                        if (spaceTicks == 0 || activeTicks > 0 && activeTicks % spaceTicks == 0) {
                            if (maxRepeats == 0 || ++repetitions <= maxRepeats) {
                                scheduleAll(false);
                            }
                        }
                    }
                }
                activeTicks++;
            }
        }
    }

    /**
     * @return {@code true} if ratelimited.
     */
    public boolean trigger(@Nullable Keybind keybind, boolean ratelimited) {
        if (active) {
            singleActionComplete();
        } else {
            // Only check ratelimiter if we've actually got something to do
            if (keybind != null && useRatelimitStatus && !canTrigger(
                    keybind.getKey(),
                    !ratelimited
            )) {
                return true;
            }
            activate(keybind);
        }
        return false;
    }

    public void deactivate() {
        active = false;
    }

    public void deactivateAndCancel() {
        deactivate();
        clearScheduled();
    }

    private void singleActionComplete() {
        if (activationType != ActivationType.HOLD)
            deactivate();
    }

    private void activate(@Nullable Keybind keybind) {
        active = true;
        activeTicks = -1; // trigger is processed prior to tick
        repetitions = 1;
        switch (sendMode) {
            case SEND -> {
                scheduleAll(spaceTicks != 0);
                singleActionComplete();
            }
            case REPEAT -> scheduleAll(false);
            case EDIT -> {
                // Edit the first message
                if (!messages.isEmpty()) {
                    CommandKeys.edit(messages.getFirst().string);
                }
                singleActionComplete();
            }
            case TYPE -> {
                // Type the first message
                if (!messages.isEmpty()) {
                    CommandKeys.type(messages.getFirst().string);
                }
                singleActionComplete();
            }
            case CYCLE -> {
                if (altKeybind.equals(keybind)) {
                    // Alt keybind cycles backwards
                    if (--cycleIndex < 0)
                        cycleIndex = messages.size() - 1;
                } else {
                    // Main keybind cycles forwards
                    if (++cycleIndex >= messages.size())
                        cycleIndex = 0;
                }
                // Split to allow multiple messages per press
                for (String str : messages.get(cycleIndex).string.split(",,")) {
                    // Blank messages are treated as spacers
                    if (!str.isBlank()) {
                        schedule(
                                messages.get(cycleIndex).delayTicks,
                                str,
                                addToHistoryStatus,
                                showHudMessageStatus
                        );
                    }
                }
                singleActionComplete();
            }
            case RANDOM -> {
                // Pick a random message and send it
                if (!messages.isEmpty()) {
                    Message msg = messages.get(RANDOM.nextInt(messages.size()));
                    if (!msg.string.isBlank()) {
                        schedule(
                                msg.delayTicks,
                                msg.string,
                                addToHistoryStatus,
                                showHudMessageStatus
                        );
                    }
                }
                singleActionComplete();
            }
        }
    }

    // Message scheduling

    private transient final List<ScheduledMessage> scheduledMessages = new ArrayList<>();

    /**
     * Schedules the message to send after {@code delay} ticks.
     */
    private void schedule(int delay, String message, boolean addToHistory, boolean showHudMessage) {
        if (delay > 0) {
            scheduledMessages.add(new ScheduledMessage(
                    delay,
                    message,
                    addToHistory,
                    showHudMessage
            ));
        } else {
            CommandKeys.send(message, showHudMessage, addToHistory);
        }
    }

    private void scheduleAll(boolean standardDelay) {
        int totalDelay = standardDelay ? -spaceTicks : 0;
        for (Message msg : messages) {
            totalDelay += standardDelay ? spaceTicks : msg.delayTicks;
            if (!msg.string.isBlank()) {
                schedule(totalDelay, msg.string, addToHistoryStatus, showHudMessageStatus);
            }
        }
    }

    /**
     * Clears all scheduled messages, including repeating ones.
     */
    private void clearScheduled() {
        scheduledMessages.clear();
    }

    private static class ScheduledMessage {

        private int delay;
        final String message;
        final boolean showHudMessage;
        final boolean addToHistory;

        public ScheduledMessage(
                int delay,
                String message,
                boolean showHudMessage,
                boolean addToHistory
        ) {
            this.delay = delay;
            this.message = message;
            this.showHudMessage = showHudMessage;
            this.addToHistory = addToHistory;
        }

        /**
         * @return {@code true} if this {@link ScheduledMessage} has finished ticking, {@code false}
         * otherwise.
         */
        private boolean tick() {
            if (--delay <= 0) {
                CommandKeys.send(message, showHudMessage, addToHistory);
                return true;
            }
            return false;
        }
    }

    // Validation

    Macro validate() {
        if (spaceTicks < 0)
            spaceTicks = 0;
        if (maxRepeats < 0)
            maxRepeats = 0;

        keybind.validate();
        altKeybind.validate();

        messages.forEach(Message::validate);
        cleanupMessages();

        return this;
    }

    void cleanupMessages() {
        messages.removeIf((msg) -> {
            // Never allow leading whitespace
            msg.string = msg.string.stripLeading();
            // Only allow trailing whitespace for TYPE mode
            if (!sendMode.equals(SendMode.TYPE) && !sendMode.equals(SendMode.EDIT)) {
                msg.string = msg.string.stripTrailing();
            }
            // Only allow blank messages for CYCLE mode (as spacers) and TYPE
            // mode (to open chat)
            return (msg.string.isBlank() && !sendMode.equals(SendMode.CYCLE) && !sendMode.equals(
                    SendMode.TYPE));
        });
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Macro> {

        @Override
        public Macro deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.has("version") ? obj.get("version").getAsInt() : 0;
            boolean silent = version != VERSION;

            boolean addToHistory =
                    JsonUtil.getOrDefault(obj, "addToHistory", addToHistoryDefault, silent);

            boolean showHudMessage =
                    JsonUtil.getOrDefault(obj, "showHudMessage", showHudMessageDefault, silent);

            boolean resumeRepeating =
                    JsonUtil.getOrDefault(obj, "resumeRepeating", resumeRepeatingDefault, silent);

            boolean useRatelimit =
                    JsonUtil.getOrDefault(obj, "useRatelimit", useRatelimitDefault, silent);

            ConflictStrategy conflictStrategy = version >= 3 // Since 2.1.0-beta.2
                    ? JsonUtil.getOrDefault(
                    obj,
                    "conflictStrategy",
                    ConflictStrategy.class,
                    conflictStrategyDefault,
                    silent
            ) : getConflictStrategy(JsonUtil.getOrDefault(obj, "conflictStrategy", "", true));

            SendMode sendMode = version >= 3
                    // Since 2.1.0-beta.2
                    ? JsonUtil.getOrDefault(
                    obj,
                    "sendMode",
                    SendMode.class,
                    sendModeDefault,
                    silent
            )
                    : getSendMode(JsonUtil.getOrDefault(obj, "sendMode", "", true));

            ActivationType activationType = JsonUtil.getOrDefault(
                    obj,
                    "activationType",
                    ActivationType.class,
                    activationTypeDefault,
                    silent
            );

            int spaceTicks = JsonUtil.getOrDefault(obj, "spaceTicks", spaceTicksDefault, silent);

            int maxRepeats = JsonUtil.getOrDefault(obj, "maxRepeats", maxRepeatsDefault, silent);

            int mcKeybind = JsonUtil.getOrDefault(obj, "mcKeybind", mcKeybindDefault, silent);

            Keybind keybind = version >= 4 // Since 2.3.0-beta.1
                    ? JsonUtil.getOrDefault(
                    ctx,
                    obj,
                    "keybind",
                    Keybind.class,
                    new Keybind(),
                    silent
            ) : new Keybind(
                    JsonUtil.getOrDefault(obj, "keyName", InputConstants.UNKNOWN, true),
                    JsonUtil.getOrDefault(obj, "limitKeyName", InputConstants.UNKNOWN, true)
            ).validate();

            Keybind altKeybind = JsonUtil.getOrDefault(
                    ctx,
                    obj,
                    "altKeybind",
                    Keybind.class,
                    new Keybind(),
                    silent
            );

            List<Message> messages = JsonUtil.getOrDefault(
                    ctx,
                    obj,
                    "messages",
                    Message.class,
                    messagesDefault.get(),
                    silent
            );

            return new Macro(
                    addToHistory,
                    showHudMessage,
                    resumeRepeating,
                    useRatelimit,
                    conflictStrategy,
                    sendMode,
                    activationType,
                    spaceTicks,
                    maxRepeats,
                    0,
                    mcKeybind,
                    keybind,
                    altKeybind,
                    messages
            ).validate();
        }

        /**
         * Legacy format util.
         */
        public static ConflictStrategy getConflictStrategy(String str) {
            return switch (str) {
                case "ZERO" -> ConflictStrategy.SUBMIT;
                case "ONE" -> ConflictStrategy.ASSERT;
                case "TWO" -> ConflictStrategy.VETO;
                case "THREE" -> ConflictStrategy.AVOID;
                default -> conflictStrategyDefault;
            };
        }

        /**
         * Legacy format util.
         */
        public static SendMode getSendMode(String str) {
            return switch (str) {
                case "ZERO" -> SendMode.SEND;
                case "ONE" -> SendMode.TYPE;
                case "TWO" -> SendMode.CYCLE;
                default -> sendModeDefault;
            };
        }
    }
}
