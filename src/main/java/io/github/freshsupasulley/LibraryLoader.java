package io.github.freshsupasulley;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Adapted from RNNoise4J.
 * 
 * @see <a href="https://github.com/henkelmax/rnnoise4j">RNNoise4J repo</a>
 */
public class LibraryLoader {
	
	public static final String OS_NAME = System.getProperty("os.name").toLowerCase();
	public static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();
	
	private static boolean isWindows()
	{
		return OS_NAME.contains("win");
	}
	
	private static boolean isMac()
	{
		return OS_NAME.contains("mac");
	}
	
	private static boolean isLinux()
	{
		return OS_NAME.contains("nux");
	}
	
	private static String getPlatform() throws IOException
	{
		if(isWindows())
		{
			return "windows";
		}
		else if(isMac())
		{
			return "mac";
		}
		else if(isLinux())
		{
			return "linux";
		}
		else
		{
			throw new IOException(String.format("Unknown operating system: %s", OS_NAME));
		}
	}
	
	private static String getArchitecture()
	{
		switch(OS_ARCH)
		{
			case "i386":
			case "i486":
			case "i586":
			case "i686":
			case "x86":
			case "x86_32":
				return "x86";
			case "amd64":
			case "x86_64":
			case "x86-64":
				return "x64";
			case "aarch64":
				return "aarch64";
			default:
				return OS_ARCH;
		}
	}
	
	private static String getNativeFolderName() throws IOException
	{
		return String.format("%s-%s", getPlatform(), getArchitecture());
	}
	
	/**
	 * Loads natives bundled into the jar at runtime.
	 * 
	 * @param nativeComparator used to sift through all natives found at os/arch/*, because natives that depend on each other need to be ordered
	 * @throws IOException if something went wrong
	 */
	public static void loadBundledNatives(Comparator<JarEntry> nativeComparator) throws IOException
	{
		String nativeFolder = getNativeFolderName();
		
		Path tempDir = Files.createTempDirectory("jscribe_natives");
		tempDir.toFile().deleteOnExit();
		
		// Find the path to this library's JAR
		URL jarURL = LibraryLoader.class.getProtectionDomain().getCodeSource().getLocation();
		
		// By default, assume its a file: protocol
		String jarFile = jarURL.getFile();
		
		// If it's a jar: protocol
		if(jarURL.getProtocol().equals("jar"))
		{
			JarURLConnection connection = (JarURLConnection) jarURL.openConnection();
			
			try
			{
				jarFile = new File(connection.getJarFileURL().toURI()).getAbsolutePath();
			} catch(URISyntaxException e)
			{
				throw new IOException("Failed to open connection to jar", e);
			}
		}
		
		JScribe.logger.info("Loading JAR from {} (file: {})", jarURL, jarFile);
		
		try(JarFile jar = new JarFile(jarFile))
		{
			Enumeration<JarEntry> entries = jar.entries();
			List<JarEntry> natives = new ArrayList<JarEntry>();
			
			while(entries.hasMoreElements())
			{
				JarEntry entry = entries.nextElement();
				
				// Only load the files, not the directory
				if(entry.getName().startsWith("natives/" + nativeFolder) && !entry.isDirectory())
				{
					JScribe.logger.info("Found native at {}", entry.getName());
					natives.add(entry);
				}
			}
			
			// The order matters!
			natives.sort(nativeComparator);
			
			for(JarEntry entry : natives)
			{
				// Get the input stream for the file in the JAR
				try(InputStream inputStream = jar.getInputStream(entry))
				{
					// Copy the file to a temporary location
					Path tempFilePath = Files.createTempFile(null, null);
					File tempFile = tempFilePath.toFile();
					tempFile.deleteOnExit();
					
					Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
					
					JScribe.logger.info("Loading native {} from {}", entry.getName(), tempFile.getAbsolutePath());
					System.load(tempFile.getAbsolutePath());
				} catch(UnsatisfiedLinkError e)
				{
					throw new IOException("Failed to load native library at " + entry.getName(), e);
				}
			}
		}
		
		JScribe.logger.info("Finished loading natives");
	}
}