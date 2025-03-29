package com.supasulley.macrophone;

/**
 * Constants that indicate the status of an app’s authorization to capture media.
 * 
 * @see <a href=
 *      "https://developer.apple.com/documentation/avfoundation/avauthorizationstatus">https://developer.apple.com/documentation/avfoundation/avauthorizationstatus</a>
 */
public enum AVAuthorizationStatus
{
	/** A status that indicates the user hasn’t yet granted or denied authorization. */
	NotDetermined,
	
	/** A status that indicates the app isn’t permitted to use media capture devices. */
	Authorized,
	
	/** A status that indicates the user has explicitly denied an app permission to capture media. */
	Denied,
	
	/** A status that indicates the user has explicitly granted an app permission to capture media. */
	Restricted;
}