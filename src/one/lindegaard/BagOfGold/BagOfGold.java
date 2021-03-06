package one.lindegaard.BagOfGold;

import java.io.File;

import one.lindegaard.BagOfGold.bank.BankSign;
import one.lindegaard.BagOfGold.commands.CommandDispatcher;
import one.lindegaard.BagOfGold.commands.ConvertCommand;
import one.lindegaard.BagOfGold.commands.DebugCommand;
import one.lindegaard.BagOfGold.commands.ReloadCommand;
import one.lindegaard.BagOfGold.commands.UpdateCommand;
import one.lindegaard.BagOfGold.commands.VersionCommand;
import one.lindegaard.BagOfGold.storage.DataStoreException;
import one.lindegaard.BagOfGold.storage.DataStoreManager;
import one.lindegaard.BagOfGold.storage.IDataStore;
import one.lindegaard.BagOfGold.storage.MySQLDataStore;
import one.lindegaard.BagOfGold.storage.SQLiteDataStore;
import one.lindegaard.BagOfGold.update.Updater;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

public class BagOfGold extends JavaPlugin {

	private static BagOfGold instance;

	private Messages mMessages;
	private MetricsManager mMetricsManager;
	private ConfigManager mConfig;
	private CommandDispatcher mCommandDispatcher;
	private ServicesManager mServiceManager;
	private PlayerSettingsManager mPlayerSettingsManager;
	private IDataStore mStore;
	private DataStoreManager mStoreManager;
	private EconomyManager mEconomyManager;

	private boolean mInitialized = false;

	@Override
	public void onLoad() {
	}

