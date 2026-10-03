package dev.twitchmod.director;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registry of pending player answers (chat vote, risk/safe, appeal, exam).
 * Lives on the server only; the client can only send an index.
 */
public final class Choices {
    private static final Map<String, Consumer<Integer>> PENDING = new HashMap<>();

    private Choices() {
    }

    public static void offer(String id, Consumer<Integer> handler) {
        PENDING.put(id, handler);
    }

    public static void resolve(String id, int index) {
        Consumer<Integer> handler = PENDING.remove(id);
        if (handler != null) handler.accept(index);
    }

    public static boolean pending(String id) {
        return PENDING.containsKey(id);
    }

    public static void expire(String id) {
        PENDING.remove(id);
    }

    public static void clear() {
        PENDING.clear();
    }
}
