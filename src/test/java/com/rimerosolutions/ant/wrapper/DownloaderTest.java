package com.rimerosolutions.ant.wrapper;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.net.URI;

import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class DownloaderTest {

        private DefaultDownloader download;
        private File testDir;
        private File downloadFile;
        private File rootDir;
        private URI sourceRoot;
        private File remoteFile;
        
        @Before
        public void setUp() throws Exception {
                download = new DefaultDownloader("antw", "aVersion");
                testDir = new File("target/testData");
                rootDir = new File(testDir, "root");
                downloadFile = new File(rootDir, "file");
                remoteFile = new File(testDir, "remoteFile");
 
                if (downloadFile.exists()) {
                        downloadFile.delete();
                }
                
                IOUtils.writeTextToFile("sometext", remoteFile);

                sourceRoot = remoteFile.toURI();
        }

        @Test
        public void testDownload() throws Exception {
                assert !downloadFile.exists();
                download.download(sourceRoot, downloadFile);
                assert downloadFile.exists();
                assertEquals("sometext", FileUtils.readFully(new FileReader(downloadFile)));
        }
}
