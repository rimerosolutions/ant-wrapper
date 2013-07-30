/*
 * Copyright 2007-2009 the original author or authors.
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
package com.rimerosolutions.ant.wrapper;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Hello world!
 *
 */
public class AntWrapperMain {

        public static final String DEFAULT_ANT_USER_HOME = System.getProperty("user.home") + "/.ant";

        public static void main(String[] args) throws Exception {
                File defaultAntUserHomeDir = new File(DEFAULT_ANT_USER_HOME);

                if (!defaultAntUserHomeDir.exists()) {
                        defaultAntUserHomeDir.mkdirs();
                }

                File wrapperJar = wrapperJar();
                File propertiesFile = wrapperProperties(wrapperJar);
                File rootDir = rootDir(wrapperJar);

                addSystemProperties(rootDir);

                WrapperExecutor wrapperExecutor = WrapperExecutor.forWrapperPropertiesFile(propertiesFile, System.out);
                wrapperExecutor.execute(args, new Installer(new DefaultDownloader("antw", wrapperVersion()),
                                                            new PathAssembler(antUserHome())), new BootstrapMainStarter());
        }


        private static void addSystemProperties(File rootDir) {
                System.getProperties().putAll(SystemPropertiesHandler.getSystemProperties(new File(antUserHome(),
                                                                                                   "ant.properties")));
                System.getProperties().putAll(SystemPropertiesHandler.getSystemProperties(new File(rootDir,
                                                                                                   "ant.properties")));
        }

        private static File rootDir(File wrapperJar) {
                return wrapperJar.getParentFile().getParentFile().getParentFile();
        }

        private static File wrapperProperties(File wrapperJar) {
                return new File(wrapperJar.getParent(), wrapperJar.getName().replaceFirst("\\.jar$", ".properties"));
        }

        private static File wrapperJar() {
                URI location;

                try {
                        location = AntWrapperMain.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                }
                catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                }

                if (!location.getScheme().equals("file")) {
                        throw new RuntimeException(
                                                   String.format("Cannot determine classpath for wrapper Jar from codebase '%s'.",
                                                                 location));
                }

                return new File(location.getPath());
        }

        static String wrapperVersion() {
                return "1.0";
        }

        private static File antUserHome() {
                String antUserHome = System.getProperty(PathAssembler.ANT_USER_HOME_STRING);

                if (antUserHome != null) {
                        return new File(antUserHome);
                }
                else if ((antUserHome = System.getenv(PathAssembler.ANT_USER_HOME_STRING)) != null) {
                        return new File(antUserHome);
                }
                else {
                        return new File(DEFAULT_ANT_USER_HOME);
                }
        }
}
