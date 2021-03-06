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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;
import java.util.regex.Pattern;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Hans Dockter
 */
public final class PathAssemblerTest {
        public static final String TEST_ANT_USER_HOME = "someUserHome";
        private final PathAssembler pathAssembler = new PathAssembler(new File(TEST_ANT_USER_HOME));
        private final WrapperConfiguration configuration = new WrapperConfiguration();

        @Before
        public void setup() {
                configuration.setDistributionBase(PathAssembler.ANT_USER_HOME_STRING);
                configuration.setDistributionPath("somePath");
                configuration.setZipBase(PathAssembler.ANT_USER_HOME_STRING);
                configuration.setZipPath("somePath");
        }

        @Test
        public void distributionDirWithAntUserHomeBase() throws Exception {
                configuration.setDistribution(new URI("http://server/dist/ant-0.9-bin.zip"));

                File distributionDir = pathAssembler.getDistribution(configuration).getDistributionDir();
                assertThat(distributionDir.getName(), matchesRegexp("[a-z0-9]+"));
                assertThat(distributionDir.getParentFile(), equalTo(file(TEST_ANT_USER_HOME + "/somePath/ant-0.9-bin")));
        }

        @Test
        public void distributionDirWithProjectBase() throws Exception {
                configuration.setDistributionBase(PathAssembler.PROJECT_STRING);
                configuration.setDistribution(new URI("http://server/dist/ant-0.9-bin.zip"));

                File distributionDir = pathAssembler.getDistribution(configuration).getDistributionDir();
                assertThat(distributionDir.getName(), matchesRegexp("[a-z0-9]+"));
                assertThat(distributionDir.getParentFile(), equalTo(file(currentDirPath() + "/somePath/ant-0.9-bin")));
        }

        @Test
        public void distributionDirWithUnknownBase() throws Exception {
                configuration.setDistribution(new URI("http://server/dist/ant-1.0.zip"));
                configuration.setDistributionBase("unknownBase");

                try {
                        pathAssembler.getDistribution(configuration);
                        fail();
                }
                catch (RuntimeException e) {
                        assertEquals("Base: unknownBase is unknown", e.getMessage());
                }
        }

        @Test
        public void distZipWithAntUserHomeBase() throws Exception {
                configuration.setDistribution(new URI("http://server/dist/ant-1.0.zip"));

                File dist = pathAssembler.getDistribution(configuration).getZipFile();
                assertThat(dist.getName(), equalTo("ant-1.0.zip"));
                assertThat(dist.getParentFile().getName(), matchesRegexp("[a-z0-9]+"));
                assertThat(dist.getParentFile().getParentFile(),
                           equalTo(file(TEST_ANT_USER_HOME + "/somePath/ant-1.0")));
        }

        @Test
        public void distZipWithProjectBase() throws Exception {
                configuration.setZipBase(PathAssembler.PROJECT_STRING);
                configuration.setDistribution(new URI("http://server/dist/ant-1.0.zip"));

                File dist = pathAssembler.getDistribution(configuration).getZipFile();
                assertThat(dist.getName(), equalTo("ant-1.0.zip"));
                assertThat(dist.getParentFile().getName(), matchesRegexp("[a-z0-9]+"));
                assertThat(dist.getParentFile().getParentFile(), equalTo(file(currentDirPath() + "/somePath/ant-1.0")));
        }

        private File file(String path) {
                return new File(path);
        }

        private String currentDirPath() {
                return System.getProperty("user.dir");
        }

        public static <T extends CharSequence> Matcher<T> matchesRegexp(final String pattern) {
                return new BaseMatcher<T>() {
                        public boolean matches(Object o) {
                                return Pattern.compile(pattern).matcher((CharSequence) o).matches();
                        }

                        public void describeTo(Description description) {
                                description.appendText("a CharSequence that matches regexp ").appendValue(pattern);
                        }
                };
        }
}
