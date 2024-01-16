package unprotesting.com.github.data;

import java.util.HashMap;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCompressionWrapper;
import unprotesting.com.github.AutoTune;
import unprotesting.com.github.config.Config;
import unprotesting.com.github.config.CsvHandler;
import unprotesting.com.github.util.AutoTuneLogger;
import unprotesting.com.github.util.EconomyUtil;
import unprotesting.com.github.util.Format;

/**
 * The database for the plugin.
 */
public class Database {

    private static final String[] ECONOMY_DATA_KEYS = {
        "GDP", "BALANCE", "DEBT", "LOSS", "INFLATION", "POPULATION" };

    private static Database instance;

    // The MapDB database.
    private DB db;
    // The map of item name to shop.
    protected HTreeMap<String, Shop> shops;
    // The map of times to Transactions
    @Getter
    protected HTreeMap<Long, Transaction> transactions;
    // The map of times to Loans
    @Getter
    protected HTreeMap<Long, Loan> loans;
    // The map of economy data name to economy data history.
    protected HTreeMap<String, double[]> economyData;
    // The map of section name to section.
    protected HashMap<String, Section> sections;
    // The map of a pair of shop names to a relation.
    protected HashMap<Pair<String, String>, Relation> relations;

    /**
     * Constructor for the Database class.
     */
    public Database() {
        instance = this;
        AutoTune plugin = AutoTune.getInstance();
        createDb(plugin.getDataFolder() + "/data.db");
        this.sections = new HashMap<String, Section>();
        createMaps();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            loadShopDefaults();
            updateChanges();
            loadSectionData();
            loadEconomyData();
            CsvHandler.writePriceData();
        });
    }

    /**
     * Get the static instance of the database.
     *
     * @return The static instance of the database.
     */
    public static Database get() {
        return instance;
    }

    /**
     * Close the database.
     */
    public void close() {
        if (db != null) {
            db.close();
        } else {
            Format.getLog().warning("Database is already closed.");
        }
    }

    /**
     * Reloads the database.
     */
    public void reload() {
        loadShopDefaults();
        updateChanges();
        loadSectionData();
        loadEconomyData();
        Bukkit.getScheduler().runTaskAsynchronously(AutoTune.getInstance(), () -> {
            CsvHandler.writePriceData();
        });
    }

    /**
     * Update the percentage changes for each shop.
     */
    public void updateChanges() {
        AutoTuneLogger logger = Format.getLog();
        for (String name : shops.keySet()) {
            Shop shop = getShop(name, true);
            shop.updateChange();
            putShop(name, shop);
            logger.finest(name + "'s change is now "
                    + Format.percent(getShop(name, true).getChange()));
        }
    }

    /**
     * Update the relations in the shop.
     */
    public void updateRelations() {
        for (String name : shops.keySet()) {
            for (String name2 : shops.keySet()) {
                if (name.equals(name2)) {
                    continue;
                }

                Pair<String, String> pair = Tuples.pair(name, name2);
                Relation relation = new Relation(getShop(name, true), getShop(name2, true));
                relations.put(pair, relation);
            }
        }
    }

    /**
     * Update a loan value.
     *
     * @param key  The key of the loan.
     * @param loan The loan to update.
     */
    public void updateLoan(Long key, Loan loan) {
        if (loans.containsKey(key)) {
            loans.put(key, loan);
        } else {
            Format.getLog().severe("Tried to update a loan that doesn't exist!");
        }
    }

    protected Shop getShop(String s, boolean warn) {
        String item = s.toLowerCase();

        if (shops.get(item) == null) {
            if (warn) Format.getLog().severe("Could not find shop for " + item);
            return null;
        }

        return shops.get(item);
    }

    protected void putShop(String key, Shop shop) {
        String name = key.toLowerCase();
        if (shops.containsKey(name)) {
            shops.put(name, shop);
        }
    }

    protected String[] getShopNames() {
        return shops.keySet().toArray(new String[0]);
    }

    protected int getPurchasesLeft(String item, UUID player, boolean isBuy) {
        Shop shop = getShop(item, true);
        int max = isBuy ? shop.getMaxBuys() : shop.getMaxSells();

        if (isBuy) {
            max -= shop.getRecentBuys().getOrDefault(player, 0);
        } else {
            max -= shop.getRecentSells().getOrDefault(player, 0);
        }

        return max;
    }

    protected boolean removeShop(String item) {
        String name = item.toLowerCase();
        if (shops.containsKey(name)) {
            shops.remove(name);
            return true;
        }
        return false;
    }

    private void createDb(String location) {
        db = DBMaker.fileDB(location)
                .checksumHeaderBypass()
                .fileMmapEnableIfSupported()
                .fileMmapPreclearDisable()
                .cleanerHackEnable()
                .allocateStartSize(10 * 1024 * 1024)
                .allocateIncrement(5 * 1024 * 1024)
                .closeOnJvmShutdown().make();
        db.getStore().fileLoad();
        db.getStore().compact();
        Format.getLog().config("Database initialized at " + location);
    }

    private void loadSectionData() {
        ConfigurationSection config = Config.get().getSections();
        AutoTuneLogger logger = Format.getLog();
        for (String key : config.getKeys(false)) {
            key = key.toLowerCase();
            ConfigurationSection section = config.getConfigurationSection(key);
            sections.put(key, new Section(key, section));
            logger.fine("Section " + key + " loaded.");
        }
    }

    private void loadShopDefaults() {
        ConfigurationSection config = Config.get().getShops();
        AutoTuneLogger logger = Format.getLog();
        for (String sectionName : config.getKeys(false)) {
            ConfigurationSection sectionConfig = config.getConfigurationSection(sectionName);

            for (String name : config.getConfigurationSection(sectionName).getKeys(false)) {
                String key = name.toLowerCase();
                Material material = Material.matchMaterial(key);
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key));
                boolean isEnchantment = enchantment != null;
                ConfigurationSection section = sectionConfig.getConfigurationSection(name);

                if (material == null && enchantment == null || section == null) {
                    logger.warning("Invalid shop. "
                            + key + " is not a valid material or enchantment.");
                    continue;
                }

                if (shops.containsKey(key)) {
                    Shop shop = getShop(key, true);
                    shop.loadConfiguration(section, sectionName);
                    shops.put(key, shop);
                    logger.finer("Shop " + key + " loaded.");
                    continue;
                }

                shops.put(key, new Shop(section, sectionName, isEnchantment));
                logger.fine("New shop " + key + " in section " + shops.get(key).getSection());
            }
        }
    }

    private void loadEconomyData() {
        if (economyData.isEmpty()) {
            for (String key : ECONOMY_DATA_KEYS) {
                economyData.put(key, new double[1]);
            }
        }

        EconomyDataUtil.updateEconomyData("INFLATION", calculateInflation());
        EconomyDataUtil.updateEconomyData("POPULATION", calculatePopulation());
        EconomyDataUtil.updateEconomyData("BALANCE", calculateBalance());
    }

    private double calculateInflation() {
        double inflation = 0;
        for (Shop shop : shops.values()) {
            inflation += shop.getChange();
        }
        inflation /= shops.size();
        return inflation;
    }

    private double calculatePopulation() {
        double population = 0;
        for (OfflinePlayer player : AutoTune.getInstance().getServer().getOfflinePlayers()) {
            if (player == null) {
                continue;
            }
            population++;
        }
        return population;
    }

    private double calculateBalance() {
        double balance = 0;
        for (OfflinePlayer player : AutoTune.getInstance().getServer().getOfflinePlayers()) {
            balance += EconomyUtil.getEconomy().getBalance(player);
        }
        return balance;
    }

    private void createMaps() {
        AutoTuneLogger logger = Format.getLog();
        this.shops = db.hashMap("shops")
                .keySerializer(new SerializerCompressionWrapper<String>(Serializer.STRING))
                .valueSerializer(new ShopSerializer())
                .createOrOpen();
        logger.fine("Loaded shops map.");
        this.transactions = db.hashMap("transactions")
                .keySerializer(new SerializerCompressionWrapper<Long>(Serializer.LONG))
                .valueSerializer(new TransactionSerializer())
                .createOrOpen();
        logger.fine("Loaded transactions map.");
        this.loans = db.hashMap("loans")
                .keySerializer(new SerializerCompressionWrapper<Long>(Serializer.LONG))
                .valueSerializer(new LoanSerializer())
                .createOrOpen();
        logger.fine("Loaded loans map.");
        this.economyData = db.hashMap("economyData")
                .keySerializer(new SerializerCompressionWrapper<String>(Serializer.STRING))
                .valueSerializer(Serializer.DOUBLE_ARRAY)
                .createOrOpen();
        logger.fine("Loaded economy data map.");
        this.relations = new HashMap<>();
    }

}
