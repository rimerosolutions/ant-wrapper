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
package com.rimerosolutions.ant.wrapper;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Touch;

/**
 * Utility class for unit tests
 * 
 * @author Yves Zoundi
 */
public final class IOUtils {

        private IOUtils() {
                throw new AssertionError();
        }

        private static void ensureParentFolderExists(File file) {
                if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                }
        }

        public static void writeTextToFile(String text, File file) {
                ensureParentFolderExists(file);

                Echo echo = new Echo();
                echo.setProject(new Project());
                echo.addText(text);
                echo.setFile(file);
                echo.execute();
        }

        public static void touchFile(File file) {
                ensureParentFolderExists(file);

                Touch touch = new Touch();
                touch.setProject(new Project());
                touch.setFile(file);
                touch.execute();
        }

        public static void deleteFolder(File folder) {
                if (folder.exists()) {
                        Delete delete = new Delete();
                        delete.setProject(new Project());
                        delete.setDir(folder);
                        delete.setIncludeEmptyDirs(true);
                        delete.setQuiet(true);
                        delete.execute();
                }
        }
}
