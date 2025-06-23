package io.github.freshsupasulley.censorcraft;

import com.electronwill.nightconfig.core.serde.ObjectSerializer;

import io.github.freshsupasulley.censorcraft.config.ClientConfig;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;

public class CensorCraft {
	
//	public static ClientConfig CLIENT = new ClientConfig();
//	public static ServerConfig SERVER = new ServerConfig();
//	
	public static void main(String[] args)
	{
		new ClientConfig();
		new ServerConfig();
		System.out.println("done");
//		ObjectSerializer serializer = ObjectSerializer.standard();
//		serializer.serialize(CLIENT, () -> CLIENT.config.createSubConfig());
	}
}
