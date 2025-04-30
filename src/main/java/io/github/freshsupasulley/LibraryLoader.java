package io.github.freshsupasulley;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.maxhenkel.rnnoise4j.UnknownPlatformException;

/**
 * Adapted from RNNoise4J.
 * 
 * @see <a href="https://github.com/henkelmax/rnnoise4j">RNNoise4J repo</a>
 */
public class LibraryLoader {
	
	private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
	private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();
	
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
	
	private static String getPlatform() throws UnknownPlatformException
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
			throw new UnknownPlatformException(String.format("Unknown operating system: %s", OS_NAME));
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
	
	private static String getNativeFolderName() throws UnknownPlatformException
	{
		return String.format("%s-%s", getPlatform(), getArchitecture());
	}
	
	public static void loadBundledNatives() throws IOException, UnknownPlatformException
	{
		String nativeFolder = getNativeFolderName();
		
		Path tempDir = Files.createTempDirectory("jscribe_natives");
		tempDir.toFile().deleteOnExit();
		
		// Find the path to this library's JAR
		URL jarUrl = LibraryLoader.class.getProtectionDomain().getCodeSource().getLocation();
		
		try(JarFile jar = new JarFile(jarUrl.getFile()))
		{
			Enumeration<JarEntry> entries = jar.entries();
			
			while(entries.hasMoreElements())
			{
				JarEntry entry = entries.nextElement();
				
				// Only load the files, not the directory
				if(entry.getName().startsWith("natives/" + nativeFolder) && !entry.isDirectory())
				{
					// Get the input stream for the file in the JAR
					try(InputStream inputStream = jar.getInputStream(entry))
					{
						// Copy the file to a temporary location
						Path tempFilePath = Files.createTempFile(null, null);
						File tempFile = tempFilePath.toFile();
						tempFile.deleteOnExit();
						
						Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
						
						JScribe.logger.debug("Loading native at {}", tempFile.getAbsolutePath());
						System.load(tempFile.getAbsolutePath());
					}
				}
			}
		}
	}
}