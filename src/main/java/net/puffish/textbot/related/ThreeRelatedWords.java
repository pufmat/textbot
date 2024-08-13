package net.puffish.textbot.related;

import net.puffish.textbot.util.TextUtil;
import net.puffish.textbot.word.ThreeWords;
import net.puffish.textbot.word.TwoWords;
import net.puffish.textbot.word.Word;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class ThreeRelatedWords {
	private final HashMap<Word, TwoWords[]> map = new HashMap<>();

	public void add(ThreeWords threeWords) {
		var key = threeWords.b();
		var newElement = new TwoWords(threeWords.a(), threeWords.c());

		var array = map.get(key);
		if (array == null) {
			map.put(key, new TwoWords[]{newElement});
		} else {
			for (var element : array) {
				if (element.equals(newElement)) {
					return;
				}
			}
			var newArray = new TwoWords[array.length + 1];
			System.arraycopy(array, 0, newArray, 0, array.length);
			newArray[array.length] = newElement;
			map.put(key, newArray);
		}
	}

	public List<Word> getList(Word a, Word b) {
		var array = map.get(b);
		if (array == null) {
			return null;
		}

		var words = new ArrayList<Word>();
		for (var twoWords : array) {
			if (TextUtil.distance(twoWords.a().toString(), a.toString()) < 0.10f) {
				words.add(twoWords.b());
			}
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
