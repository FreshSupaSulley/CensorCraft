package io.github.freshsupasulley;

import java.io.IOException;

/**
 * Indicates no microphones were detected that can be used.
 */
public class NoMicrophoneException extends IOException {
	
	private static final long serialVersionUID = -7735773729444554129L;
	
	NoMicrophoneException(String message)
	{
		super(message);
	}
}
