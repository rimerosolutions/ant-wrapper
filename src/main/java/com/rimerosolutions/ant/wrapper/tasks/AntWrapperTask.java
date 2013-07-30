/*
 * Copyright 2013 Rimero Solutions
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
package com.rimerosolutions.ant.wrapper.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;

/**
 * Ant wrapper task.
 * 
 * <strong>setDescription is ignored.</strong>
 * 
 * @author Yves Zoundi
 */
public class AntWrapperTask extends Task {
        static final String TASK_DESCRIPTION = "Generate Ant Command Line Wrapper";
        static final int BUFFER_SIZE = 1024;
        static final String DEFAULT_BASE_DISTRIBUTION_URL = "http://archive.apache.org/dist/ant/binaries";
        static final String ANT_VERSION_FILE_LOCATION = "/org/apache/tools/ant/version.txt";
        static final String ANT_VERSION_PROPERTY = "VERSION";
        static final String FILE_URL_SCHEME = "file";
        static final String ANT_WRAPPER_PROPERTIES_FILE_COMMENTS = "Ant download properties";

        public static final String ANT_BIN_FILENAME_TEMPLATE = "apache-ant-%s-bin.zip";
        public static final String WRAPPER_ROOT_FOLDER_NAME = "wrapper";
        public static final String WRAPPER_PROPERTIES_FILE_NAME = "wrapper.properties";
        public static final String WRAPPER_JAR_FILE_NAME = "wrapper.jar";
        public static final String DISTRIBUTION_URL_PROPERTY = "distributionUrl";

        public static final String LAUNCHER_WINDOWS_FILE_NAME = "antw.bat";
        public static final String LAUNCHER_WINDOWSCMD_FILE_NAME = "antw.cmd";
        public static final String LCP_WINDOWS_FILE_NAME = "lcp.bat";
        public static final String LAUNCHER_UNIX_FILE_NAME = "antw";

        static final String RESOURCES_LOCATION = "com/rimerosolutions/ant/wrapper";
        static final String[] LAUNCHER_RESOURCES = { 
                LAUNCHER_WINDOWS_FILE_NAME, 
                LAUNCHER_WINDOWSCMD_FILE_NAME, 
                LCP_WINDOWS_FILE_NAME,
                LAUNCHER_UNIX_FILE_NAME
        };

        /** Distribution Url optional parameter */
        private String baseDistributionUrl;

        /** Optional Ant version to use for the wrapper */
        private String antVersion;

        /**
         * Sets the Ant version to use
         * 
         * @param antVersion The Ant version to use (Default auto-detected)
         */
        public void setAntVersion(String antVersion) {
                this.antVersion = antVersion;
        }

        /** Sets the root URL containing the Apache Ant binaries in zip format */
        public void setBaseDistributionUrl(String baseDistributionUrl) {
                this.baseDistributionUrl = baseDistributionUrl;
        }

        /** {@inheritDoc} */
        @Override
        public String getDescription() {
                return TASK_DESCRIPTION;
        }

        /** {@inheritDoc} */
        @Override
        public void execute() throws BuildException {
                copyScripts();
                writeWrapperPropertiesFile();
        }

        private void copyScripts() {
                final ClassLoader classLoader = getClass().getClassLoader();

                for (String launcherFileName : LAUNCHER_RESOURCES) {
                        URL launcherUrl = classLoader.getResource(RESOURCES_LOCATION + "/" + launcherFileName);
                        File launcherFile = new File(getProject().getBaseDir(), launcherFileName);

                        try {
                                copyResourceToFile(new FileResource(new File(launcherUrl.getFile())), launcherFile);
                        } catch (Exception e) {
                                throw new BuildException(e);
                        }

                        if (!launcherFile.setExecutable(true)) {
                                log("Could not set executable flag on file: " + launcherFile.getAbsolutePath());
                        }
                }
        }

        private void writeWrapperPropertiesFile() {
                File wrapperSupportDir = new File(getProject().getBaseDir(), WRAPPER_ROOT_FOLDER_NAME);
                ensureWrapperSupportDirExists(wrapperSupportDir);

                copyResourceToFile(new FileResource(findWrapperJarFile()), new File(wrapperSupportDir, WRAPPER_JAR_FILE_NAME));

                FileOutputStream propertiesOutputStream = null;

                try {
                        File file = new File(wrapperSupportDir, WRAPPER_PROPERTIES_FILE_NAME);
                        Properties props = new Properties();
                        props.put(DISTRIBUTION_URL_PROPERTY, buildDistributionUrl());
                        propertiesOutputStream = new FileOutputStream(file);
                        props.store(propertiesOutputStream, ANT_WRAPPER_PROPERTIES_FILE_COMMENTS);
                } catch (IOException ioe) {
                        throw new BuildException("Unable to store wrapper properties", ioe);
                } finally {
                        FileUtils.close(propertiesOutputStream);
                }
        }

        private void ensureWrapperSupportDirExists(File wrapperSupportDir) {
                if (!wrapperSupportDir.exists()) {
                        wrapperSupportDir.mkdirs();
                }
        }

        private String buildDistributionUrl() {
                StringBuilder binaryFileLocation = new StringBuilder(248);

                if (baseDistributionUrl == null) {
                        baseDistributionUrl = DEFAULT_BASE_DISTRIBUTION_URL;
                }

                binaryFileLocation.append(baseDistributionUrl);

                if (!baseDistributionUrl.endsWith("/")) {
                        binaryFileLocation.append('/');
                }

                binaryFileLocation.append(ANT_BIN_FILENAME_TEMPLATE);

                if (antVersion == null || antVersion.trim().length() == 0) {
                        antVersion = getAntRuntimeVersion();
                }

                return String.format(binaryFileLocation.toString(), antVersion);
        }

        protected File findWrapperJarFile() {
                URI location;

                try {
                        location = AntWrapperTask.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                } catch (URISyntaxException e) {
                        throw new BuildException(e);
                }

                if (!location.getScheme().equals(FILE_URL_SCHEME)) {
                        throw new BuildException(String.format("Cannot determine classpath for wrapper Jar from codebase '%s'.", location));
                }

                return new File(location.getPath());
        }

        private static void copyResourceToFile(Resource r, File f) {
                Copy copy = new Copy();
                copy.setProject(new Project());
                copy.add(r);
                copy.setTofile(f);
                copy.execute();
        }

        private static String getAntRuntimeVersion() throws BuildException {
                InputStream in = null;

                try {
                        Properties props = new Properties();
                        in = AntWrapperTask.class.getResourceAsStream(ANT_VERSION_FILE_LOCATION);
                        props.load(in);
                        
                        return props.getProperty(ANT_VERSION_PROPERTY);
                } catch (IOException ioe) {
                        throw new BuildException("Could not load the version information:" + ioe.getMessage());
                } catch (NullPointerException npe) {
                        throw new BuildException("Could not load the Apache Ant version information.");
                } finally {
                        if (in != null) {
                                FileUtils.close(in);
                        }
                }

        }
}
