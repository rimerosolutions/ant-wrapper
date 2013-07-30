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

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;

/**
 * Path assembler
 *
 * @author Hans Dockter
 * @author Yves Zoundi
 */
public class PathAssembler {

        public static final String ANT_USER_HOME_STRING = "ANT_USER_HOME";
        public static final String PROJECT_STRING = "PROJECT";
        private File antUserHome;

        public PathAssembler() {
        }

        public PathAssembler(File antUserHome) {
                this.antUserHome = antUserHome;
        }

        /**
         * Determines the local locations for the distribution to use given the supplied configuration.
         */
        public LocalDistribution getDistribution(WrapperConfiguration configuration) {
                String baseName = getDistName(configuration.getDistribution());
                String distName = removeExtension(baseName);
                String rootDirName = rootDirName(distName, configuration);

                String distDirPathName = configuration.getDistributionPath() + "/" + rootDirName;
                File distDir = new File(getBaseDir(configuration.getDistributionBase()), distDirPathName);

                String distZipFilename = configuration.getZipPath() + "/" + rootDirName + "/" + baseName;
                File distZip = new File(getBaseDir(configuration.getZipBase()), distZipFilename);

                return new LocalDistribution(distDir, distZip);
        }

        private String rootDirName(String distName, WrapperConfiguration configuration) {
                String urlHash = getMd5Hash(configuration.getDistribution().toString());
                
                return String.format("%s/%s", distName, urlHash);
        }

        private String getMd5Hash(String string) {
                try {
                        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                        byte[] bytes = string.getBytes();
                        messageDigest.update(bytes);
                        return new BigInteger(1, messageDigest.digest()).toString(32);
                }
                catch (Exception e) {
                        throw new RuntimeException("Could not hash input string.", e);
                }
        }

        private String removeExtension(String name) {
                int p = name.lastIndexOf(".");

                if (p < 0) {
                        return name;
                }

                return name.substring(0, p);
        }

        private String getDistName(URI distUrl) {
                String path = distUrl.getPath();
                int p = path.lastIndexOf("/");

                if (p < 0) {
                        return path;
                }

                return path.substring(p + 1);
        }

        private File getBaseDir(String base) {
                if (base.equals(ANT_USER_HOME_STRING)) {
                        return antUserHome;
                }
                else if (base.equals(PROJECT_STRING)) {
                        return new File(System.getProperty("user.dir"));
                }
                else {
                        throw new RuntimeException("Base: " + base + " is unknown");
                }
        }

        public class LocalDistribution {

                private final File distZip;
                private final File distDir;

                public LocalDistribution(File distDir, File distZip) {
                        this.distDir = distDir;
                        this.distZip = distZip;
                }

                /**
                 * Returns the location to install the distribution into.
                 */
                public File getDistributionDir() {
                        return distDir;
                }

                /**
                 * Returns the location to install the distribution ZIP file to.
                 */
                public File getZipFile() {
                        return distZip;
                }
        }
}
