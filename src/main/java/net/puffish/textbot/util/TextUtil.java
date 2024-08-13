package net.puffish.textbot.util;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.text.similarity.SimilarityScore;

public final class TextUtil {
	private static final SimilarityScore<Integer> SIMILARITY_SCORE = new LevenshteinDistance();

	public static float distance(String a, String b) {
		var distance = SIMILARITY_SCORE.apply(a, b);
		return (float) distance / Math.max(a.length(), b.length());
	}
}
