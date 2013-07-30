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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Zip;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Hans Dockter
 * @author Yves Zoundi
 */
public class InstallerTest {
        private File testDir = new File("target/testData/InstallerTest-" + System.currentTimeMillis());
        private Installer install;
        private File distributionDir;
        private File zipStore;
        private File antHomeDir;
        private File zipDestination;
        private WrapperConfiguration configuration = new WrapperConfiguration();
        private Downloader download;
        private PathAssembler pathAssembler;
        private PathAssembler.LocalDistribution localDistribution;

        @Before
        public void setup() throws Exception {
                testDir.mkdirs();
                configuration.setZipBase(PathAssembler.PROJECT_STRING);
                configuration.setZipPath("someZipPath");
                configuration.setDistributionBase(PathAssembler.ANT_USER_HOME_STRING);
                configuration.setDistributionPath("someDistPath");
                configuration.setDistribution(new URI("http://server/ant-0.9.zip"));
                configuration.setAlwaysDownload(false);
                configuration.setAlwaysUnpack(false);
                distributionDir = new File(testDir, "someDistPath");
                antHomeDir = new File(distributionDir, "ant-0.9");
                zipStore = new File(testDir, "zips");
                zipDestination = new File(zipStore, "ant-0.9.zip");

                download = mock(Downloader.class);
                pathAssembler = mock(PathAssembler.class);
                localDistribution = mock(PathAssembler.LocalDistribution.class);

                when(localDistribution.getZipFile()).thenReturn(zipDestination);
                when(localDistribution.getDistributionDir()).thenReturn(distributionDir);
                when(pathAssembler.getDistribution(configuration)).thenReturn(localDistribution);

                install = new Installer(download, pathAssembler);
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

        private void createTestZip(File zipDestination) throws Exception {
                File explodedZipDir = new File(testDir, "explodedZip");
                explodedZipDir.mkdirs();
                zipDestination.getParentFile().mkdirs();
                File antScript = new File(explodedZipDir, "ant-0.9/bin/ant");
                antScript.getParentFile().mkdirs();
                IOUtils.writeTextToFile("something", antScript);

                zipTo(explodedZipDir, zipDestination);
        }

        public void testCreateDist() throws Exception {
                File homeDir = install.createDist(configuration);

                Assert.assertEquals(antHomeDir, homeDir);
                Assert.assertTrue(homeDir.isDirectory());
                Assert.assertTrue(new File(homeDir, "bin/ant").exists());
                Assert.assertTrue(zipDestination.exists());

                Assert.assertEquals(localDistribution, pathAssembler.getDistribution(configuration));
                Assert.assertEquals(distributionDir, localDistribution.getDistributionDir());
                Assert.assertEquals(zipDestination, localDistribution.getZipFile());

                // download.download(new URI("http://some/test"),
                // distributionDir);
                // verify(download).download(new URI("http://some/test"),
                // distributionDir);
        }

        @Test
        public void testCreateDistWithExistingDistribution() throws Exception {
                IOUtils.touchFile(zipDestination);
                antHomeDir.mkdirs();
                File someFile = new File(antHomeDir, "some-file");
                IOUtils.touchFile(someFile);

                File homeDir = install.createDist(configuration);

                Assert.assertEquals(antHomeDir, homeDir);
                Assert.assertTrue(antHomeDir.isDirectory());
                Assert.assertTrue(new File(homeDir, "some-file").exists());
                Assert.assertTrue(zipDestination.exists());

                Assert.assertEquals(localDistribution, pathAssembler.getDistribution(configuration));
                Assert.assertEquals(distributionDir, localDistribution.getDistributionDir());
                Assert.assertEquals(zipDestination, localDistribution.getZipFile());
        }

        @Test
        public void testCreateDistWithExistingDistAndZipAndAlwaysUnpackTrue() throws Exception {
                createTestZip(zipDestination);
                antHomeDir.mkdirs();
                File garbage = new File(antHomeDir, "garbage");
                IOUtils.touchFile(garbage);
                configuration.setAlwaysUnpack(true);

                File homeDir = install.createDist(configuration);

                Assert.assertEquals(antHomeDir, homeDir);
                Assert.assertTrue(antHomeDir.isDirectory());
                Assert.assertFalse(new File(homeDir, "garbage").exists());
                Assert.assertTrue(zipDestination.exists());

                Assert.assertEquals(localDistribution, pathAssembler.getDistribution(configuration));
                Assert.assertEquals(distributionDir, localDistribution.getDistributionDir());
                Assert.assertEquals(zipDestination, localDistribution.getZipFile());
        }

        @Test
        public void testCreateDistWithExistingZipAndDistAndAlwaysDownloadTrue() throws Exception {
                createTestZip(zipDestination);
                File garbage = new File(antHomeDir, "garbage");
                IOUtils.touchFile(garbage);
                configuration.setAlwaysUnpack(true);

                File homeDir = install.createDist(configuration);

                Assert.assertEquals(antHomeDir, homeDir);
                Assert.assertTrue(antHomeDir.isDirectory());
                Assert.assertTrue(new File(homeDir, "bin/ant").exists());
                Assert.assertFalse(new File(homeDir, "garbage").exists());
                Assert.assertTrue(zipDestination.exists());

                Assert.assertEquals(localDistribution, pathAssembler.getDistribution(configuration));
                Assert.assertEquals(distributionDir, localDistribution.getDistributionDir());
                Assert.assertEquals(zipDestination, localDistribution.getZipFile());

                // download.download(new URI("http://some/test"),
                // distributionDir);
                // verify(download).download(new URI("http://some/test"),
                // distributionDir);
        }

        public void zipTo(File directoryToZip, File zipFile) {
                Zip zip = new Zip();
                zip.setBasedir(directoryToZip);
                zip.setDestFile(zipFile);
                zip.setProject(new Project());

                Zip.WhenEmpty whenEmpty = new Zip.WhenEmpty();
                whenEmpty.setValue("create");
                zip.setWhenempty(whenEmpty);
                zip.execute();
        }

}