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

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;

import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.tcl.core.ITclKeywords;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    //private static Logger logger = Logger.getLogger(Activator.class);

    // The plug-in ID
    public static final String PLUGIN_ID = "com.f5.irule.ui"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    private IWorkbenchPage page;
    
    /**
     * The constructor
     */
    public Activator() {
        page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }

    private static final String DLTK_TCL_KEYWORDS_EXTENSION_POINT = "org.eclipse.dltk.tcl.core.tclkeywords";
    private static final String CLASS = "class";
    private static final String SABOTAGE_WORD = "$$$$$$$$$$$";

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {

        configureLog4J();
        processTclkeywords();	    	
        super.start(context);
        plugin = this;

        ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {
            @Override
            public void resourceChanged(IResourceChangeEvent event) {
                IResourceDelta change = event.getDelta();                
                //logger.debug("Something changed in " + change.getFullPath());
                if (change != null) {
                    try {
                        DeltaModelFileVisitor visitor = new DeltaModelFileVisitor(page);
                        change.accept(visitor);
                        visitor.processLastChange();
                    } catch (CoreException e) {
                        e.printStackTrace();
                    }                    
                }
            }
        });
    }

    /**
     * Use {@link DOMConfigurator} to configure log4j from a log4j.xml configuration file.
     */
    private void configureLog4J() throws IOException, FactoryConfigurationError {
        URL confURL = Activator.class.getClassLoader().getResource("log4j.xml");
        if (confURL != null) {
            URL fileURL = FileLocator.toFileURL(confURL);
            if (fileURL != null) {
                String file = fileURL.getFile();
                DOMConfigurator.configureAndWatch(file, 5000);
            } 
        }
    }

    private static void processTclkeywords() {
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        IConfigurationElement[] cfg = extensionRegistry.getConfigurationElementsFor(DLTK_TCL_KEYWORDS_EXTENSION_POINT);
        for (int i = 0; i < cfg.length; i++) {
            IConfigurationElement configurationElement = cfg[i];
            try {
                ITclKeywords keywords = (ITclKeywords) configurationElement.createExecutableExtension(CLASS);
                sabotageDisabledCommands(keywords);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }	
        }
    }

    private static void sabotageDisabledCommands(ITclKeywords keywords) {
        if (keywords != null) {
            for (int q = 0; q < ITclKeywords.END_INDEX; ++q) {
                String[] kw2 = keywords.getKeywords(q);
                for (int i1 = 0; i1 < kw2.length; i1++) {
                    String key = kw2[i1];
                    List<String> disabledTclCommands = DisabledTclCommands.getList();// DISABLED_TCL_COMMANDS;
                    if (disabledTclCommands.contains(key)) {
                        kw2[i1] = SABOTAGE_WORD;
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path
     *
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }
}
