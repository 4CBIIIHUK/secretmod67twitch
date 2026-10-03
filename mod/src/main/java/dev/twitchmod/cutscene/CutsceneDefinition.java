package dev.twitchmod.cutscene;

/**
 * A cutscene is a timeline, not a stop-the-world event: 3–8 s for most scenes,
 * longer only for the intro and the finale.
 */
public class CutsceneDefinition {
    public static final String STORY = "story";
    public static final String DRAMA = "drama";
    public static final String ATMOSPHERE = "atmosphere";
    public static final String TWIST = "twist";

    public String id;
    public String type = STORY;
    public String title = "";
    public int durationSec = 5;
    /** static | zoom | orbit | crane | anvil */
    public String camera = "static";
    public boolean flash;
    public boolean shake;
    public String[] lines = new String[0];
    /** second index for each line */
    public int[] lineAt = new int[0];

    public CutsceneDefinition() {
    }

    public CutsceneDefinition(String id, String type, String title, int durationSec, String camera) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.durationSec = durationSec;
        this.camera = camera;
    }

    public CutsceneDefinition line(int atSec, String text) {
        String[] l = new String[lines.length + 1];
        System.arraycopy(lines, 0, l, 0, lines.length);
        l[lines.length] = text;
        lines = l;
        int[] t = new int[lineAt.length + 1];
        System.arraycopy(lineAt, 0, t, 0, lineAt.length);
        t[lineAt.length] = atSec;
        lineAt = t;
        return this;
    }

    public String lineAt(int second) {
        String current = null;
        for (int i = 0; i < lines.length; i++) {
            if (lineAt[i] <= second) current = lines[i];
        }
        return current;
    }

    public CutsceneDefinition flashing() {
        this.flash = true;
        return this;
    }

    public CutsceneDefinition shaky() {
        this.shake = true;
        return this;
    }
}
