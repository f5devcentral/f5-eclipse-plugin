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

import java.util.Set;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.ast.statements.Statement;
import org.eclipse.dltk.core.CompletionRequestor;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.tcl.ast.TclStatement;
import org.eclipse.dltk.tcl.core.extensions.ICompletionExtension;
import org.eclipse.dltk.tcl.internal.core.codeassist.TclCompletionEngine;
import org.eclipse.dltk.tcl.internal.core.codeassist.completion.CompletionOnKeywordArgumentOrFunctionArgument;
import org.eclipse.dltk.tcl.internal.core.codeassist.completion.CompletionOnKeywordOrFunction;
import org.eclipse.dltk.tcl.internal.core.codeassist.completion.CompletionOnVariable;
import org.eclipse.dltk.tcl.internal.core.codeassist.completion.TclCompletionParser;

public class TclIruleCompletionExtension implements ICompletionExtension {

    @Override
    public boolean visit (Expression e, TclCompletionParser parser, int position) {
        //System.out.println(".core.text.TclIruleCompletionExtension.visit.Expression:: " + e);
        return false;
    }

    @Override
    public boolean visit (Statement s, TclCompletionParser parser, int position) {
        //System.out.println(".core.text.TclIruleCompletionExtension.visit.Statement:: " + s);
        return false;
    }

    @Override
    public void completeOnKeywordOrFunction (CompletionOnKeywordOrFunction key, ASTNode astNodeParent, TclCompletionEngine engine) {
        //System.out.println(".core.text.TclIruleCompletionExtension.completeOnKeywordOrFunction:: " + key);
        //System.out.println(".core.text.TclIruleCompletionExtension.completeOnKeywordOrFunction:: " + astNodeParent);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void completeOnKeywordArgumentsOne (String name, char[] token, CompletionOnKeywordArgumentOrFunctionArgument arg, Set methodNames, TclStatement st, TclCompletionEngine engine) {
        //System.out.println(".core.text.TclIruleCompletionExtension.completeOnKeywordArgumentsOne:: " + name);
        if (name.equals("when")) {
            engine.findKeywords(token, TclIruleSchema.getEventNames(), true);
        }
    }

    @Override
    public void setRequestor (CompletionRequestor requestor) {
    }

    @Override
    public void completeOnVariables (CompletionOnVariable astNode, TclCompletionEngine engine) {
        //System.out.println(".core.text.TclIruleCompletionExtension.completeOnVariables:: " + astNode);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean modelFilter (Set completions, IModelElement modelElement) {
        return false;
    }
}
