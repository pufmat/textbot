package net.puffish.textbot.word;

import net.puffish.textbot.util.StringCache;

import java.util.regex.Pattern;

public final class Word {
	private static final Pattern EMOJI_PATTERN = Pattern.compile("^<a?:([^:]+):([0-9]+)>$");

	private static final int MAX_WORD_LENGTH = 32;

	private final String comparableString;
	private final String string;
	private final boolean isLast;
	private final long author;
	private final long emoji;

	private Word(long author, String string, String comparableString, long emoji, boolean isLast) {
		this.author = author;
		this.string = string;
		this.comparableString = comparableString;
		this.emoji = emoji;
		this.isLast = isLast;
	}

	public static Word create(long author, String string) {
		return create(author, string, false);
	}

	public static Word create(long author, String string, boolean isLast) {
		var comparableString = string.toLowerCase()
				.replace(".", "")
				.replace(":", "")
				.replace(",", "")
				.replace("!", "")
				.replace("?", "");

		var emoji = 0L;
		var matcher = EMOJI_PATTERN.matcher(string);
		if (matcher.find()) {
			try {
				emoji = Long.parseLong(matcher.group(2));
			} catch (Exception ignored) {
			}
		}

		return new Word(
				author,
				string,
				comparableString,
				emoji,
				isLast
		);
	}

	public Word cached(StringCache stringCache) {
		return new Word(
				author,
				stringCache.register(string),
				stringCache.register(comparableString),
				emoji,
				isLast
		);
	}

	public boolean isValid() {
		if (comparableString.isEmpty()) {
			return false;
		}
		if (string.contains("@")) {
			return false;
		}
		if (string.contains("discord.gg")) {
			return false;
		}
		if (emoji != 0) {
			return true;
		}
		return string.length() <= MAX_WORD_LENGTH;
	}

	public boolean isEmoji() {
		return emoji != 0;
	}

	public long getEmojiIdLong() {
		return emoji;
	}

	public long getAuthorIdLong() {
		return author;
	}

	public boolean isLast() {
		return isLast;
	}

	@Override
	public String toString() {
		return string;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		var word = (Word) o;
		return comparableString.equals(word.comparableString);
	}

	@Override
	public int hashCode() {
		return comparableString.hashCode();
	}
}
