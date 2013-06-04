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
package com.rimerosolutions.buildtools.ant.wrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Ant wrapper task.
 *
 * <strong>setDescription is ignored.</strong>
 *
 * @author Yves Zoundi
 */
public final class AntWrapperTask extends Task {
        private static String antVersion;
        static final String TASK_DESCRIPTION = "Generate Ant Command Line Wrapper";
        static final int BUFFER_SIZE = 1024;
        static final String DIST_URL_TEMPLATE = "http://archive.apache.org/dist/ant/binaries/apache-ant-%s-bin.zip";
        static final String ANT_VERSION_FILE_LOCATION = "/org/apache/tools/ant/version.txt";
        static final String ANT_VERSION_PROPERTY = "VERSION";
        static final String FILE_URL_SCHEME = "file";
        static final String WRAPPER_PROPERTIES_FILE_NAME = "wrapper.properties";
        static final String WRAPPER_ROOT_FOLDER_NAME = "wrapper";
        static final String WRAPPER_JAR_FILE_NAME = "wrapper.jar";
        static final String ANT_WRAPPER_PROPERTIES_FILE_COMMENTS = "Ant download properties";
        static final String DISTRIBUTION_URL_PROPERTY = "distributionUrl";

        static final String LAUNCHER_WINDOWS_FILE_NAME = "antw.bat";
        static final String LAUNCHER_WINDOWSCMD_FILE_NAME = "antw.cmd";
        static final String LCP_WINDOWS_FILE_NAME = "lcp.bat";
        static final String LAUNCHER_UNIX_FILE_NAME = "antw";

        static final String[] LAUNCHER_RESOURCES = {LAUNCHER_WINDOWS_FILE_NAME, LAUNCHER_WINDOWSCMD_FILE_NAME, LCP_WINDOWS_FILE_NAME, LAUNCHER_UNIX_FILE_NAME};

        public String getDescription() {
                return TASK_DESCRIPTION;
        }

        public void execute() {
                copyScripts();
                writeWrapperPropertiesFile();
        }

        private void copyScripts() {
                final ClassLoader classLoader = AntWrapperTask.class.getClassLoader();

                for (String launcherFileName : LAUNCHER_RESOURCES) {
                        InputStream launcherStream = classLoader.getResourceAsStream(launcherFileName);
                        File launcherFile = new File(getProject().getBaseDir(), launcherFileName);

                        try {
                                writeToFile(launcherStream, launcherFile);
                        }
                        catch (Exception e) {
                                throw new RuntimeException(e);
                        }

                        if (!launcherFile.setExecutable(true)) {
                                log("Could not set executable flag on file: " + launcherFile.getAbsolutePath());
                        }
                }
        }

        private void writeWrapperPropertiesFile() {
                File wrapperDestFolder = new File(getProject().getBaseDir(), WRAPPER_ROOT_FOLDER_NAME);
                wrapperDestFolder.mkdirs();

                Properties props = new Properties();
                props.put(DISTRIBUTION_URL_PROPERTY, String.format(DIST_URL_TEMPLATE, getAntVersion()));
                File file = new File(wrapperDestFolder, WRAPPER_PROPERTIES_FILE_NAME);
                FileOutputStream fileOut = null;
                InputStream is = null;

                try {
                        is = new FileInputStream(wrapperJar());
                        writeToFile(is, new File(wrapperDestFolder, WRAPPER_JAR_FILE_NAME));
                        fileOut = new FileOutputStream(file);
                        props.store(fileOut, ANT_WRAPPER_PROPERTIES_FILE_COMMENTS);
                }
                catch (IOException ioe) {
                        throw new RuntimeException("Unable to store wrapper properties", ioe);
                }
                finally {
                        if (fileOut != null) {
                                try {
                                        fileOut.close();
                                }
                                catch (IOException ioe) {
                                        throw new RuntimeException(ioe);
                                }
                        }
                        if (is != null) {
                                try {
                                        fileOut.close();
                                }
                                catch (IOException ioe) {
                                        throw new RuntimeException(ioe);
                                }
                        }
                }
        }

        private static File wrapperJar() {
                URI location;

                try {
                        location = AntWrapperTask.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                }
                catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                }

                if (!location.getScheme().equals(FILE_URL_SCHEME)) {
                        throw new RuntimeException(
                                                   String.format("Cannot determine classpath for wrapper Jar from codebase '%s'.", location));
                }

                return new File(location.getPath());
        }

        private static void writeToFile(InputStream stream, File filePath) throws IOException {
                FileChannel outChannel = null;
                ReadableByteChannel inChannel = null;
                FileOutputStream fos = null;

                try {
                        fos = new FileOutputStream(filePath);
                        outChannel = fos.getChannel();
                        inChannel = Channels.newChannel(stream);
                        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

                        while (inChannel.read(buffer) >= 0 || buffer.position() > 0) {
                                buffer.flip();
                                outChannel.write(buffer);
                                buffer.clear();
                        }
                }
                finally {
                        if (inChannel != null) {
                                inChannel.close();
                        }

                        if (outChannel != null) {
                                outChannel.close();
                        }

                        if (fos != null) {
                                fos.close();
                        }

                        if (stream != null) {
                                stream.close();
                        }
                }
        }

        public static synchronized String getAntVersion() throws BuildException {
                if (antVersion == null) {
                        InputStream in = null;

                        try {
                                Properties props = new Properties();
                                in = AntWrapperTask.class.getResourceAsStream(ANT_VERSION_FILE_LOCATION);
                                props.load(in);
                                antVersion = props.getProperty(ANT_VERSION_PROPERTY);
                        }
                        catch (IOException ioe) {
                                throw new BuildException("Could not load the version information:" + ioe.getMessage());
                        }
                        catch (NullPointerException npe) {
                                throw new BuildException("Could not load the Apache Ant version information.");
                        }
                        finally {
                                if (in != null) {
                                        try {
                                                in.close();
                                        }
                                        catch (IOException ioe) {
                                                throw new BuildException("Unable to close version info stream", ioe);
                                        }
                                }
                        }
                }

                return antVersion;
        }
}
