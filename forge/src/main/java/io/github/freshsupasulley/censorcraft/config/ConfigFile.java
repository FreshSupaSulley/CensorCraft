package io.github.freshsupasulley.censorcraft.config;

import java.io.File;
import java.nio.file.Path;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.file.GenericBuilder;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;

public abstract class ConfigFile {

	private transient Path path;

	public ConfigFile(File file) {
		// Setting autosave means it will not allow program to die... same with autoreload (2 things I need)
		GenericBuilder<CommentedConfig, CommentedFileConfig> builder = CommentedFileConfig.builder(file)/*.autosave()*/
				.writingMode(WritingMode.REPLACE).sync().preserveInsertionOrder()
				.onFileNotFound(FileNotFoundAction.CREATE_EMPTY);

		try (CommentedFileConfig config = builder.build()) {
			config.load();
			this.path = config.getNioPath();
			
			config.addAll(ObjectSerializer.standard().serializeFields(this, () -> config.createSubConfig()));
			config.save();
			System.out.println("done w " + file);
		} catch (Exception e) {
			System.err.println("Config file is malformed at {}!" + path);
			// throw e;
		}
	}

//	abstract void register(ConfigWrapper config);

	public Path getFilePath() {
		return path;
	}
}
