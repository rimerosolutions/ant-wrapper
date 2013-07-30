/*
 * Copyright 2007-2008 the original author or authors.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Properties;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Previous authors
 * @author Yves Zoundi
 */
public class WrapperExecutorTest {

        private Installer install;
        private BootstrapMainStarter start;
        private File propertiesFile;
        private Properties properties = new Properties();
        private File testDir = new File("target/testData/WrapperExecutorTest-" + System.currentTimeMillis());
        private File mockInstallDir = new File(testDir, "mock-dir");

        @Before
        public void initialize() {
                try {
                        install = mock(Installer.class);
                        when(install.createDist(Mockito.any(WrapperConfiguration.class))).thenReturn(mockInstallDir);
                        start = mock(BootstrapMainStarter.class);

                        testDir.mkdirs();
                        propertiesFile = new File(testDir, WrapperExecutor.WRAPPER_PROPERTIES_PATH);

                        properties.put("distributionUrl", "http://server/test/ant.zip");
                        properties.put("distributionBase", "testDistBase");
                        properties.put("distributionPath", "testDistPath");
                        properties.put("zipStoreBase", "testZipBase");
                        properties.put("zipStorePath", "testZipPath");

                        writePropertiesFile(properties, propertiesFile, "header");
                }
                catch (Exception e) {
                        throw new RuntimeException(e.getMessage());
                }
        }
        
        @After
        public void cleanupTestDir() {
                Delete delete = new Delete();
                delete.setProject(new Project());
                delete.setDir(testDir);
                delete.setQuiet(true);
                delete.setIncludeEmptyDirs(true);
                delete.execute();
        }

        @Test
        public void loadWrapperMetadataFromFile() throws Exception {
                WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile(propertiesFile, System.out);

                Assert.assertEquals(new URI("http://server/test/ant.zip"), wrapper.getDistribution());
                Assert.assertEquals(new URI("http://server/test/ant.zip"), wrapper.getConfiguration().getDistribution());
                Assert.assertEquals("testDistBase", wrapper.getConfiguration().getDistributionBase());
                Assert.assertEquals("testDistPath", wrapper.getConfiguration().getDistributionPath());
                Assert.assertEquals("testZipBase", wrapper.getConfiguration().getZipBase());
                Assert.assertEquals("testZipPath", wrapper.getConfiguration().getZipPath());
        }

        @Test
        public void loadWrapperMetadataFromDirectory() throws Exception {
                WrapperExecutor wrapper = WrapperExecutor.forProjectDirectory(testDir, System.out);

                System.out.println("\nFolder:" + testDir.getAbsolutePath());
                for (String s : testDir.list()) {
                        System.out.println ("file -> " + s);
                }


                Assert.assertEquals(new URI("http://server/test/ant.zip"), wrapper.getDistribution());
                Assert.assertEquals(new URI("http://server/test/ant.zip"), wrapper.getConfiguration().getDistribution());
                Assert.assertEquals("testDistBase", wrapper.getConfiguration().getDistributionBase());
                Assert.assertEquals("testDistPath", wrapper.getConfiguration().getDistributionPath());
                Assert.assertEquals("testZipBase", wrapper.getConfiguration().getZipBase());
                Assert.assertEquals("testZipPath", wrapper.getConfiguration().getZipPath());
        }

        @Test
        public void useDefaultMetadataNoPropertiesFile() throws Exception {
                WrapperExecutor wrapper = WrapperExecutor.forProjectDirectory(new File(testDir, "unknown"), System.out);

                Assert.assertNull(wrapper.getDistribution());
                Assert.assertNull(wrapper.getConfiguration().getDistribution());
                Assert.assertEquals(PathAssembler.ANT_USER_HOME_STRING, wrapper.getConfiguration().getDistributionBase());
                Assert.assertEquals(Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getDistributionPath());
                Assert.assertEquals(PathAssembler.ANT_USER_HOME_STRING, wrapper.getConfiguration().getZipBase());
                Assert.assertEquals(Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getZipPath());
        }

        @Test
        public void propertiesFileOnlyContainsDistURL() throws Exception {

                properties = new Properties();
                properties.put("distributionUrl", "http://server/test/ant.zip");
                writePropertiesFile(properties, propertiesFile, "header");

                WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile(propertiesFile, System.out);

                Assert.assertEquals(new URI("http://server/test/ant.zip"), wrapper.getDistribution());
                Assert.assertEquals(new URI("http://server/test/ant.zip"), wrapper.getConfiguration().getDistribution());
                Assert.assertEquals(PathAssembler.ANT_USER_HOME_STRING, wrapper.getConfiguration().getDistributionBase());
                Assert.assertEquals(Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getDistributionPath());
                Assert.assertEquals(PathAssembler.ANT_USER_HOME_STRING, wrapper.getConfiguration().getZipBase());
                Assert.assertEquals(Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getZipPath());
        }

        @Test
        public void executeInstallAndLaunch() throws Exception {
                WrapperExecutor wrapper = WrapperExecutor.forProjectDirectory(propertiesFile, System.out);

                wrapper.execute(new String[] { "arg" }, install, start);
                verify(install).createDist(Mockito.any(WrapperConfiguration.class));
                verify(start).start(new String[] { "arg" }, mockInstallDir);
        }

        @Test()
        public void failWhenDistNotSetInProperties() throws Exception {
                properties = new Properties();
                writePropertiesFile(properties, propertiesFile, "header");

                try {
                        WrapperExecutor.forWrapperPropertiesFile(propertiesFile, System.out);
                        Assert.fail("Expected RuntimeException");
                }
                catch (RuntimeException e) {
                        Assert.assertEquals("Could not load wrapper properties from '" + propertiesFile + "'.", e.getMessage());
                        Assert.assertEquals("No value with key 'distributionUrl' specified in wrapper properties file '"
                                            + propertiesFile + "'.", e.getCause().getMessage());
                }

        }

        @Test
        public void failWhenPropertiesFileDoesNotExist() {
                propertiesFile = new File(testDir, "unknown.properties");

                try {
                        WrapperExecutor.forWrapperPropertiesFile(propertiesFile, System.out);
                        Assert.fail("Expected RuntimeException");
                }
                catch (RuntimeException e) {
                        Assert.assertEquals("Wrapper properties file '" + propertiesFile + "' does not exist.", e.getMessage());
                }
        }

        @Test
        public void testRelativeDistUrl()
                throws Exception {

                properties = new Properties();
                properties.put("distributionUrl", "some/relative/url/to/bin.zip");
                writePropertiesFile(properties, propertiesFile, "header");

                WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile(propertiesFile, System.out);
                Assert.assertNotEquals("some/relative/url/to/bin.zip", wrapper.getDistribution().getSchemeSpecificPart());
                Assert.assertTrue(wrapper.getDistribution().getSchemeSpecificPart().endsWith("some/relative/url/to/bin.zip"));
        }

        private void writePropertiesFile(Properties properties, File propertiesFile, String message) throws Exception {
                propertiesFile.getParentFile().mkdirs();
                OutputStream outStream = null;

                try {
                        outStream = new FileOutputStream(propertiesFile);
                        properties.store(outStream, message);
                }
                finally {
                        if (outStream != null) {
                                try {
                                        outStream.close();
                                }
                                catch (IOException ioe) {
                                        // ignore
                                }
                        }
                }
        }
}
