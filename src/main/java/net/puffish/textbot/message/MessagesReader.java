package net.puffish.textbot.message;

import de.siegmar.fastcsv.reader.CsvReader;
import net.puffish.textbot.related.FourRelatedWords;
import net.puffish.textbot.related.ThreeRelatedWords;
import net.puffish.textbot.related.TwoRelatedWords;
import net.puffish.textbot.sentence.Sentence;
import net.puffish.textbot.util.StringCache;
import net.puffish.textbot.word.Word;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MessagesReader {
	private HashMap<Long, Long> previousUsers = new HashMap<>();

	private final FourRelatedWords fourRelatedWords = new FourRelatedWords();
	private final ThreeRelatedWords threeRelatedWords = new ThreeRelatedWords();
	private final TwoRelatedWords twoRelatedWords = new TwoRelatedWords();
	private final List<Word> firstWords = new ArrayList<>();

	private List<String> messageParts = new ArrayList<>();
	private long currentUser;

	private final List<Sentence> sentences = new ArrayList<>();

	private int skippedCount;
	private final int sentencesLimit;

	private final StringCache stringCache = new StringCache();

	public MessagesReader(int sentencesLimit) {
		this.sentencesLimit = sentencesLimit;
		this.skippedCount = 0;
	}

	public void read(Path path) throws IOException {
		try (var csv = CsvReader.builder().ofCsvRecord(path)) {
			csv.forEach(row -> message(
					Long.parseLong(row.getField(0)),
					Long.parseLong(row.getField(1)),
					row.getField(2)
			));
		}
		finishMessage();
	}

	public void generateRelatedWords() {
		for (var sentence : sentences) {
			firstWords.add(sentence.getWordsList()[0]);
			sentence.collectRelated(twoRelatedWords, threeRelatedWords, fourRelatedWords);
		}
	}

	public void cache() {
		sentences.replaceAll(sentence -> sentence.cached(stringCache));
	}

	public void free() {
		previousUsers.clear();
		previousUsers = null;
		messageParts.clear();
		messageParts = null;
	}

	private void message(long channel, long user, String message) {
		var previousUser = previousUsers.get(channel);

		if (previousUser == null) {
			currentUser = user;
			previousUsers.put(channel, currentUser);
		} else if (previousUser != user) {
			finishMessage();
			currentUser = user;
			previousUsers.put(channel, currentUser);
		}

		messageParts.add(message);
	}

	private void finishMessage() {
		if (!messageParts.isEmpty()) {
			var sentence = Sentence.create(currentUser, messageParts);
			if (sentence.getWordsList().length > 0) {
				sentences.add(sentence);
				if (sentences.size() > sentencesLimit) {
					sentences.remove(0);
					skippedCount++;
				}
			}
		}
		messageParts.clear();
	}

	public TwoRelatedWords getRelatedTwoWords() {
		return twoRelatedWords;
	}

	public ThreeRelatedWords getRelatedThreeWords() {
		return threeRelatedWords;
	}

	public FourRelatedWords getRelatedFourWords() {
		return fourRelatedWords;
	}

	public List<Word> getFirstWords() {
		return firstWords;
	}

	public List<Sentence> getSentences() {
		return sentences;
	}

	public int getSkippedCount() {
		return skippedCount;
	}

	public StringCache getStringCache() {
		return stringCache;
	}

}
