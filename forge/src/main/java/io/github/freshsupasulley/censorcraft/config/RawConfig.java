package io.github.freshsupasulley.censorcraft.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.WritingMode;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Alternative to {@link ConfigSpec}.
 */
public abstract class RawConfig {

	private CommentedFileConfig config;

	public RawConfig(String name) {
		config = CommentedFileConfig.builder(FMLPaths.CONFIGDIR.get().resolve(CensorCraft.MODID + name + ".toml"))
				.autosave().autoreload().writingMode(WritingMode.REPLACE).sync().preserveInsertionOrder()
				.onFileNotFound(FileNotFoundAction.CREATE_EMPTY).build();
		config.load();

		register();

		config.save();
	}

	public Path getFilePath() {
		return config.getNioPath();
	}

	protected abstract void register();

	// <T> ConfigValue<T> setDefault(String key, T defaultValue, String comment,
	// Predicate<T> validator)
	// {
	// if(!config.contains(key))
	// {
	// config.set(key, defaultValue);
	// }
	//
	// config.setComment(key, comment);
	//
	// return new ConfigValue<T>(config, key, defaultValue, validator, comment);
	// }

	<T extends Comparable<T>> ConfigValueBuilder<T> newConfig(String name, T value) {
		ConfigValueBuilder<T> result = new ConfigValueBuilder<T>(name, value);
		builders.add(result);
		return result;
	}

	<T extends Comparable<T>> ConfigValue<T> add(ConfigValueBuilder<T> builder) {
		ConfigValue<T> built = builder.build(config);

		if (!config.contains(built.key)) {
			config.set(built.key, built.defaultValue);
		}

		if (!builder.comments.isEmpty()) {
			config.setComment(built.key,
					builder.comments.stream().collect(Collectors.joining(System.getProperty("line.separator"))));
		}
		
		return built;
	}

	class ConfigValueBuilder<T extends Comparable<T>> {

		private final String key;
		private final T defaultValue;

		// Optional
		private List<String> comments = new ArrayList<String>();
		private Predicate<T> validator = (t) -> true;

		public ConfigValueBuilder(String key, T defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
		}

		public ConfigValueBuilder<T> addValidator(Predicate<T> validator) {
			if (!validator.test(defaultValue)) {
				throw new IllegalArgumentException("Default value fails validation");
			}

			this.validator = validator;
			return this;
		}

		// I am a fucking genius
		public ConfigValueBuilder<T> setRange(T min, T max) {
			Comparator<T> comparator = Comparator.naturalOrder();
			Predicate<T> rangeValidator = (T value) -> comparator.compare(value, min) >= 0
					&& comparator.compare(value, max) <= 0;
			addValidator(rangeValidator);
			return this;
		}

		public ConfigValueBuilder<T> addComment(String comment) {
			this.comments.add(comment);
			return this;
		}

		public ConfigValue<T> build() {
			if (!config.contains(key)) {
				config.set(key, defaultValue);
			}

			if (!comments.isEmpty()) {
				config.setComment(key, comments.stream().collect(Collectors.joining(System.getProperty("line.separator"))));
			}
			
			return new ConfigValue<T>(config, key, defaultValue, validator);
		}
	}

	public class ConfigValue<T> {

		private final CommentedFileConfig config;
		private final String key;
		private final T defaultValue;
		private Predicate<T> validator;

		public ConfigValue(CommentedFileConfig config, String key, T defaultValue, Predicate<T> validator) {
			this.config = config;
			this.key = key;
			this.defaultValue = defaultValue;
			this.validator = validator;
		}

		public T get() {
			T value = config.getOrElse(key, defaultValue);

			if (validator.test(defaultValue)) {
				return value;
			}

			return defaultValue;
		}

		public void set(T value) {
			if (validator.test(value)) {
				config.set(key, value);
			}
		}
	}
}
