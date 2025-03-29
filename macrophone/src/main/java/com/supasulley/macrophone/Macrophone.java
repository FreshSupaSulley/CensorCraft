package com.supasulley.macrophone;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Allows prompting macOS clients for microphone permission.
 */
public class Macrophone {
	
	static
	{
		try
		{
			Path tempLib = Files.createTempFile("macrophone", ".dylib");
			tempLib.toFile().deleteOnExit();
			Files.copy(Macrophone.class.getClassLoader().getResourceAsStream("macrophone.dylib"), tempLib, StandardCopyOption.REPLACE_EXISTING);
			System.load(tempLib.toAbsolutePath().toString());
		} catch(UnsatisfiedLinkError e)
		{
			System.err.println("Failed to load native library");
			e.printStackTrace();
		} catch(Exception e)
		{
			System.err.println("Failed to prepare native library for loading");
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns an authorization status that indicates whether the user grants the app permission to capture audio.
	 * 
	 * @see <a href=
	 *      "https://developer.apple.com/documentation/avfoundation/avcapturedevice/authorizationstatus(for:)">https://developer.apple.com/documentation/avfoundation/avcapturedevice/authorizationstatus(for:)</a>
	 * 
	 * @return {@linkplain AVAuthorizationStatus} enum representing the authorization status value
	 */
	public static native AVAuthorizationStatus getMicrophonePermission();
	
	/**
	 * Opens microphone privacy settings.
	 */
	public static native void openMicrophoneSettings();
	
	/**
	 * Requests the user’s permission to allow the app to capture audio.
	 * 
	 * <p>
	 * This method is <b>non-blocking</b>. You'll need to call {@linkplain Macrophone#getMicrophonePermission()} to determine if permission was granted.
	 * </p>
	 * 
	 * <p>
	 * <B>IMPORTANT!</b> If your Java program is not running in an environment with entitlements and you attempt to request microphone access, "the system
	 * terminates your app"
	 * (<a href="https://developer.apple.com/documentation/avfoundation/requesting-authorization-to-capture-and-save-media?language=objc">documentation</a>). This
	 * library is only suitable if you are bundling it with entitlements or running it from a jar with a process that has entitlements (do not use in an IDE).
	 * </p>
	 * 
	 * @see <a href=
	 *      "https://developer.apple.com/documentation/avfoundation/avcapturedevice/requestaccess(for:completionhandler:)">https://developer.apple.com/documentation/avfoundation/avcapturedevice/requestaccess(for:completionhandler:)</a>
	 */
	public static native void requestMicrophoneAccess();
	
//	public static native void startTranscribing();
}
