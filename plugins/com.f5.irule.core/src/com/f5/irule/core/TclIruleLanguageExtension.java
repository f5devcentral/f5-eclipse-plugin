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
package com.f5.irule.core;

import org.eclipse.dltk.tcl.core.extensions.ICompletionExtension;
import org.eclipse.dltk.tcl.core.extensions.IMatchLocatorExtension;
import org.eclipse.dltk.tcl.core.extensions.IMixinBuildVisitorExtension;
import org.eclipse.dltk.tcl.core.extensions.ISelectionExtension;
import org.eclipse.dltk.tcl.core.extensions.ISourceElementRequestVisitorExtension;
import org.eclipse.dltk.tcl.core.extensions.ITclLanguageExtension;

import com.f5.irule.core.text.TclIruleCompletionExtension;

public class TclIruleLanguageExtension implements ITclLanguageExtension {

    public static String NAME = "TclIrule";

    public TclIruleLanguageExtension () {
    }

    @Override
    public ICompletionExtension createCompletionExtension () {
        //System.out.println(".core.TclIruleLanguageExtension.createCompletionExtension");
        return new TclIruleCompletionExtension();
    }

    @Override
    public IMatchLocatorExtension createMatchLocatorExtension () {
        //System.out.println(".core.TclIruleLanguageExtension.createMatchLocatorExtension");
        return null;
    }

    @Override
    public IMixinBuildVisitorExtension createMixinBuildVisitorExtension () {
        //System.out.println(".core.TclIruleLanguageExtension.createMixinBuildVisitorExtension");
        return null;
    }

    @Override
    public ISelectionExtension createSelectionExtension () {
        //System.out.println(".core.TclIruleLanguageExtension.createSelectionExtension");
        return null;
    }

    @Override
    public ISourceElementRequestVisitorExtension createSourceElementRequestVisitorExtension () {
        //System.out.println(".core.TclIruleLanguageExtension.createSourceElementRequestVisitorExtension");
        return null;
    }

    @Override
    public String getName () {
        return NAME;
    }
}
