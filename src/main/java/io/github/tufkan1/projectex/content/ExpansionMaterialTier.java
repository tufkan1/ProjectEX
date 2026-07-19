package io.github.tufkan1.projectex.content;

/** Stable item identities for the post-red-matter crafting chain. */
public enum ExpansionMaterialTier {
    MAGENTA("magenta"), PINK("pink"), PURPLE("purple"), VIOLET("violet"),
    BLUE("blue"), CYAN("cyan"), GREEN("green"), LIME("lime"),
    YELLOW("yellow"), ORANGE("orange"), WHITE("white");

    private final String id;

    ExpansionMaterialTier(String id) { this.id = id; }

    public String id() { return id; }
    public String fuelId() { return id + "_fuel"; }
    public String matterId() { return id + "_matter"; }
}
