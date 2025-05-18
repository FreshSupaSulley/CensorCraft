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
import java.util.Enumeration;
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
	
	public static boolean isWindows()
	{
		return OS_NAME.contains("win");
	}
	
	public static boolean isMac()
	{
		return OS_NAME.contains("mac");
	}
	
	public static boolean isLinux()
	{
		return OS_NAME.contains("nux");
	}
	
	// private static String getPlatform() throws IOException
	// {
	// if(isWindows())
	// {
	// return "windows";
	// }
	// else if(isMac())
	// {
	// return "mac";
	// }
	// else if(isLinux())
	// {
	// return "linux";
	// }
	// else
	// {
	// throw new IOException(String.format("Unknown operating system: %s", OS_NAME));
	// }
	// }
	
	public static String getArchitecture()
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
	
	/**
	 * Use to determine if this system can custom-built Vulkan libs. Must be on Windows with AMD 64-bit processor.
	 * 
	 * @return true if this system can use Vulkan, false otherwise
	 */
	public static boolean canUseVulkan()
	{
		return LibraryLoader.isWindows() && LibraryLoader.getArchitecture().equals("x64");
	}
	
	// private static String getNativeFolderName() throws IOException
	// {
	// return String.format("%s-%s", getPlatform(), getArchitecture());
	// }
	//
	// /**
	// * Loads natives bundled into the jar at runtime.
	// *
	// * @param nativeComparator used to sift through all natives found at os/arch/*, because natives that depend on each other need to be ordered
	// * @throws IOException if something went wrong
	// */
	// public static void loadBundledNatives(Comparator<JarEntry> nativeComparator) throws IOException
	// {
	// String nativeFolder = getNativeFolderName();
	//
	// // Find the path to this library's JAR
	// URL jarURL = LibraryLoader.class.getProtectionDomain().getCodeSource().getLocation();
	//
	// // By default, assume its a file: protocol
	// String jarFile = jarURL.getFile();
	//
	// // If it's a jar: protocol
	// if(jarURL.getProtocol().equals("jar"))
	// {
	// JarURLConnection connection = (JarURLConnection) jarURL.openConnection();
	//
	// try
	// {
	// jarFile = new File(connection.getJarFileURL().toURI()).getAbsolutePath();
	// } catch(URISyntaxException e)
	// {
	// throw new IOException("Failed to open connection to jar", e);
	// }
	// }
	//
	// JScribe.logger.info("Loading JAR from {} (file: {})", jarURL, jarFile);
	//
	// try(JarFile jar = new JarFile(jarFile))
	// {
	// Enumeration<JarEntry> entries = jar.entries();
	// List<JarEntry> natives = new ArrayList<JarEntry>();
	//
	// while(entries.hasMoreElements())
	// {
	// JarEntry entry = entries.nextElement();
	//
	// // Only load the files, not the directory
	// if(entry.getName().startsWith("natives/" + nativeFolder) && !entry.isDirectory())
	// {
	// JScribe.logger.info("Found native at {}", entry.getName());
	// natives.add(entry);
	// }
	// }
	//
	// // The order matters!
	// natives.sort(nativeComparator);
	//
	// for(JarEntry entry : natives)
	// {
	// // Get the input stream for the file in the JAR
	// try(InputStream inputStream = jar.getInputStream(entry))
	// {
	// // Copy the file to a temporary location
	// Path tempFilePath = Files.createTempFile(null, null);
	// File tempFile = tempFilePath.toFile();
	// tempFile.deleteOnExit();
	//
	// Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
	//
	// JScribe.logger.info("Loading native {} from {}", entry.getName(), tempFile.getAbsolutePath());
	// System.load(tempFile.getAbsolutePath());
	// } catch(UnsatisfiedLinkError e)
	// {
	// throw new IOException("Failed to extract and load native library " + entry.getName(), e);
	// }
	// }
	// }
	//
	// JScribe.logger.info("Finished loading natives");
	// }
	
	/**
	 * Extracts a folder to a temp directory which is deleted at JVM exit.
	 * 
	 * @param folderName name of the folder (i.e. "natives/win-amd64-vulkan")
	 * @return temp directory path
	 * @throws IOException if something went wrong
	 */
	public static Path extractFolderToTemp(String folderName) throws IOException
	{
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
		
		JScribe.logger.info("Reading JAR from {} (file: {})", jarURL, jarFile);
		
		Path tmpDir = Files.createTempDirectory("jscribe_natives_");
		tmpDir.toFile().deleteOnExit();
		
		JScribe.logger.info("Creating temp directory at {}", tmpDir);
		
		try(JarFile jar = new JarFile(jarFile))
		{
			Enumeration<JarEntry> entries = jar.entries();
			
			while(entries.hasMoreElements())
			{
				JarEntry entry = entries.nextElement();
				
				// Only load the files, not the directory
				if(entry.getName().startsWith(folderName) && !entry.isDirectory())
				{
					// Get the input stream for the file in the JAR
					try(InputStream inputStream = jar.getInputStream(entry))
					{
						// Copy the file to a temporary location
						Path targetPath = tmpDir.resolve(entry.getName());
						Files.createDirectories(targetPath.getParent());
						JScribe.logger.info("Extracting native {} to {}", entry.getName(), targetPath);
						Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}
		
		JScribe.logger.info("Finished loading natives");
		return tmpDir.resolve(folderName);
	}
}