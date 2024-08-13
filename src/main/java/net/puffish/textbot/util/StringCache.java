package net.puffish.textbot.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class StringCache {
	private final AtomicInteger registeredCount = new AtomicInteger(0);
	private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

	public String register(String string) {
		registeredCount.incrementAndGet();
		var previous = cache.putIfAbsent(string, string);
		return previous == null ? string : previous;
	}

	public int getRegisteredCount() {
		return registeredCount.get();
	}

	public int getCachedCount() {
		return cache.size();
	}
}
