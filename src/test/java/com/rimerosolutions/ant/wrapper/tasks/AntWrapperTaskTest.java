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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rimerosolutions.ant.wrapper.IOUtils;
import com.rimerosolutions.ant.wrapper.tasks.AntWrapperTask;

/**
 * @author Yves Zoundi
 */
public class AntWrapperTaskTest {
        
        private File testDir = new File("target/testData/wrapper-" + System.currentTimeMillis());
        private final String dummyAntVersion = "0.0.1";
        private final String dummyDistributionUrl = "http://serverbase";
        private final String dummyWrapperJarFilename = "ant-wrapper.jar";

        private void executeWrapperTask() {
                Project project = new Project();
                project.setBaseDir(testDir);
                
                AntWrapperTask task = new AntWrapperTask() {
                        protected File findWrapperJarFile() {
                                return new File(testDir, dummyWrapperJarFilename);
                        }
                };
                task.setProject(project);
                task.setAntVersion(dummyAntVersion);
                task.setBaseDistributionUrl(dummyDistributionUrl);
                task.execute();
        }
        
        private String readDistributionUrlFromWrapperProperties(File propertiesFile) throws IOException {
                Properties props = new Properties();
                InputStream is = null;
                
                try {
                        is = new FileInputStream(propertiesFile);
                        props.load(is);
                        
                        return props.getProperty(AntWrapperTask.DISTRIBUTION_URL_PROPERTY);
                }
                finally {
                        if (is != null) {
                                FileUtils.close(is);
                        }
                }
                
        }
        
        private String getExpectedDistributionUrl() {
                StringBuilder sb =  new StringBuilder();
                sb.append(dummyDistributionUrl).append('/');
                sb.append(AntWrapperTask.ANT_BIN_FILENAME_TEMPLATE);
                
                return String.format(sb.toString(), dummyAntVersion);
        }
        
        @Before
        public void initializeWrapperDir() {
               testDir.mkdirs();
               IOUtils.touchFile(new File(testDir, dummyWrapperJarFilename));
        }
        
        @After
        public void cleanupWrapperDir() {
                IOUtils.deleteFolder(testDir);
        }
        
        @Test
        public void verifyWrapperArtifacts() throws Exception {
                executeWrapperTask();
                
                File wrapperSupportDir = new File(testDir, AntWrapperTask.WRAPPER_ROOT_FOLDER_NAME);
                File wrapperPropertiesFile = new File(wrapperSupportDir, AntWrapperTask.WRAPPER_PROPERTIES_FILE_NAME);
                
                assertTrue(wrapperSupportDir.exists());
                assertTrue(new File(testDir, AntWrapperTask.LAUNCHER_WINDOWS_FILE_NAME).exists());
                assertTrue(new File(testDir, AntWrapperTask.LAUNCHER_WINDOWSCMD_FILE_NAME).exists());
                assertTrue(new File(testDir, AntWrapperTask.LCP_WINDOWS_FILE_NAME).exists());
                assertTrue(new File(testDir, AntWrapperTask.LAUNCHER_UNIX_FILE_NAME).exists());
                assertTrue(wrapperPropertiesFile.exists());
                assertTrue(new File(wrapperSupportDir, AntWrapperTask.WRAPPER_JAR_FILE_NAME).exists());
                
                assertEquals(getExpectedDistributionUrl(), readDistributionUrlFromWrapperProperties(wrapperPropertiesFile));
                
                
        }
}
