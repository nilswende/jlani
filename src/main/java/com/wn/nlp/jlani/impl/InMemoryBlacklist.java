package com.wn.nlp.jlani.impl;

import com.wn.nlp.jlani.value.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * Flags words as unwanted using an in-memory blacklist.
 */
public class InMemoryBlacklist implements Blacklist {
	static final String PROPERTY = "BlacklistFile";
	private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryBlacklist.class);
	private final Set<Word> blacklist;
	
	private InMemoryBlacklist(final Set<Word> blacklist) {
		this.blacklist = blacklist;
	}
	
	/**
	 * Creates a new Blacklist from a list of words.
	 *
	 * @param properties the JLanI properties. May contain a path to a blacklist file
	 */
	public static Blacklist ofProperty(final Properties properties) {
		Objects.requireNonNull(properties);
		var pathString = properties.getProperty(PROPERTY);
		if (pathString == null) {
			return new NullBlacklist();
		} else if (pathString.isBlank()) {
			LOGGER.info("Ignored empty '{}' declaration", PROPERTY);
			return new NullBlacklist();
		}
		var path = Path.of(pathString);
		if (Files.notExists(path)) {
			LOGGER.warn("Ignored missing '{}': {}", PROPERTY, path.toAbsolutePath());
			return new NullBlacklist();
		}
		try (var reader = new InputStreamReader(Files.newInputStream(path))) {
			return ofReader(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	/**
	 * Creates a new Blacklist from a list of words.
	 *
	 * @param reader the blacklisted words
	 */
	public static Blacklist ofReader(final Reader reader) {
		var set = new HashSet<Word>();
		try (var lineReader = new BufferedReader(reader)) {
			for (String line; (line = lineReader.readLine()) != null; ) {
				if (line.isBlank()) continue;
				set.add(new Word(line));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return new InMemoryBlacklist(set);
	}
	
	@Override
	public boolean isBlacklisted(final Word word) {
		return blacklist.contains(word);
	}
}
