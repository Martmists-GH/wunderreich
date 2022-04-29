package de.ambertation.wunderreich.config;

import de.ambertation.wunderreich.registries.WunderreichItems;

public class MainConfig extends ConfigFile {
    public final static String FEATURE_CATEGORY = "features";
    public final static String WUNDERKISTE_CATEGORY = "features.wunderkiste";

    public final BooleanValue doNotDespawnWithNameTag = new BooleanValue(FEATURE_CATEGORY,
                                                                         "doNotDespawnWithNameTag",
                                                                         true);

    public final BooleanValue allowTradesCycling = new BooleanValue(FEATURE_CATEGORY, "allowTradesCycling", true);

    public final BooleanValue allowLibrarianSelection = new BooleanValue(
            FEATURE_CATEGORY,
            "allowLibrarianSelection",
            true
    ).and(allowTradesCycling);

    public final BooleanValue cyclingNeedsWhisperer = new BooleanValue(
            FEATURE_CATEGORY,
            "cyclingNeedsWhisperer",
            true
    ).and(allowTradesCycling);

    public final BooleanValue allowBuilderTools = new BooleanValue(FEATURE_CATEGORY,
                                                                   "allowBuilderTools",
                                                                   true);

    public final BooleanValue addSlabs = new BooleanValue(FEATURE_CATEGORY,
                                                          "addSlabs",
                                                          true);


    public final BooleanValue wunderkisteRedstoneSignal = new BooleanValue(WUNDERKISTE_CATEGORY,
                                                                           "enableRedstoneSignal",
                                                                           true);

    public final BooleanValue wunderkisteRedstoneAnalog = new BooleanValue(WUNDERKISTE_CATEGORY,
                                                                           "enableAnalogRedstoneOutput",
                                                                           true);

    public MainConfig() {
        super("main");
        wunderkisteRedstoneSignal.hideInUI();
        wunderkisteRedstoneAnalog.hideInUI();
    }

    public boolean allowLibrarianSelection() {
        return allowLibrarianSelection.get()
                && Configs.ITEM_CONFIG.valueOf(WunderreichItems.BLANK_WHISPERER)
                && Configs.ITEM_CONFIG.valueOf(WunderreichItems.WHISPERER);
    }
}
