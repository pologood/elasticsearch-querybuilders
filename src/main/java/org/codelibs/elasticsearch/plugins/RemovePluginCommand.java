/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codelibs.elasticsearch.plugins;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.lucene.util.IOUtils;
import org.codelibs.elasticsearch.cli.ExitCodes;
import org.codelibs.elasticsearch.cli.SettingCommand;
import org.codelibs.elasticsearch.cli.Terminal;
import org.codelibs.elasticsearch.cli.UserException;
import org.codelibs.elasticsearch.common.Strings;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.env.Environment;
import org.codelibs.elasticsearch.node.internal.InternalSettingsPreparer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.codelibs.elasticsearch.cli.Terminal.Verbosity.VERBOSE;

/**
 * A command for the plugin cli to remove a plugin from elasticsearch.
 */
class RemovePluginCommand extends SettingCommand {

    private final OptionSpec<String> arguments;

    RemovePluginCommand() {
        super("Removes a plugin from elasticsearch");
        this.arguments = parser.nonOptions("plugin name");
    }

    @Override
    protected void execute(Terminal terminal, OptionSet options, Map<String, String> settings) throws Exception {
        String arg = arguments.value(options);
        execute(terminal, arg, settings);
    }

    // pkg private for testing
    void execute(Terminal terminal, String pluginName, Map<String, String> settings) throws Exception {
        final Environment env = InternalSettingsPreparer.prepareEnvironment(Settings.EMPTY, terminal, settings);

        terminal.println("-> Removing " + Strings.coalesceToEmpty(pluginName) + "...");

        final Path pluginDir = env.pluginsFile().resolve(pluginName);
        if (Files.exists(pluginDir) == false) {
            throw new UserException(
                    ExitCodes.CONFIG,
                    "plugin " + pluginName + " not found; run 'elasticsearch-plugin list' to get list of installed plugins");
        }

        final List<Path> pluginPaths = new ArrayList<>();

        final Path pluginBinDir = env.binFile().resolve(pluginName);
        if (Files.exists(pluginBinDir)) {
            if (Files.isDirectory(pluginBinDir) == false) {
                throw new UserException(ExitCodes.IO_ERROR, "Bin dir for " + pluginName + " is not a directory");
            }
            pluginPaths.add(pluginBinDir);
            terminal.println(VERBOSE, "Removing: " + pluginBinDir);
        }

        terminal.println(VERBOSE, "Removing: " + pluginDir);
        final Path tmpPluginDir = env.pluginsFile().resolve(".removing-" + pluginName);
        Files.move(pluginDir, tmpPluginDir, StandardCopyOption.ATOMIC_MOVE);
        pluginPaths.add(tmpPluginDir);

        IOUtils.rm(pluginPaths.toArray(new Path[pluginPaths.size()]));

        // we preserve the config files in case the user is upgrading the plugin, but we print
        // a message so the user knows in case they want to remove manually
        final Path pluginConfigDir = env.configFile().resolve(pluginName);
        if (Files.exists(pluginConfigDir)) {
            terminal.println(
                    "-> Preserving plugin config files [" + pluginConfigDir + "] in case of upgrade, delete manually if not needed");
        }
    }

}
