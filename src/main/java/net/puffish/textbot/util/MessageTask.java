package net.puffish.textbot.util;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public final class MessageTask {
	private final Message referenceMessage;
	private final long creationTime;

	public MessageTask(Message referenceMessage) {
		this.referenceMessage = referenceMessage;
		this.creationTime = System.currentTimeMillis();
	}

	public MessageChannelUnion getChannel() {
		return referenceMessage.getChannel();
	}

	public String getReferenceMessageContent() {
		return referenceMessage.getContentRaw();
	}

	public Message getReferenceMessage() {
		return referenceMessage;
	}

	public long getCreationTime() {
		return creationTime;
	}

}
