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

package dev.terminalmc.commandkeys.platform.services;

import java.nio.file.Path;

public interface IPlatformServices {

    /**
     * @return the name of the current platform.
     */
    String getPlatformName();

    /**
     * @return {@code true} if the mod is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * @return the game directory of the instance.
     */
    Path getGameDir();

    /**
     * @return the configuration directory of the instance.
     */
    Path getConfigDir();

    /**
     * @return {@code true} if in a development environment.
     */
    boolean isDevEnv();

    /**
     * @return the name of the environment type.
     */
    default String getEnvName() {
        return isDevEnv() ? "development" : "production";
    }
}
