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
package com.f5.irule.core.ui;

import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.internal.ui.text.hover.DocumentationHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import com.f5.irule.core.TclIruleNature;
import com.f5.irule.core.text.TclIruleWordFinder;

@SuppressWarnings("restriction")
public class TclIruleTextHover extends DocumentationHover {

    @Override
    public String getHoverInfo (ITextViewer viewer, IRegion hoverRegion) {
        IDLTKLanguageToolkit toolkit = DLTKLanguageManager.getLanguageToolkit(getEditorInputModelElement());
        if (toolkit == null) {
            return null;
        }
        
        String nature = toolkit.getNatureId();
        if (nature == null || nature != TclIruleNature.NATURE_ID) {
            return null;
        }
        
        IRegion region = getHoverRegion(viewer, hoverRegion.getOffset());
        if (region != null) {
            return super.getHoverInfo(viewer, region);

        } else {
            return null;
        }
    }

    @Override
    public IRegion getHoverRegion (ITextViewer viewer, int offset) {
        return TclIruleWordFinder.find(viewer.getDocument(), offset);
    }
}