	@Override
	public void onEnable() {

		instance = this;

		mServiceManager = Bukkit.getServicesManager();

		mMessages = new Messages(this);
		mMessages.exportDefaultLanguages(this);

		mConfig = new ConfigManager(this, new File(getDataFolder(), "config.yml"));

		if (mConfig.loadConfig())
			mConfig.saveConfig();
		else
			throw new RuntimeException(instance.getMessages().getString("bagofgold.config.fail"));

		if (isbStatsEnabled())
			instance.getMessages().debug("bStat is enabled");
		else {
			Bukkit.getConsoleSender().sendMessage(
					ChatColor.RED + "[BagOfGold]=====================WARNING=============================");
			Bukkit.getConsoleSender()
					.sendMessage(ChatColor.RED + "The statistics collection is disabled. As developer I need the");
			Bukkit.getConsoleSender()
					.sendMessage(ChatColor.RED + "statistics from bStats.org. The statistics is 100% anonymous.");
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "https://bstats.org/plugin/bukkit/bagofgold");
			Bukkit.getConsoleSender().sendMessage(
					ChatColor.RED + "Please enable this in /plugins/bStats/config.yml and get rid of this");
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "message. Loading will continue in 15 sec.");
			Bukkit.getConsoleSender().sendMessage(
					ChatColor.RED + "[BagOfGold]=========================================================");
			long now = System.currentTimeMillis();
			while (System.currentTimeMillis() < now + 15000L) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}

		Updater.setCurrentJarFile(this.getFile().getName());

		// Register commands
		mCommandDispatcher = new CommandDispatcher(this, "bagofgold",
				instance.getMessages().getString("bagofgold.command.base.description") + getDescription().getVersion());
		getCommand("bagofgold").setExecutor(mCommandDispatcher);
		getCommand("bagofgold").setTabCompleter(mCommandDispatcher);
		mCommandDispatcher.registerCommand(new ReloadCommand(this));
		mCommandDispatcher.registerCommand(new UpdateCommand(this));
		mCommandDispatcher.registerCommand(new VersionCommand(this));
		mCommandDispatcher.registerCommand(new DebugCommand(this));
		mCommandDispatcher.registerCommand(new ConvertCommand(this));

		// Check for new MobHuntig updates
		new Updater(this).hourlyUpdateCheck(getServer().getConsoleSender(), mConfig.updateCheck, false);

		if (mConfig.databaseType.equalsIgnoreCase("mysql"))
			mStore = new MySQLDataStore(this);
		else
			mStore = new SQLiteDataStore(this);

		try {
			mStore.initialize();
		} catch (DataStoreException e) {
			e.printStackTrace();
			try {
				mStore.shutdown();
			} catch (DataStoreException e1) {
				e1.printStackTrace();
			}
			setEnabled(false);
			return;
		}

		// Check for new BagOfGold updates
		Updater.setCurrentJarFile(this.getFile().getName());

		mStoreManager = new DataStoreManager(this, mStore);

		mPlayerSettingsManager = new PlayerSettingsManager(this);

		mEconomyManager = new EconomyManager(this);

		if (!getServer().getName().toLowerCase().contains("glowstone")) {
			mMetricsManager = new MetricsManager(this);
			mMetricsManager.startMetrics();

			mMetricsManager.startBStatsMetrics();
		}

		// Initialize BagOfGold Bank Signs
		new BankSign(this);

		// Try to load BagOfGold
		hookEconomy(Economy_BagOfGold.class, ServicePriority.Normal, "one.lindegaard.BagOfGold.BagOfGoldEconomy");

		mInitialized = true;

	}

	@Override
	public void onDisable() {
		if (!mInitialized)
			return;
		instance.getMessages().debug("BagOfGold disabled.");
	}

	private boolean isbStatsEnabled() {
		File bStatsFolder = new File(instance.getDataFolder().getParentFile(), "bStats");
		File configFile = new File(bStatsFolder, "config.yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		return config.getBoolean("enabled", true);
	}

	// ************************************************************************************
	// Hook into Vault / Economy
	// ************************************************************************************

	private void hookEconomy(Class<? extends Economy> hookClass, ServicePriority priority, String... packages) {
		try {
			if (packagesExists(packages)) {
				Economy economy = hookClass.getConstructor(Plugin.class).newInstance(this);
				mServiceManager.register(Economy.class, economy, Bukkit.getPluginManager().getPlugin("Vault"),
						ServicePriority.Normal);
				Bukkit.getLogger().info(String.format("[BagOfGold][Economy] BagOfGold found: %s",
						economy.isEnabled() ? "Loaded" : "Waiting"));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format(
					"[BagOfGold][Economy] There was an error hooking BagOfGold - check to make sure you're using a compatible version!"));
		}
	}

	/**
	 * Determines if all packages in a String array are within the Classpath
	 * This is the best way to determine if a specific plugin exists and will be
	 * loaded. If the plugin package isn't loaded, we shouldn't bother waiting
	 * for it!
	 * 
	 * @param packages
	 *            String Array of package names to check
	 * @return Success or Failure
	 */
	private static boolean packagesExists(String... packages) {
		try {
			for (String pkg : packages) {
				Class.forName(pkg);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// ************************************************************************************
	// Managers and handlers
	// ************************************************************************************
	public static BagOfGold getInstance() {
		return instance;
	}

	public ConfigManager getConfigManager() {
		return mConfig;
	}

	/**
	 * Get the MessagesManager
	 * 
	 * @return
	 */
	public Messages getMessages() {
		return mMessages;
	}

	public CommandDispatcher getCommandDispatcher() {
		return mCommandDispatcher;
	}

	/**
	 * Gets the Store Manager
	 * 
	 * @return
	 */
	public IDataStore getStoreManager() {
		return mStore;
	}

	/**
	 * Gets the Database Store Manager
	 * 
	 * @return
	 */
	public DataStoreManager getDataStoreManager() {
		return mStoreManager;
	}

	/**
	 * Get the PlayerSettingsManager
	 * 
	 * @return
	 */
	public PlayerSettingsManager getPlayerSettingsManager() {
		return mPlayerSettingsManager;
	}

	/**
	 * Get the EconomyManager
	 * 
	 * @return
	 */
	public EconomyManager getEconomyManager() {
		return mEconomyManager;
	}

	public static BagOfGold getApi() {
		return instance;
	}

}
