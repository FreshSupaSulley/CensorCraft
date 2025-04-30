package io.github.freshsupasulley;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

import de.maxhenkel.rnnoise4j.UnknownPlatformException;

/**
 * Adapted from RNNoise4J.
 * 
 * @see https://github.com/henkelmax/rnnoise4j
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
		
		try(FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jarUrl.toURI()), Collections.emptyMap()))
		{
			Path jarRoot = fs.getPath("/");
			Path nativePath = jarRoot.resolve(nativeFolder);
			
			if(!Files.exists(nativePath))
			{
				throw new UnknownPlatformException("Natives not found at " + nativeFolder);
			}
			
			// Copy all files in the native folder
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(nativePath))
			{
				for(Path file : stream)
				{
					if(Files.isRegularFile(file))
					{
						Path tempFile = tempDir.resolve(file.getFileName().toString());
						Files.copy(file, tempFile, StandardCopyOption.REPLACE_EXISTING);
						tempFile.toFile().deleteOnExit();
						
						// Load the native library
						System.load(tempFile.toAbsolutePath().toString());
					}
				}
			}
		} catch(URISyntaxException e)
		{
			throw new IOException(e);
		}
	}
}