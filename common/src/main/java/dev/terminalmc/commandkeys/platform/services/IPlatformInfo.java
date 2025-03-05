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

package dev.terminalmc.commandkeys.platform.services;

import java.nio.file.Path;

public interface IPlatformInfo {
    /**
     * @return the configuration directory of the instance.
     */
    Path getConfigDir();
}
