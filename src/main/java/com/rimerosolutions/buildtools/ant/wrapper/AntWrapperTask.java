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

public class AntWrapperTask extends Task {
        private static String antVersion;
        static final String DIST_URL_TEMPLATE = "http://archive.apache.org/dist/ant/binaries/apache-ant-%s-bin.zip";

        static final String WRAPPER_PROPERTIES_FILE_NAME = "wrapper.properties";
        static final String WRAPPER_ROOT_FOLDER_NAME = "wrapper";
        static final String WRAPPER_JAR_FILE_NAME = "wrapper.jar";
        static final String DISTRIBUTION_URL_PROPERTY = "distributionUrl";

        static final String LAUNCHER_WINDOWS_FILE_NAME = "antw.bat";
        static final String LAUNCHER_UNIX_FILE_NAME = "antw";

        public void execute() {

                final String[] launcherFileNames = { LAUNCHER_WINDOWS_FILE_NAME, LAUNCHER_UNIX_FILE_NAME };
                final ClassLoader classLoader = AntWrapperTask.class.getClassLoader();

                for (String launcherFileName : launcherFileNames) {
                        InputStream mvnLauncherStream = classLoader.getResourceAsStream(launcherFileName);
                        File launcherFile = new File(getProject().getBaseDir(), launcherFileName);
                        try {
                                writeToFile(mvnLauncherStream, launcherFile);
                        }
                        catch (Exception e) {
                                throw new RuntimeException(e);
                        }

                        if (!launcherFile.setExecutable(true)) {
                                log("Could not set executable flag on file: " + launcherFile.getAbsolutePath());
                        }
                }

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
                        props.store(fileOut, "Ant download properties");

                }
                catch (IOException ioe) {

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
                if (!location.getScheme().equals("file")) {
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
                        ByteBuffer buffer = ByteBuffer.allocate(1024);

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
                        try {
                                Properties props = new Properties();
                                InputStream in = AntWrapperTask.class
                                                .getResourceAsStream("/org/apache/tools/ant/version.txt");
                                props.load(in);
                                in.close();
                                antVersion = props.getProperty("VERSION");
                        }
                        catch (IOException ioe) {
                                throw new BuildException("Could not load the version information:" + ioe.getMessage());
                        }
                        catch (NullPointerException npe) {
                                throw new BuildException("Could not load the version information.");
                        }
                }
                return antVersion;
        }
}