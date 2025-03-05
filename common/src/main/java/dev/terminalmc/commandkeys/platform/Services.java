/*
 * Framework by TerminalMC
 *
 * To the extent possible under law, the person who associated CC0 with
 * Framework has waived all copyright and related or neighboring rights
 * to Framework.
 *
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package dev.terminalmc.commandkeys.platform;

import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.platform.services.IPlatformInfo;

import java.util.ServiceLoader;

public class Services {
    public static final IPlatformInfo PLATFORM = load(IPlatformInfo.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        CommandKeys.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
