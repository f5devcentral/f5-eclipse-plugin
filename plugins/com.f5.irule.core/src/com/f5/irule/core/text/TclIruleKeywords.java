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
package com.f5.irule.core.text;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.dltk.tcl.core.ITclKeywords;
import org.eclipse.ui.statushandlers.StatusManager;

import com.f5.irule.core.Ids;

/**
 * Used to provide a list of keywords,<br>
 * composed of JET specified iRule Commands and Events.<br>
 * The list is extracted from irule-schema.json via {@link TclIruleSchema}.
 */
public class TclIruleKeywords implements ITclKeywords {
    private static String[] iruleKeywords = {
            // Can initialize a static list here
    };

    /*
    private static String[] moduleKeywords = { "foo_mod" };

    private static String[] functionKeywords = { "foo_func" };

    private static String[] namespaceKeywords = { "foo_namespace" };

    private static String[] execKeywords = { "foo_exec" };
    */

    @Override
    public String[] getKeywords() {
        if (iruleKeywords.length == 0) {
            iruleKeywords = TclIruleSchema.getCommandNames();

            IStatus status = new Status(IStatus.INFO, Ids.PLUGIN, "Tcl IRule keywords: " + iruleKeywords.length);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }

        return iruleKeywords;
    }

    @Override
    public String[] getKeywords(int type) {
        /*
         * if (type == MODULE) { return moduleKeywords; } else if (type == FUNCTION) { return functionKeywords; } else
         * if (type == NAMESPACE) { return namespaceKeywords; } else if (type == EXEC_EXPRESSION) { return execKeywords;
         * }
         */
        return getKeywords();
    }
}
