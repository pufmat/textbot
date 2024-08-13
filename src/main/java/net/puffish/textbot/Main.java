package net.puffish.textbot;

import net.puffish.textbot.message.MessageGenerator;
import net.puffish.textbot.message.MessagesReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

public final class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	// setting larger limit allows generating more complex
	// messages but increases memory usage and startup time
	private static final int SENTENCES_LIMIT = 100000;

	public static void main(String[] args) throws Exception {
		var token = Objects.requireNonNull(System.getenv("TOKEN"));
		var messagesPath = Path.of(System.getenv("MESSAGES_PATH"));

		LOGGER.info("Reading messages...");
		var start = System.currentTimeMillis();
		var mr = new MessagesReader(SENTENCES_LIMIT);
		mr.read(messagesPath);
		var stop = System.currentTimeMillis();
		LOGGER.info("Done in {}s!", Math.round((stop - start) / 1000D));

		mr.free();

		LOGGER.info("Caching sentences...");
		start = System.currentTimeMillis();
		mr.cache();
		stop = System.currentTimeMillis();
		LOGGER.info("Done in {}s!", Math.round((stop - start) / 1000D));

		LOGGER.info("Generating relations...");
		start = System.currentTimeMillis();
		mr.generateRelatedWords();
		stop = System.currentTimeMillis();
		LOGGER.info("Done in {}s!", Math.round((stop - start) / 1000D));

		LOGGER.info("Loaded {} sentences.", mr.getSentences().size());
		LOGGER.info("Skipped {} sentences.", mr.getSkippedCount());
		LOGGER.info("Loaded {} first words.", mr.getFirstWords().size());
		LOGGER.info("Loaded {} groups of 2 words.", mr.getRelatedTwoWords().getCount());
		LOGGER.info("Loaded {} groups of 3 words.", mr.getRelatedThreeWords().getCount());
		LOGGER.info("Loaded {} groups of 4 words.", mr.getRelatedFourWords().getCount());
		LOGGER.info("Reduced {} strings to {}.", mr.getStringCache().getRegisteredCount(), mr.getStringCache().getCachedCount());

		var mg = new MessageGenerator(mr);

		var bot = new Bot(mg, token);
		bot.start();
	}
}
