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
package com.f5.irule.core.parser;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.declarations.ModuleDeclaration;
import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.ast.references.SimpleReference;
import org.eclipse.dltk.tcl.ast.TclStatement;
import org.eclipse.dltk.tcl.core.AbstractTclCommandProcessor;
import org.eclipse.dltk.tcl.core.ITclCommandDetector;
import org.eclipse.dltk.tcl.core.ITclCommandDetectorExtension;

/**
 * The detectCommand() method is called in real-time as the user types<br>
 * to detect when a full command is recognized.<br>
 * It works in tandem with an {@link AbstractTclCommandProcessor}<br>
 * and is leveraged by ScriptDocumentationProvider for displaying Hover Documentation.
 */
public class TclIruleCommandDetector implements ITclCommandDetector, ITclCommandDetectorExtension {

    @Override
    public void setBuildRuntimeModelFlag (boolean value) {
    }

    @Override
    public void processASTNode (ASTNode node) {
    }

    @Override
    public CommandInfo detectCommand (TclStatement statement, ModuleDeclaration module, ASTNode decl) {
    	// Remember any CommandInfo returned from here should have a processor
    	// extension class defined with a corresponding id entry in plugin.xml

        Expression commandName = statement.getAt(0);
        if (commandName instanceof SimpleReference) { // TODO maybe not the right type
            String value = ((SimpleReference) commandName).getName();

            if (value.equals("when")) {
                // TODO do we need to include the event name (a FieldDeclaration?) as a parameter?
                return new CommandInfo("#irule#when", null);
            }
        }

        return null;
    }
}
