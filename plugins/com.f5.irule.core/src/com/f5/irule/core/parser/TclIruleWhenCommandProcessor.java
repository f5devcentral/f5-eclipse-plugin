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

import com.f5.irule.core.parser.ast.TclIruleWhenStatement;
import com.f5.irule.core.text.TclIruleSchema;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.ast.references.SimpleReference;
import org.eclipse.dltk.tcl.ast.TclStatement;
import org.eclipse.dltk.tcl.ast.expressions.TclBlockExpression;
import org.eclipse.dltk.tcl.core.AbstractTclCommandProcessor;
import org.eclipse.dltk.tcl.core.ITclParser;

/*
 * Processor for the iRule 'when' command
 */
public class TclIruleWhenCommandProcessor extends AbstractTclCommandProcessor {

    public TclIruleWhenCommandProcessor () {
    }

    @Override
    public ASTNode process (TclStatement statement, ITclParser parser, ASTNode parent) {
        // statement takes 2 arguments
        if (statement.getCount() != 3) {
            //System.out.println(".core.parser.TclIruleWhenCommandProcessor.process:: ERROR - invalid declaration -> " + statement);
            return null;
        }

        // verify arg1 is a valid event name
        Expression arg1 = statement.getAt(1);
        //System.out.println(".core.parser.TclIruleWhenCommandProcessor.process:: " + arg1);
        SimpleReference event = null;
        if (arg1 instanceof SimpleReference) {
            event = (SimpleReference) arg1;

            if (!TclIruleSchema.isEvent(event.getName())) {
               return null;
            }

        } else {
            return null;
        }

        // verify arg2 is a TclBlockExpression
        Expression arg2 = statement.getAt(2);
        //System.out.println(".core.parser.TclIruleWhenCommandProcessor.process:: " + arg2);
        TclBlockExpression block = null;
        if (arg2 instanceof TclBlockExpression) {
            block = (TclBlockExpression) arg2;

        } else {
            return null;
        }

        TclIruleWhenStatement ws = new TclIruleWhenStatement(event.getName(), block, statement.sourceStart(), statement.sourceEnd());

        this.addToParent(parent, ws);

        return ws;
    }
}