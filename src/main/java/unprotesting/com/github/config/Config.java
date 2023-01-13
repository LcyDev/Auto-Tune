package unprotesting.com.github.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import unprotesting.com.github.AutoTune;
import unprotesting.com.github.util.AutoTuneLogger;
import unprotesting.com.github.util.Format;

/**
 * The class for loading and storing the configuration options.
 */
@Getter
public class Config {

    // The static instance of the config.
    private static Config config;
    // The list of config filenames.
    private static final String[] filenames = { "config.yml", "shops.yml",
        "playerdata.yml", "messages.yml" };
    // The list of files.
    private static File[] files;
    // The list of configs.
    private static YamlConfiguration[] configs;

    private final double timePeriod;
    private final double volatility;
    private final double sellPriceDifference;
    private final boolean durabilityFunction;
    private final Integer minimumPlayers;
    private final double interest;
    private final double tutorialUpdate;
    private final boolean webServer;
    private final Integer port;
    private final String background;
    private final String logLevel;
    private final String locale;
    private final boolean enableSellLimits;
    private final boolean enableLoans;

    private final String notInShop;
    private final String notEnoughMoney;
    private final String notEnoughSpace;
    private final String notEnoughItems;
    private final String notUnlocked;
    private final String runOutOfBuys;
    private final String runOutOfSells;
    private final String shopPurchase;
    private final String shopSell;
    private final String holdItemInHand;
    private final String enchantmentError;
    private final String autosellProfit;
    private final String invalidShopSection;
    private final String backgroundPaneText;

    private final List<String> shopLore;
    private final List<String> shopGdpLore;
    private final List<String> purchaseBuyLore;
    private final List<String> purchaseSellLore;
    private final List<String> autosellLore;
    private final List<String> help;
    private final List<String> adminHelp;
    private final List<String> tutorial;

    private final ConfigurationSection shops;
    private final ConfigurationSection sections;
    private ConfigurationSection autosell;

    /**
     * Initializes the config files.
     */
    public static void init() {
        try {
            initializeConfig();
        } catch (Exception e) {
            Format.getLog().info("Failed to initialize config.");
            Format.getLog().config(e.toString());
        }
    }

    private static void initializeConfig() {
        files = new File[filenames.length];
        configs = new YamlConfiguration[filenames.length];
        config = new Config();
    }

    /**
     * Gets the config.
     *
     * @return the config object
     */
    public static Config get() {
        return config;
    }

