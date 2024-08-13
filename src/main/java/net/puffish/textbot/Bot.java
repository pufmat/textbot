package net.puffish.textbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.puffish.textbot.message.MessageGenerator;
import net.puffish.textbot.util.ChannelState;
import net.puffish.textbot.util.MessageTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class Bot extends ListenerAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);

	private final MessageGenerator mg;
	private final JDA jda;

	private final ConcurrentHashMap<Long, ChannelState> channelsStates = new ConcurrentHashMap<>();
	private final Deque<String> messagesQueue = new LinkedList<>();
	private final Deque<MessageTask> tasksQueue = new LinkedList<>();
	private final Random random = new Random();
	private final Object lock = new Object();

	public Bot(MessageGenerator mg, String token) {
		this.mg = mg;
		this.jda = JDABuilder.createLight(token,
						GatewayIntent.GUILD_MESSAGES,
						GatewayIntent.GUILD_EMOJIS_AND_STICKERS
				)
				.enableCache(CacheFlag.EMOJI)
				.addEventListeners(this)
				.build();
	}

	public void start() {
		this.jda.updateCommands()
				.addCommands(Commands.slash("info", "Shows information about the bot"))
				.queue();

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				for (var state : channelsStates.values()) {
					state.setMessages(Math.max(0, state.getMessages() - 1));
				}
			}
		}, 0, 10 * 1000);

		new Thread(() -> {
			while (true) {
				synchronized (lock) {
					if (messagesQueue.size() < 5) {
						mg.getRandomMessage(getEmojiPredicate()).ifPresent(messagesQueue::addLast);
					} else if (!tasksQueue.isEmpty()) {
						var task = tasksQueue.removeFirst();
						if (System.currentTimeMillis() < task.getCreationTime() + 10 * 1000) {
							var referenceMessage = task.getReferenceMessageContent();
							var channel = task.getChannel();

							mg.getRandomReplyMessage(getEmojiPredicate(), referenceMessage).ifPresent(generatedMessage -> {
								sendReplyMessage(channel, generatedMessage, task.getReferenceMessage());
								LOGGER.debug("Sending reply message on channel: {} message: {}", channel.getId(), generatedMessage);
							});
						}
					} else {
						try {
							lock.wait();
						} catch (InterruptedException ignored) {
						}
					}
				}
			}
		}).start();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!event.isFromGuild()) {
			return;
		}

		var message = event.getMessage();
		var user = message.getAuthor();
		if (user.isBot()) {
			return;
		}

		var channel = event.getGuildChannel();
		var channelId = message.getIdLong();

		// respond ping
		if (message.getMentions().getUsers().stream().anyMatch(u -> u.equals(jda.getSelfUser()))) {
			scheduleReplyMessage(message);
			return;
		}

		// respond reference
		var referenced = message.getReferencedMessage();
		if (referenced != null) {
			if (referenced.getAuthor().equals(jda.getSelfUser())) {
				scheduleReplyMessage(message);
				return;
			}
		}

		// respond other
		var state = channelsStates.computeIfAbsent(channelId, k -> new ChannelState());

		state.setMessages(state.getMessages() + 1);

		var currentTime = System.currentTimeMillis();
		if (state.getMessages() > 15 && state.getPreviousTime() < currentTime) {
			state.setMessages(0);
			state.setPreviousTime(currentTime + random.nextInt(60, 90) * 1000L);
			sendRandomMessage(channel);
		}
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		switch (event.getName()) {
			case "info" -> {
				var mcb = new MessageCreateBuilder();
				mcb.addComponents(ActionRow.of(Button.primary("refresh_info", "Refresh")));
				mcb.addEmbeds(createInfoEmbed());
				event.reply(mcb.build()).queue();
			}
		}
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		switch (event.getComponentId()) {
			case "refresh_info" -> event.editMessageEmbeds(createInfoEmbed()).queue();
		}
	}

	private MessageEmbed createInfoEmbed() {
		var eb = new EmbedBuilder();
		eb.setColor(0x7af300);
		eb.setTitle("Information");
		eb.setFooter("Updated");
		eb.setTimestamp(Instant.now());

		var servers = jda.getGuilds().size();
		var emojis = jda.getEmojiCache().size();

		eb.addField("Servers", String.valueOf(servers), false);
		eb.addField("Emojis", String.valueOf(emojis), false);
		eb.addField("Ping", jda.getGatewayPing() + "ms", false);

		return eb.build();
	}

	private Predicate<Long> getEmojiPredicate() {
		return id -> jda.getEmojiCache().getElementById(id) != null;
	}

	private void scheduleReplyMessage(Message message) {
		if (message.getContentStripped().isBlank()) {
			return;
		}

		synchronized (lock) {
			tasksQueue.add(new MessageTask(message));

			lock.notify();
		}
	}

	private void sendRandomMessage(MessageChannel channel) {
		synchronized (lock) {
			if (messagesQueue.isEmpty()) {
				LOGGER.debug("Messages queue is empty!");
				return;
			}

			var message = messagesQueue.removeFirst();
			sendMessage(channel, message);
			LOGGER.debug("Sending message on channel: {} message: {}", channel.getId(), message);

			lock.notify();
		}
	}

	private void sendMessage(MessageChannel channel, String message) {
		channel.sendTyping().queue();
		channel.sendMessage(message).queueAfter(3, TimeUnit.SECONDS);
	}

	private void sendReplyMessage(MessageChannel channel, String message, Message reference) {
		channel.sendTyping().queue();
		var mcb = new MessageCreateBuilder();
		mcb.setContent(message);
		reference.reply(mcb.build()).queueAfter(3, TimeUnit.SECONDS);
	}

}
