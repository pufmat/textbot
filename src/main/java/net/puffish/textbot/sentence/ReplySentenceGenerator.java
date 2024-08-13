package net.puffish.textbot.sentence;

import net.puffish.textbot.word.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ReplySentenceGenerator {
	private final List<Sentence> sentences;
	private final List<Integer> options;

	public ReplySentenceGenerator(List<Sentence> sentences) {
		this.sentences = sentences;
		this.options = new ArrayList<>();
	}

	public boolean generate(String start) {
		var startSentence = Sentence.create(0, Collections.singletonList(start));

		var bestCount = 0;

		for (var i = 0; i < sentences.size() - 1; i++) {
			var sentence = sentences.get(i);

			var count = 0;
			for (var startWord : startSentence.getWordsList()) {
				for (var word : sentence.getWordsList()) {
					if (word.equals(startWord)) {
						count++;
						break;
					}
				}
			}

			if (count == 0) {
				continue;
			}

			if (count > bestCount) {
				bestCount = count;
				options.clear();
				options.add(i);
			} else if (count == bestCount) {
				options.add(i);
			}
		}

		return bestCount > 0;
	}

	public List<Word> getRandomWords(Random random) {
		if (options.isEmpty()) {
			return new ArrayList<>();
		}

		// pick random best matching sentence
		var i = options.get(random.nextInt(options.size()));

		// generated words based on the next sentence
		var sentence = sentences.get(i + 1);

		var words = new ArrayList<Word>();
		for (var word : sentence.getWordsList()) {
			// ignore short words
			if (word.toString().length() <= 2) {
				continue;
			}

			words.add(word);
		}

		return words;
	}
}
