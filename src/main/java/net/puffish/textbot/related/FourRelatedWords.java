package net.puffish.textbot.related;

import net.puffish.textbot.util.TextUtil;
import net.puffish.textbot.word.FourWords;
import net.puffish.textbot.word.ThreeWords;
import net.puffish.textbot.word.Word;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class FourRelatedWords {
	private final HashMap<Word, ThreeWords[]> map = new HashMap<>();

	public void add(FourWords fourWords) {
		var key = fourWords.c();
		var newElement = new ThreeWords(fourWords.a(), fourWords.b(), fourWords.d());

		var array = map.get(key);
		if (array == null) {
			map.put(key, new ThreeWords[]{newElement});
		} else {
			for (var element : array) {
				if (element.equals(newElement)) {
					return;
				}
			}
			var newArray = new ThreeWords[array.length + 1];
			System.arraycopy(array, 0, newArray, 0, array.length);
			newArray[array.length] = newElement;
			map.put(key, newArray);
		}
	}

	public List<Word> getList(Word a, Word b, Word c) {
		var array = map.get(c);
		if (array == null) {
			return null;
		}

		var words = new ArrayList<Word>();
		for (var threeWords : array) {
			if (TextUtil.distance(threeWords.a().toString(), a.toString()) > 0.80f) {
				continue;
			}
			if (TextUtil.distance(threeWords.b().toString(), b.toString()) > 0.10f) {
				continue;
			}
			words.add(threeWords.c());
		}

		if (words.isEmpty()) {
			return null;
		}
		return words;
	}

	public int getCount() {
		return map.size();
	}
}
