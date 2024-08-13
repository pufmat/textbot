package net.puffish.textbot.util;

public class ChannelState {
	private long messages = 0;
	private long previousTime = 0;

	public long getMessages() {
		return messages;
	}

	public void setMessages(long messages) {
		this.messages = messages;
	}

	public long getPreviousTime() {
		return previousTime;
	}

	public void setPreviousTime(long previousTime) {
		this.previousTime = previousTime;
	}
}
