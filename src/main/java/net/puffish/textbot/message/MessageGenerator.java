package net.puffish.textbot.message;

import net.puffish.textbot.sentence.ReplySentenceGenerator;
import net.puffish.textbot.sentence.SentenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

public class MessageGenerator {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageGenerator.class);

	private final MessagesReader mr;

	public MessageGenerator(MessagesReader mr) {
		this.mr = mr;
	}

	public Optional<String> getRandomMessage(Predicate<Long> emojis) {
		var sg = new SentenceGenerator(mr, 9, 12, 1, 3);
		sg.setAllowedEmojis(emojis);
		return sg.generateRandom();
	}

	public Optional<String> getRandomReplyMessage(Predicate<Long> emojis, String message) {
		var rwg = new ReplySentenceGenerator(mr.getSentences());
		if (!rwg.generate(message)) {
			return Optional.empty();
		}

		var random = new Random();

		var start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start + 1000) { // timeout
			var requiredWords = rwg.getRandomWords(random);
			if (requiredWords.isEmpty()) {
				continue;
			}

			var sg = new SentenceGenerator(mr, random.nextInt(5) + 1, 12, 1, 3, requiredWords, 2);
			sg.setAllowedEmojis(emojis);

			var result = sg.tryGenerateRandom();
			if (result.isPresent()) {
				LOGGER.debug("Reply message generation took: {}ms!", System.currentTimeMillis() - start);
				return result;
			}
		}

		LOGGER.debug("Reply message generation timed out!");
		return Optional.empty();
	}
}
