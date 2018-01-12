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

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.FactoryConfigurationError;

import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    private static BundleContext context;

    static BundleContext getContext() {
        return context;
    }
    
    public void start(BundleContext bundleContext) throws Exception {
        Activator.context = bundleContext;
        configureLog4J();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        Activator.context = null;
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
}
