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
package com.f5.irule.model;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * {@link ModelFile} for Tcl ILX resources.<br>
 * Its {@link ModelObject #iControlRestPatch} method<br>
 * Synchronously upload the ILX file to the Big-IP.
 */
public class ILXRuleModelFile extends ILXModelFile {

    private static Logger logger = Logger.getLogger(ILXRuleModelFile.class);

    public ILXRuleModelFile(String name, BigIPConnection conn, String partition, Type type, IPath filePath) {
        super(name, conn, partition, type, filePath);
    }

    @Override
    public IStatus iControlRestPatch(RequestCompletion completion) {
        logger.debug("Write ILX " + this);
        IPath fullPath = getFullPath();
        IPath location = getLocation();
        RestFramework.getInstance().writeILXResource(getConnection(), fullPath, location, completion);
        return Status.OK_STATUS;
    }

    public static void processIlxRule(BigIPConnection conn, String partition, String name, IPath path, ModelParent parent) {
        if (name == null) {
            logger.warn("Missing name. Can't process ILX Rule");
            return;
        }
        IPath filePath = path.append(name + "." + RuleProvider.TCL);
        ModelObject existingChild = parent.getChild(name);
        ModelFile rule = new ILXRuleModelFile(name, conn, partition, ModelObject.Type.ILX_RULE, filePath);
        if (existingChild != null) {
            logger.debug("Replacing " + existingChild + " with " + rule + " in " + parent);
            parent.removeChild(existingChild);
        }
        parent.addChild(rule);
        
    }

}