    private Config() {
        AutoTune instance = AutoTune.getInstance();
        for (int i = 0; i < filenames.length; i++) {
            if (!new File(instance.getDataFolder(), filenames[i]).exists()) {
                instance.saveResource(filenames[i], false);
            }
        }

        saveWebFolder();

        for (int i = 0; i < filenames.length; i++) {
            File file = new File(instance.getDataFolder(), filenames[i]);
            if (!file.exists()) {
                instance.getLogger().info("Failed to load config file: " + filenames[i]);
                continue;
            }
            files[i] = file;
            configs[i] = YamlConfiguration.loadConfiguration(files[i]);
        }

        this.logLevel = configs[0].getString("log-level", "INFO");
        Format.loadLogger(Level.parse(logLevel));
        AutoTuneLogger logger = Format.getLog();

        this.timePeriod = configs[0].getDouble("time-period", 30);
        logger.finer("Time period: " + timePeriod);
        this.volatility = configs[0].getDouble("volatility", 0.5);
        logger.finer("Volatility: " + volatility);
        this.sellPriceDifference = configs[0].getDouble("sell-price-difference", 20);
        logger.finer("Sell price difference: " + sellPriceDifference);
        this.durabilityFunction = configs[0].getBoolean("durability-function", true);
        logger.finer("Durability function: " + durabilityFunction);
        this.minimumPlayers = configs[0].getInt("minimum-players", 2);
        logger.finer("Minimum players: " + minimumPlayers);
        this.interest = configs[0].getDouble("interest", 0.05);
        logger.finer("Interest: " + interest);
        this.tutorialUpdate = configs[0].getDouble("tutorial-update", 300);
        logger.finer("Tutorial update: " + tutorialUpdate);
        this.webServer = configs[0].getBoolean("web-server", true);
        logger.finer("Web-Server: " + webServer);
        this.port = configs[0].getInt("port", 8989);
        logger.finer("Port: " + port);
        this.background = configs[0].getString("background", "BLACK_STAINED_GLASS_PANE");
        logger.finer("Background: " + background);
        this.locale = configs[0].getString("locale", "en_US");
        Format.loadLocale(this.locale);
        logger.finer("Locale: " + locale);
        this.enableSellLimits = configs[0].getBoolean("enable-sell-limits", false);
        logger.finer("Skip Max Limits: " + enableSellLimits);
        this.enableLoans = configs[0].getBoolean("enable-loans", false);
        logger.finer("Loans Enabled: " + enableLoans);

        this.notInShop = configs[3].getString("not-in-shop");
        logger.finest("Not in shop: " + notInShop);
        this.notEnoughMoney = configs[3].getString("not-enough-money");
        logger.finest("Not enough money: " + notEnoughMoney);
        this.notEnoughSpace = configs[3].getString("not-enough-space");
        logger.finest("Not enough space: " + notEnoughSpace);
        this.notEnoughItems = configs[3].getString("not-enough-items");
        logger.finest("Not enough items: " + notEnoughItems);
        this.notUnlocked = configs[3].getString("not-unlocked");
        logger.finest("Not unlocked: " + notUnlocked);
        this.runOutOfBuys = configs[3].getString("run-out-of-buys");
        logger.finest("Run out of buys: " + runOutOfBuys);
        this.runOutOfSells = configs[3].getString("run-out-of-sells");
        logger.finest("Run out of sells: " + runOutOfSells);
        this.shopPurchase = configs[3].getString("shop-purchase");
        logger.finest("Shop purchase: " + shopPurchase);
        this.shopSell = configs[3].getString("shop-sell");
        logger.finest("Shop sell: " + shopSell);
        this.holdItemInHand = configs[3].getString("hold-item-in-hand");
        logger.finest("Hold item in hand: " + holdItemInHand);
        this.enchantmentError = configs[3].getString("enchantment-error");
        logger.finest("Enchantment error: " + enchantmentError);
        this.autosellProfit = configs[3].getString("autosell-profit");
        logger.finest("Autosell profit: " + autosellProfit);
        this.invalidShopSection = configs[3].getString("invalid-shop-section");
        logger.finest("Invalid shop section: " + invalidShopSection);
        this.backgroundPaneText = configs[3].getString("background-pane-text", "<obf>|</obf>");
        logger.finest("Background pane text: " + backgroundPaneText);

        this.shopLore = configs[3].getStringList("shop-lore");
        logger.finest("Shop lore: " + Arrays.toString(shopLore.toArray()));
        this.shopGdpLore = configs[3].getStringList("shop-gdp-lore");
        logger.finest("GDP shop lore: " + Arrays.toString(shopGdpLore.toArray()));
        this.purchaseBuyLore = configs[3].getStringList("purchase-buy-lore");
        logger.finest("Purchase buy lore: " + Arrays.toString(purchaseBuyLore.toArray()));
        this.purchaseSellLore = configs[3].getStringList("purchase-sell-lore");
        logger.finest("Purchase sell lore: " + Arrays.toString(purchaseSellLore.toArray()));
        this.autosellLore = configs[3].getStringList("autosell-lore");
        logger.finest("Autosell lore: " + Arrays.toString(autosellLore.toArray()));
        this.help = configs[3].getStringList("help");
        logger.finest("Tutorial: " + Arrays.toString(help.toArray()));
        this.adminHelp = configs[3].getStringList("admin-help");
        logger.finest("Tutorial: " + Arrays.toString(adminHelp.toArray()));
        this.tutorial = configs[3].getStringList("tutorial");
        logger.finest("Tutorial: " + Arrays.toString(tutorial.toArray()));

        this.shops = configs[1].getConfigurationSection("shops");
        logger.finer("Loaded shops configuration.");
        this.sections = configs[1].getConfigurationSection("sections");
        logger.finer("Loaded sections configuration.");

        this.autosell = configs[2].getConfigurationSection("autosell");
        logger.finer("Loaded autosell configuration.");
    }

    /**
     * Set a new configuration section for autosell data and save it.
     *
     * @param section The new section.
     */
    public void setAutosell(@NotNull ConfigurationSection section) {
        this.autosell = section;
        configs[2] = new YamlConfiguration();
        configs[2].set("autosell", section);
        try {
            configs[2].save(files[2]);
        } catch (IOException e) {
            Format.getLog().severe("Could not save playerdata configuration.");
            Format.getLog().config(e.toString());
        }
    }

    private void saveWebFolder() {
        AutoTune instance = AutoTune.getInstance();
        File webFolder = new File(instance.getDataFolder(), "web");
        if (!webFolder.exists()) {
            webFolder.mkdir();
        }
        File buildFolder = new File(webFolder, "build");
        if (!buildFolder.exists()) {
            buildFolder.mkdir();
        }
        String[] files = { "index.html", "favicon.png",
            "global.css", "build/bundle.js", "build/bundle.js.map" };
        for (String file : files) {
            File webFile = new File(webFolder, file);
            if (!webFile.exists()) {
                instance.saveResource("web/" + file, true);
            }
        }
    }
}
