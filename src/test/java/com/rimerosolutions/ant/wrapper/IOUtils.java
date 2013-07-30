package com.rimerosolutions.ant.wrapper;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Touch;

public class IOUtils {

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
}
