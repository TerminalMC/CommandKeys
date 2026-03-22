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

package dev.terminalmc.commandkeys.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import dev.terminalmc.commandkeys.config.Profile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static dev.terminalmc.commandkeys.util.Localization.localized;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@SuppressWarnings("unchecked")
public class Commands<S> extends CommandDispatcher<S> {

    public void register(CommandDispatcher<S> dispatcher, CommandBuildContext buildContext) {
        Minecraft mc = Minecraft.getInstance();
        dispatcher.register((LiteralArgumentBuilder<S>) literal(CommandKeys.MOD_ID)
                .then(literal("profile")
                        .then(argument("name", StringArgumentType.greedyString())
                                .suggests(((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Config.get()
                                                .getProfiles()
                                                .stream()
                                                .map(Profile::getDisplayName), builder
                                )))
                                .executes(ctx -> {
                                    String oldName = Config.get().activeProfile().name;
                                    String name = StringArgumentType.getString(ctx, "name");

                                    boolean success = false;
                                    int i = 0;
                                    for (Profile profile : Config.get().getProfiles()) {
                                        if (profile.getDisplayName().equals(name)) {
                                            Config.get().activateProfile(i);
                                            success = true;
                                            break;
                                        }
                                        i++;
                                    }

                                    MutableComponent msg = CommandKeys.PREFIX.copy();
                                    if (success) {
                                        msg.append(localized(
                                                "message",
                                                "command.profile.success",
                                                Component.literal(oldName)
                                                        .withStyle(ChatFormatting.RED),
                                                Component.literal(name)
                                                        .withStyle(ChatFormatting.GREEN)
                                        ));
                                    } else {
                                        msg.append(localized(
                                                "message",
                                                "command.profile.failure"
                                        ).withStyle(ChatFormatting.RED));
                                    }

                                    mc.gui.getChat().addClientSystemMessage(msg);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
        );
    }
}
