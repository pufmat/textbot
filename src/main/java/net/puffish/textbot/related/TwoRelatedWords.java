package net.puffish.textbot.related;

import net.puffish.textbot.word.TwoWords;
import net.puffish.textbot.word.Word;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public final class TwoRelatedWords {
	private final HashMap<Word, Word[]> map = new HashMap<>();

	public void add(TwoWords twoWords) {
		var key = twoWords.a();
		var newElement = twoWords.b();

		var array = map.get(key);
		if (array == null) {
			map.put(key, new Word[]{newElement});
		} else {
			for (var element : array) {
				if (element.equals(newElement)) {
					return;
				}
			}
			var newArray = new Word[array.length + 1];
			System.arraycopy(array, 0, newArray, 0, array.length);
			newArray[array.length] = newElement;
			map.put(key, newArray);
		}
	}

	public List<Word> getList(Word a) {
		var array = map.get(a);
		if (array == null) {
			return null;
		}
		return Arrays.asList(array);
	}

	public int getCount() {
		return map.size();
	}

}
