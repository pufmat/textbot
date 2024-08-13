package net.puffish.textbot.sentence;

import net.puffish.textbot.message.MessagesReader;
import net.puffish.textbot.word.Word;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;

public final class SentenceGenerator {
	private final Random random = new Random();

	private final MessagesReader mr;

	private final int minLength;
	private final int maxLength;

	private final int minStreak;
	private final int maxStreak;

	private final List<Word> requiredWords;
	private Predicate<Long> allowedEmojis = null;
	private int minRequiredWords;

	public SentenceGenerator(MessagesReader mr, int minLength, int maxLength, int minStreak, int maxStreak) {
		this(mr, minLength, maxLength, minStreak, maxStreak, null, 0);
	}

	public SentenceGenerator(MessagesReader mr, int minLength, int maxLength, int minStreak, int maxStreak, List<Word> requiredWords, int minRequiredWords) {
		this.mr = mr;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.minStreak = minStreak;
		this.maxStreak = maxStreak;

		this.requiredWords = requiredWords;
		if (requiredWords != null) {
			this.minRequiredWords = Math.min(requiredWords.size(), minRequiredWords);
		}
	}

	public void setAllowedEmojis(Predicate<Long> allowedEmojis) {
		this.allowedEmojis = allowedEmojis;
	}

	public Optional<String> generateRandom() {
		return generateRandom(null);
	}

