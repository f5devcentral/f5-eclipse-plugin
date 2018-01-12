/*******************************************************************************
 * Copyright 2015-2017 F5 Networks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.f5.irule.ui;

import java.util.Arrays;
import java.util.List;

/**
 * Disabled Tcl Commands, taken from
 * tmos-bugs-staging pre-release 13.0 as of December 7, 2016
 */
public class DisabledTclCommands {

    private static final List<String> DISABLED_TCL_COMMANDS =
        Arrays.asList(new String[] {
            "auto_execok",
            "auto_import",
            "auto_load",
            "auto_mkindex",
            "auto_mkindex_old",
            "auto_qualify",
            "auto_reset",
            "bgerror",
            "cd",
            "eof",
            "exec",
            "exit",
            "fblocked",
            "fconfigure",
            "fcopy",
            "file",
            "filevent",
            "filename",
            "flush",
            "gets",
            "glob",
            "http",
            "interp",
            "load",
            "memory",
            "namespace",
            "open",
            "package",
            "pid",
            "pkg::create",
            "pkg_mkindex",
            "pwd",
            "rename",
            "seek",
            "socket",
            "source",
            "tcl_findLibrary",
            "tell",
            "time",
            "unknown",
            "update",
            "vwait"
        });

    public static List<String> getList() {
        return DISABLED_TCL_COMMANDS;
    }

}
