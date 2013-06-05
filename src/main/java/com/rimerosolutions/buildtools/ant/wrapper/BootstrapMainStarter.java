/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rimerosolutions.buildtools.ant.wrapper;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hans Dockter
 */
public class BootstrapMainStarter {
        public void start(String[] args, File antHome) throws Exception {
                File[] antJars = findBootstrapJars(antHome);

                URL[] jarUrls = new URL[antJars.length];
                for (int i = 0; i < antJars.length; i++) {
                        jarUrls[i] = antJars[i].toURI().toURL();
                }

                URLClassLoader contextClassLoader = new URLClassLoader(jarUrls, ClassLoader.getSystemClassLoader().getParent());
                Thread.currentThread().setContextClassLoader(contextClassLoader);

                Class<?> mainClass = contextClassLoader.loadClass("org.apache.tools.ant.Main");
                mainClass.getMethod("main", String[].class).invoke(null, new Object[] { args });
        }

        private File[] findBootstrapJars(File antHome) {
                List<File> bootstrapJars = new ArrayList<File>();

                for (File file : new File(antHome, "lib").listFiles()) {
                        if (file.getName().endsWith(".jar")) {
                                bootstrapJars.add(file);
                        }
                }

                String javaHome = System.getenv("JAVA_HOME");
                
                if (javaHome != null) {
                        File toolsJar = new File(javaHome, "lib/tools.jar");
                        File toolsJarOsx = new File(javaHome, "lib/classes.jar");
                        if (toolsJar.exists()) {
                                bootstrapJars.add(toolsJar);
                        }

                        if (toolsJarOsx.exists()) {
                                bootstrapJars.add(toolsJarOsx);
                        }
                }

                return bootstrapJars.toArray(new File[bootstrapJars.size()]);

        }
}