	public Optional<String> generateRandom(String startMessage) {
		var start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start + 1000) { // timeout
			var message = tryGenerateRandom(startMessage);
			if (message.isPresent() && !message.orElseThrow().isBlank()) {
				return message;
			}
		}
		return Optional.empty();
	}

	public Optional<String> tryGenerateRandom() {
		return tryGenerateRandom(null);
	}

	public Optional<String> tryGenerateRandom(String startMessage) {
		var stack = new Stack<Step>();

		if (startMessage == null) {
			stack.add(new Step(null, 1, mr.getFirstWords()));
		} else {
			var words = startMessage.split("\\s+");

			Step previousStep = null;
			for (var word : words) {
				if (previousStep == null) {
					previousStep = new Step(null, 1, word);
				} else {
					previousStep = new Step(previousStep, previousStep.getDepth() + 1, word);
				}
			}
			if (previousStep != null) {
				var list = getRelatedWordsList(previousStep);
				if (list != null) {
					stack.add(new Step(previousStep, previousStep.getDepth() + 1, list));
				}
			}
		}

		// stack can not be initially empty
		if (stack.isEmpty()) {
			throw new IllegalStateException();
		}

		while (!stack.isEmpty()) {
			var currentStep = stack.peek();

			if (currentStep.chooseRandomWord()) {
				var depth = currentStep.getDepth();
				var parentStep = currentStep.getParent();

				// lower limit of user streak
				if (parentStep != null) {
					// if current user streak is 1 then that means the algorithm chose word
					// from a different user so check how long was the previous user streak
					if (parentStep.getUserStreak() < minStreak && currentStep.getUserStreak() == 1) {

						if (minLength < 50) {
							// start again
							return Optional.empty();
						} else {
							// go step back
							continue;
						}

					}
				}

				/*
				// upper limit of user streak
				if(currentStep.getUserStreak() > maxStreak){

					if (minLength < 50) {
						// start again
						return Optional.empty();
					} else {
						// go step back
						continue;
					}

				}
				*/

				if (depth >= minLength && depth <= maxLength) {
					if (currentStep.getWord().isLast()) {

						// check if generated sentence contains required words
						if (requiredWords != null) {
							if (currentStep.getRequiredWordsCount() < minRequiredWords) {
								return Optional.empty();
							}
						}

						return Optional.ofNullable(buildSentence(stack));
					}
				}

				if (depth < maxLength) {
					var list = getRelatedWordsList(currentStep);
					if (list != null) {
						stack.add(new Step(currentStep, currentStep.getDepth() + 1, list));
					}
				}
			} else {
				stack.pop();
			}

		}

		throw new RuntimeException("Could not generate sentence!");
	}

	private List<Word> getRelatedWordsList(Step currentStep) {
		var word = currentStep.getWord();

		var parentStep = currentStep.getParent();
		if (parentStep == null) {
			return mr.getRelatedTwoWords().getList(word);
		}

		var grandparentStep = parentStep.getParent();
		if (grandparentStep == null) {
			return mr.getRelatedThreeWords().getList(parentStep.getWord(), word);
		}

		return mr.getRelatedFourWords().getList(grandparentStep.getWord(), parentStep.getWord(), word);
	}

	private String buildSentence(Stack<Step> stack) {
		var sb = new StringBuilder();
		for (var step : stack) {
			var word = step.getWord();

			if (word.isEmoji()) {
				if (allowedEmojis != null) {
					if (!allowedEmojis.test(word.getEmojiIdLong())) {
						continue; // skip this emoji
					}
				}
			}

			if (!sb.isEmpty()) {
				sb.append(" ");
			}
			sb.append(word);
		}

		if (sb.isEmpty()) {
			return null;
		}
		return sb.toString();
	}

	private class Step {
		private final Step parent;

		// the algorithm first chooses words from important words list
		private final List<Word> importantWords = new ArrayList<>();
		private final List<Word> words = new ArrayList<>();

		private final Set<Long> usersBlacklist = new HashSet<>();

		private final int depth;
		private Word word;

		private int userStreak;
		private int requiredWordsCount;

		public Step(Step parent, int depth, String word) {
			this.parent = parent;
			this.depth = depth;
			this.word = Word.create(0, word);

			this.userStreak = 1;
			this.requiredWordsCount = 0;
		}

		public Step(Step parent, int depth, List<Word> words) {
			this.parent = parent;
			this.depth = depth;
			this.word = null;

			this.words.addAll(words);

			this.userStreak = 0;
			this.requiredWordsCount = 0;

			//blacklist users to limit user streak and avoid two streaks with same user
			if (parent != null) {
				Set<Long> set = null;

				if (parent.getUserStreak() >= maxStreak) { //if streak reaches limit
					//blacklist all authors of already used words (to end streak)
					set = parent.usersBlacklist;
				} else {
					//blacklist all authors of already used words except previous word authors (to allow streak)
					var grandparent = parent.getParent();
					if (grandparent != null) {
						set = grandparent.usersBlacklist;
					}
				}

				if (set != null) {
					var it = this.words.iterator();
					while (it.hasNext()) {
						var word = it.next();
						if (set.contains(word.getAuthorIdLong())) {
							it.remove();
						}
					}
				}
			}

			//move required words to important words list
			if (requiredWords != null) {
				var it = this.words.iterator();
				while (it.hasNext()) {
					var word = it.next();
					if (requiredWords.contains(word)) {
						//Moving already used words to important words list causes many duplicates. Do not do this!
						if (!isWordUsed(word)) {
							it.remove();
							importantWords.add(word);
						}
					}
				}
			}
		}

		private boolean isWordUsed(Word word) {
			if (this.word != null) {
				if (this.word.equals(word)) {
					return true;
				}
			}
			if (this.parent != null) {
				return this.parent.isWordUsed(word);
			}
			return false;
		}

		public Step getParent() {
			return parent;
		}

		public int getDepth() {
			return depth;
		}

		public Word getWord() {
			return word;
		}

		public int getUserStreak() {
			return userStreak;
		}

		public int getRequiredWordsCount() {
			return requiredWordsCount;
		}

		public boolean chooseRandomWord() {
			if (words.isEmpty()) {
				return false;
			}

			// choose random word, first choose from important words list
			if (importantWords.isEmpty()) {
				word = words.remove(random.nextInt(words.size()));
			} else {
				word = importantWords.remove(random.nextInt(importantWords.size()));
			}

			// update user blacklist
			usersBlacklist.clear();
			if (parent != null) {
				usersBlacklist.addAll(parent.usersBlacklist);
			}
			usersBlacklist.add(word.getAuthorIdLong());

			// update user streak
			if (parent == null) {
				userStreak = 1;
			} else {
				if (parent.getWord().getAuthorIdLong() == word.getAuthorIdLong()) {
					userStreak = parent.getUserStreak() + 1;
				} else {
					userStreak = 1;
				}
			}

			if (requiredWords != null) {
				int parentRequiredWordsCount;
				if (parent == null) {
					parentRequiredWordsCount = 0;
				} else {
					parentRequiredWordsCount = parent.getRequiredWordsCount();
				}
				if (requiredWords.contains(word)) {
					requiredWordsCount = parentRequiredWordsCount + 1;
				} else {
					requiredWordsCount = parentRequiredWordsCount;
				}
			}


			return true;
		}

	}
}
