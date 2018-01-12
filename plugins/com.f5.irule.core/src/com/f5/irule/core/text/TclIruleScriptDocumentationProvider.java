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

import java.io.Reader;
import java.io.StringReader;

import org.eclipse.dltk.core.IMember;
import org.eclipse.dltk.tcl.internal.ui.documentation.ScriptDocumentationProvider;
import org.eclipse.dltk.ui.documentation.IScriptDocumentationProvider;

@SuppressWarnings("restriction")
public class TclIruleScriptDocumentationProvider extends ScriptDocumentationProvider implements IScriptDocumentationProvider {

    @Override
    public Reader getInfo (IMember element, boolean lookIntoParents, boolean lookIntoExternal) {
        //System.out.println(".core.text.TclIruleScriptDocumentationProvider.getInfo.IMember:: " + element);
        return null;
    }

    @Override
    public Reader getInfo (String content) {
        //System.out.println(".core.text.TclIruleScriptDocumentationProvider.getInfo.String:: " + content);

        String description = TclIruleSchema.getDescription(content);
        if (description != null){
            StringBuilder text = new StringBuilder();
            
            // Command/Event name
            text.append("<h1>" + content + "</h1>");
            
            // Description
            String withBreaks = description.replace("\n", "<br>");
            text.append("<p>" + withBreaks + "</p>");
            
            // Examples (with preserved formatting)
            String examples = TclIruleSchema.getExamples(content);
            if (examples != null) {
                text.append("<h2>Examples</h2>");
                text.append("<pre>" + examples + "</pre>");
            }
            
            if (text.length() > 0) {
                return new StringReader(text.toString());
            }
        }       

        return null;
    }
}
