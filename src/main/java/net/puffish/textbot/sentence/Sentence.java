package net.puffish.textbot.sentence;

import net.puffish.textbot.related.FourRelatedWords;
import net.puffish.textbot.related.ThreeRelatedWords;
import net.puffish.textbot.related.TwoRelatedWords;
import net.puffish.textbot.util.StringCache;
import net.puffish.textbot.word.FourWords;
import net.puffish.textbot.word.ThreeWords;
import net.puffish.textbot.word.TwoWords;
import net.puffish.textbot.word.Word;

import java.util.ArrayList;
import java.util.List;

public final class Sentence {
	private final Word[] wordsList;

	private Sentence(Word[] wordsList) {
		this.wordsList = wordsList;
	}

	public static Sentence create(long author, List<String> list) {
		var wordsList = new ArrayList<Word>();

		for (var part : list) {
			var parts = part.split("\\s+");

			for (var i = 0; i < parts.length; i++) {
				var isLast = i == parts.length - 1;

				var word = Word.create(author, parts[i], isLast);
				if (!word.isValid()) {
					continue;
				}

				wordsList.add(word);
			}
		}

		return new Sentence(wordsList.toArray(new Word[0]));
	}

	public void collectRelated(TwoRelatedWords two, ThreeRelatedWords three, FourRelatedWords four) {
		for (var i = 0; i < Math.max(0, wordsList.length - 1); i++) {
			two.add(new TwoWords(wordsList[i], wordsList[i + 1]));
		}
		for (var i = 0; i < Math.max(0, wordsList.length - 2); i++) {
			three.add(new ThreeWords(wordsList[i], wordsList[i + 1], wordsList[i + 2]));
		}
		for (var i = 0; i < Math.max(0, wordsList.length - 3); i++) {
			four.add(new FourWords(wordsList[i], wordsList[i + 1], wordsList[i + 2], wordsList[i + 3]));
		}
	}

	public Sentence cached(StringCache cache) {
		var words = new Word[wordsList.length];
		for (var i = 0; i < wordsList.length; i++) {
			words[i] = wordsList[i].cached(cache);
		}
		return new Sentence(words);
	}

	public Word[] getWordsList() {
		return wordsList;
	}
}
