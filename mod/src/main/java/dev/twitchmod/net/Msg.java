package dev.twitchmod.net;

/** Small serialisable text: translation key plus integer arguments. */
public class Msg {
    public String key;
    public int[] args;

    public Msg() {
    }

    public Msg(String key, int... args) {
        this.key = key;
        this.args = args;
    }

    public static Msg of(String key) {
        return new Msg(key);
    }

    public static Msg of(String key, int arg) {
        return new Msg(key, arg);
    }
}
