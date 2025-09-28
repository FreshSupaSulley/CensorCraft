package io.github.freshsupasulley.censorcraft.api.punishments;

import com.electronwill.nightconfig.toml.TomlFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * A punishment that will be fired on the client-side.
 *
 * <p>Most of the time, server-side punishments are sufficient. But this is useful if you need client-specific code to
 * be run on the player's machine.</p>
 *
 * <p>The server config's punishment settings is automatically sent to the client, meaning you can utilize the
 * <code>config</code> instance variable on the client as well.</p>
 */
public abstract class ClientPunishment extends Punishment {
	
	/**
	 * Punishes the player for this punishment type <b>on the client-side</b> (on the punished-player's machine).
	 *
	 * <p>You have access to read the <code>config</code> instance variable here! It's transferred over the wire to the
	 * client. Setting anything to the config does nothing.</p>
	 */
	public abstract void punish();
	
	/**
	 * Serializes and returns the server's config file to be sent to the client.
	 */
	public final byte[] serializeConfig()
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(out);
		TomlFormat.instance().createWriter().write(config, writer);
		
		try
		{
			writer.flush();
		} catch(IOException e)
		{
			// This should never happen so lazily wrap
			throw new RuntimeException(e);
		}
		
		return out.toByteArray();
	}
}
