package io.github.freshsupasulley.censorcraft.api.events.client;

/**
 * Fired when transcription results come back from whisper on the client before they're sent to the server.
 */
public interface SendTranscriptionEvent extends ClientEvent {
	
	/**
	 * Returns the transcription result of what the player spoke into their microphone.
	 *
	 * @return transcription result
	 */
	String getTranscription();
}
