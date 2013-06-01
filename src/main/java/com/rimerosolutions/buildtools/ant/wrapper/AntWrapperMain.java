package com.rimerosolutions.buildtools.ant.wrapper;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Hello world!
 * 
 */
public class AntWrapperMain {

        public static final String DEFAULT_ANT_USER_HOME = System.getProperty( "user.home" ) + "/.ant"; 

        public static void main( String[] args )
            throws Exception
        {
            File defaultAntUserHomeDir = new File(DEFAULT_ANT_USER_HOME);
            
            if (!defaultAntUserHomeDir.exists()) {
                    defaultAntUserHomeDir.mkdirs();
            }
            
            File wrapperJar = wrapperJar();
            File propertiesFile = wrapperProperties( wrapperJar );
            File rootDir = rootDir( wrapperJar ); 
            
            addSystemProperties( rootDir );

            WrapperExecutor wrapperExecutor = WrapperExecutor.forWrapperPropertiesFile( propertiesFile, System.out );
            wrapperExecutor.execute( args, new Installer( new DefaultDownloader( "antw", wrapperVersion() ),
                                                        new PathAssembler( antUserHome() ) ), new BootstrapMainStarter() );
        }
 

        private static void addSystemProperties( File rootDir )
        {
            System.getProperties().putAll( SystemPropertiesHandler.getSystemProperties( new File( antUserHome(),
                                                                                                  "ant.properties" ) ) );
            System.getProperties().putAll( SystemPropertiesHandler.getSystemProperties( new File( rootDir,
                                                                                                  "ant.properties" ) ) );
        }

        private static File rootDir( File wrapperJar )
        {
            return wrapperJar.getParentFile().getParentFile().getParentFile();
        }

        private static File wrapperProperties( File wrapperJar )
        {
            return new File( wrapperJar.getParent(), wrapperJar.getName().replaceFirst( "\\.jar$", ".properties" ) );
        }

        private static File wrapperJar()
        {
            URI location;
            try
            {
                location = AntWrapperMain.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            }
            catch ( URISyntaxException e )
            {
                throw new RuntimeException( e );
            }
            if ( !location.getScheme().equals( "file" ) )
            {
                throw new RuntimeException(
                                            String.format( "Cannot determine classpath for wrapper Jar from codebase '%s'.",
                                                           location ) );
            }
            return new File( location.getPath() );
        }

        static String wrapperVersion()
        {
           return "1.0";
        }

        private static File antUserHome()
        {
            String antUserHome = System.getProperty( PathAssembler.ANT_USER_HOME_STRING);
            if ( antUserHome != null )
            {
                return new File( antUserHome );
            }
            else if ( ( antUserHome = System.getenv( PathAssembler.ANT_USER_HOME_STRING ) ) != null )
            {
                return new File( antUserHome );
            }
            else
            {
                return new File( DEFAULT_ANT_USER_HOME );
            }
        }
    }
