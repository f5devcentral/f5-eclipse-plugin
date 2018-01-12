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
package com.f5.irule.ui.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IViewLayout;

public class IrulePerspective implements IPerspectiveFactory {

    public static final String PERSPECTIVE_ID = "com.f5.irule.ui.perspective.IrulePerspective";

    public static final String ID_IRULE_EXPLORER = "com.f5.irule.ui.views.IruleView";

    protected void addViews(IPageLayout layout) {
        String editorArea = layout.getEditorArea();

        IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, (float) 0.25, editorArea); //$NON-NLS-1$
        left.addView(ID_IRULE_EXPLORER);

        IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.74f, editorArea);
        bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
        bottom.addView("org.eclipse.pde.runtime.LogView");
        bottom.addPlaceholder(IPageLayout.ID_OUTLINE);
        bottom.addPlaceholder(IPageLayout.ID_PROGRESS_VIEW);
        bottom.addPlaceholder(IPageLayout.ID_TASK_LIST);
    }

    protected void addViewShortcuts(IPageLayout layout) {
        layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
        layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
        layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
        layout.addShowViewShortcut(ID_IRULE_EXPLORER);
    }

    @Override
    public void createInitialLayout(IPageLayout layout) {
        addViewShortcuts(layout);
        addViews(layout);
        
        // Make the Explorer tab non-closable so that the user doesn't accidentally end up with
        // an "unusable" perspective.
        IViewLayout viewLayout = layout.getViewLayout(ID_IRULE_EXPLORER);
        viewLayout.setCloseable(false);
    }
}
