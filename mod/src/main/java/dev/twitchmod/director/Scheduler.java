package dev.twitchmod.director;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Active-time scheduler. Every delayed task is bound to its event context, so a
 * cancelled event, a dimension change or a world reload cancels its tasks too.
 */
public final class Scheduler {
    private static final List<Task> TASKS = new ArrayList<>();

    private Scheduler() {
    }

    private static class Task {
        EventContext ctx;
        long at;
        int interval;
        Runnable run;
        boolean repeat;
    }

    public static void after(EventContext ctx, int ticks, Runnable run) {
        Task t = new Task();
        t.ctx = ctx;
        t.at = ctx.state.activeTicks + ticks;
        t.run = run;
        TASKS.add(t);
    }

    public static void repeat(EventContext ctx, int intervalTicks, Runnable run) {
        Task t = new Task();
        t.ctx = ctx;
        t.at = ctx.state.activeTicks + intervalTicks;
        t.interval = intervalTicks;
        t.run = run;
        t.repeat = true;
        TASKS.add(t);
    }

    public static void tick(long activeTick) {
        if (TASKS.isEmpty()) return;
        Iterator<Task> it = TASKS.iterator();
        List<Task> addLater = new ArrayList<>();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.at > activeTick) continue;
            try {
                t.run.run();
            } catch (Exception e) {
                System.out.println("[twitchmod][scheduler] " + e);
            }
            if (t.repeat) {
                t.at = activeTick + t.interval;
            } else {
                it.remove();
            }
        }
        TASKS.addAll(addLater);
    }

    public static void cancel(EventContext ctx) {
        TASKS.removeIf(t -> t.ctx == ctx);
    }

    public static void clearAll() {
        TASKS.clear();
    }
}
