package one.lindegaard.MobHunting.bounty;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import one.lindegaard.MobHunting.Messages;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.achievements.AchievementManager;
import one.lindegaard.MobHunting.rewards.CustomItems;
import one.lindegaard.MobHunting.storage.IDataCallback;
import one.lindegaard.MobHunting.storage.UserNotFoundException;
import one.lindegaard.MobHunting.util.Misc;

public class BountyManager implements Listener {

	private MobHunting instance;

	// mBounties contains all bounties on the OfflinePlayer and the Bounties put
	// on other players
	private static Set<Bounty> mOpenBounties = new HashSet<Bounty>();

	public BountyManager(MobHunting instance) {
		this.instance = instance;
		if (MobHunting.getConfigManager().enableRandomBounty) {
			Bukkit.getPluginManager().registerEvents(this, instance);
			Bukkit.getScheduler().runTaskTimer(instance, new Runnable() {
				public void run() {
					createRandomBounty();
				}
			}, MobHunting.getConfigManager().timeBetweenRandomBounties * 20 * 60,
					MobHunting.getConfigManager().timeBetweenRandomBounties * 20 * 60);
			Bukkit.getScheduler().runTaskTimer(MobHunting.getInstance(), new Runnable() {
				public void run() {
					for (Bounty bounty : mOpenBounties) {
						if (bounty.getEndDate() < System.currentTimeMillis() && bounty.isOpen()) {
							bounty.setStatus(BountyStatus.expired);
							MobHunting.getDataStoreManager().updateBounty(bounty);
							Messages.debug("BountyManager: Expired Bounty %s", bounty.toString());
							mOpenBounties.remove(bounty);
						}
					}
				}
			}, 600, 7200);
		}
	}

	public Set<Bounty> getAllBounties() {
		return mOpenBounties;
	}

	public Set<OfflinePlayer> getWantedPlayers() {
		Set<OfflinePlayer> wantedPlayers = new HashSet<OfflinePlayer>();
		for (Bounty b : mOpenBounties) {
			if (b.isOpen() && !wantedPlayers.contains(b.getWantedPlayer()))
				wantedPlayers.add(b.getWantedPlayer());
		}
		return wantedPlayers;
	}

	public Bounty getOpenBounty(String worldGroup, OfflinePlayer wantedPlayer, OfflinePlayer bountyOwner) {
		for (Bounty bounty : mOpenBounties) {
			if (!bounty.isOpen())
				continue;

			if (bounty.getBountyOwner() == null) {
				if (bountyOwner == null) {
					if (bounty.getWantedPlayer().equals(wantedPlayer) && bounty.getWorldGroup().equals(worldGroup)) {
						//Messages.debug("BountyManager: Found bounty: %s", bounty.toString());
						return bounty;
					}
				}
			} else {
				if (bounty.getBountyOwner().equals(bountyOwner) && bounty.getWantedPlayer().equals(wantedPlayer)
						&& bounty.getWorldGroup().equals(worldGroup)) {
					//Messages.debug("BountyManager: Found bounty: %s", bounty.toString());
					return bounty;
				}
			}

		}
		//Messages.debug("BountyManager: No open bounty found.");
		return null;
	}

	public Bounty getBounty(String worldGroup, OfflinePlayer wantedPlayer, OfflinePlayer bountyOwner) {
		for (Bounty bounty : mOpenBounties) {

			if (bounty.getBountyOwner() == null) {
				if (bountyOwner == null) {
					if (bounty.getWantedPlayer().equals(wantedPlayer) && bounty.getWorldGroup().equals(worldGroup)) {
						//Messages.debug("BountyManager: Found bounty: %s", bounty.toString());
						return bounty;
					}
				}
			} else {
				if (bounty.getBountyOwner().equals(bountyOwner) && bounty.getWantedPlayer().equals(wantedPlayer)
						&& bounty.getWorldGroup().equals(worldGroup)) {
					//Messages.debug("BountyManager: Found bounty: %s", bounty.toString());
					return bounty;
				}
			}

		}
		//Messages.debug("BountyManager: No bounty found.");
		return null;
	}

	public Bounty getOpenBounty(Bounty bounty) {
		for (Bounty b : mOpenBounties) {
			if (b.isOpen() && b.equals(bounty))
				return bounty;
		}
		return null;
	}

	public Set<Bounty> getOpenBounties(String worldGroup, OfflinePlayer wantedPlayer) {
		Set<Bounty> bounties = new HashSet<Bounty>();
		for (Bounty bounty : mOpenBounties) {
			if (bounty.isOpen() && bounty.getWantedPlayer().equals(wantedPlayer)
					&& bounty.getWorldGroup().equals(worldGroup)) {
				bounties.add(bounty);
			}
		}
		return bounties;
	}

	public void sort() {
		Set<Bounty> sortedSet = new TreeSet<Bounty>(new BountyComparator()).descendingSet();
		sortedSet.addAll(mOpenBounties);
		mOpenBounties = sortedSet;
	}

	class BountyComparator implements Comparator<Bounty> {
		@Override
		public int compare(Bounty b1, Bounty b2) {
			if (b1.equals(b2))
				return Double.compare(b1.getPrize(), b2.getPrize());
			else if (b1.getWantedPlayer().getName().equals(b2.getWantedPlayer().getName()))
				if (b1.getBountyOwner() == null)
					return -1;
				else if (b2.getBountyOwner() == null)
					return 1;
				else
					return b1.getBountyOwner().getName().compareTo(b2.getBountyOwner().getName());
			else
				return b1.getWantedPlayer().getName().compareTo(b2.getWantedPlayer().getName());
		}
	}

	// Tests
	public boolean hasOpenBounty(String worldGroup, OfflinePlayer wantedPlayer, OfflinePlayer bountyOwner) {
		int n = 0;
		for (Bounty bounty : mOpenBounties) {
			//Messages.debug("hasOpenBounty n=%s, testing: %s", n++, bounty.toString());

			if (!bounty.isOpen() || !bounty.getWorldGroup().equals(worldGroup)
					|| !bounty.getWantedPlayer().equals(wantedPlayer)) {
				//Messages.debug("hasOpenBounty (continue): %s", bounty.toString());
				continue;
			}

			if (bounty.getBountyOwner() == null)
				if (bountyOwner == null) {
					//Messages.debug("hasOpenBounty (true - both is null): %s", bounty.toString());
					return true;
				} else {
					//Messages.debug("hasOpenBounty (continue2): %s", bounty.toString());
					continue;
				}
			else {
				if (bounty.getBountyOwner().equals(bountyOwner))
					return true;
			}
		}
		//Messages.debug("hasOpenBounty (no bounty found)");
		return false;

	}

	public boolean hasOpenBounty(Bounty b) {
		for (Bounty bounty : mOpenBounties) {
			if (bounty.isOpen() && bounty.equals(b))
				return true;
		}
		return false;
	}

	public static boolean hasOpenBounties(String worldGroup, OfflinePlayer wantedPlayer) {
		for (Bounty bounty : mOpenBounties) {
			if (bounty.isOpen() && bounty.getWantedPlayer().equals(wantedPlayer))
				return true;
		}
		return false;
	}

	// ****************************************************************************
	// Events
	// ****************************************************************************

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(final PlayerJoinEvent event) {
		if (MobHunting.getConfigManager().disablePlayerBounties)
			return;

		Bukkit.getScheduler().runTaskLater(instance, new Runnable() {
			@Override
			public void run() {
				Player player = event.getPlayer();
				load(player);
			}
		}, (long) 5);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Set<Bounty> toBeRemoved = new HashSet<Bounty>();
		Iterator<Bounty> itr = getAllBounties().iterator();
		int n = 0;
		while (itr.hasNext()) {
			Bounty bounty = itr.next();
			if (bounty.getWantedPlayer().equals(event.getPlayer())) {
				toBeRemoved.add(bounty);
				n++;
			}
		}
		if (n > 0) {
			mOpenBounties.removeAll(toBeRemoved);
			Messages.debug("%s bounties on %s was removed when player quit", n, event.getPlayer().getName());
		}
	}

	// ****************************************************************************
	// Save & Load
	// ****************************************************************************
	public void load(final OfflinePlayer offlinePlayer) {
		MobHunting.getDataStoreManager().requestBounties(BountyStatus.open, offlinePlayer,
				new IDataCallback<Set<Bounty>>() {

					@Override
					public void onCompleted(Set<Bounty> data) {
						boolean sort = false;
						int n = 0;
						Iterator<Bounty> itr = data.iterator();
						while (itr.hasNext()) {
							Bounty bounty = itr.next();
							if (!hasOpenBounty(bounty.getWorldGroup(), bounty.getWantedPlayer(),
									bounty.getBountyOwner())) {
								if (bounty.getEndDate() > System.currentTimeMillis() && bounty.isOpen()) {
									mOpenBounties.add(bounty);
									n++;
								} else {
									Messages.debug("BountyManager: Expired onLoad Bounty %s", bounty.toString());
									bounty.setStatus(BountyStatus.expired);
									MobHunting.getDataStoreManager().updateBounty(bounty);
									delete(bounty);
								}
								sort = true;
							}
						}
						if (sort)
							sort();
						Messages.debug("%s bounties for %s was loaded.", n, offlinePlayer.getName());
						if (n > 0) {
							Messages.playerActionBarMessage((Player) offlinePlayer,
									Messages.getString("mobhunting.bounty.youarewanted"));
							Messages.broadcast(Messages.getString("mobhunting.bounty.playeriswanted", "playername",
									offlinePlayer.getName()), (Player) offlinePlayer);
						}
					}

					@Override
					public void onError(Throwable error) {
						if (error instanceof UserNotFoundException)
							if (offlinePlayer.isOnline()) {
								Player p = (Player) offlinePlayer;
								p.sendMessage(Messages.getString("mobhunting.bounty.user-not-found"));
							} else {
								error.printStackTrace();
								if (offlinePlayer.isOnline()) {
									Player p = (Player) offlinePlayer;
									p.sendMessage(Messages.getString("mobhunting.bounty.load-fail"));
								}
							}
					}

				});
	}

	/**
	 * put/add a bounty on the set of Bounties.
	 * 
	 * @param offlinePlayer
	 * @param bounty
	 */
	public void save(Bounty bounty) {
		//Messages.debug("Save bounty=%s", bounty.toString());
		if (hasOpenBounty(bounty.getWorldGroup(), bounty.getWantedPlayer(), bounty.getBountyOwner())) {
			getOpenBounty(bounty.getWorldGroup(), bounty.getWantedPlayer(), bounty.getBountyOwner()).setPrize(
					getOpenBounty(bounty.getWorldGroup(), bounty.getWantedPlayer(), bounty.getBountyOwner()).getPrize()
							+ bounty.getPrize());
			getOpenBounty(bounty.getWorldGroup(), bounty.getWantedPlayer(), bounty.getBountyOwner())
					.setMessage(bounty.getMessage());
			MobHunting.getDataStoreManager().updateBounty(
					getOpenBounty(bounty.getWorldGroup(), bounty.getWantedPlayer(), bounty.getBountyOwner()));
		} else {
			mOpenBounties.add(bounty);
			MobHunting.getDataStoreManager().updateBounty(bounty);
		}
	}

	public void cancel(Bounty bounty) {
		getOpenBounty(bounty.getWorldGroup(), bounty.getWantedPlayer(), bounty.getBountyOwner())
				.setStatus(BountyStatus.canceled);
		MobHunting.getDataStoreManager()
				.updateBounty(getBounty(bounty.getWorldGroup(), bounty.getWantedPlayer(), bounty.getBountyOwner()));

		Iterator<Bounty> it = mOpenBounties.iterator();
		while (it.hasNext()) {
			Bounty b = (Bounty) it.next();
			if (b.equals(bounty))
				it.remove();
		}
	}

	public void delete(Bounty bounty) {
		getOpenBounty(bounty.getWorldGroup(), bounty.getWantedPlayer(), bounty.getBountyOwner())
				.setStatus(BountyStatus.deleted);
		MobHunting.getDataStoreManager()
				.updateBounty(getOpenBounty(bounty.getWorldGroup(), bounty.getWantedPlayer(), bounty.getBountyOwner()));

		Iterator<Bounty> it = mOpenBounties.iterator();
		while (it.hasNext()) {
			Bounty b = (Bounty) it.next();
			if (b.equals(bounty))
				it.remove();
		}
	}

	// *************************************************************************************
	// BOUNTY GUI
	// *************************************************************************************

	// private static Inventory inventory;
	private static HashMap<CommandSender, Inventory> inventoryMap = new HashMap<CommandSender, Inventory>();

	public static void showOpenBounties(CommandSender sender, String worldGroupName, OfflinePlayer wantedPlayer,
			boolean useGui) {
		if (sender instanceof Player) {
			// Player player = (Player) sender;

			if (hasOpenBounties(worldGroupName, wantedPlayer)) {
				Set<Bounty> bountiesOnWantedPlayer = MobHunting.getBountyManager().getOpenBounties(worldGroupName,
						wantedPlayer);
				if (useGui) {
					final Inventory inventory = Bukkit.createInventory((InventoryHolder) sender, 54,
							ChatColor.BLUE + "" + ChatColor.BOLD + "Wanted:" + wantedPlayer.getName());
					int n = 0;
					for (Bounty bounty : bountiesOnWantedPlayer) {
						if (bounty.isOpen()) {
							if (bounty.getBountyOwner() != null)
								AchievementManager.addInventoryDetails(
										CustomItems.getPlayerHead(wantedPlayer.getName(), bounty.getPrize()), inventory,
										n, ChatColor.GREEN + wantedPlayer.getName(),
										new String[] { ChatColor.WHITE + "", Messages.getString(
												"mobhunting.commands.bounty.bounties", "bountyowner",
												bounty.getBountyOwner().getName(), "prize",
												MobHunting.getRewardManager().format(bounty.getPrize()), "wantedplayer",
												bounty.getWantedPlayer().getName(), "daysleft",
												(bounty.getEndDate() - System.currentTimeMillis()) / (86400000L)) });
							else
								AchievementManager.addInventoryDetails(
										CustomItems.getPlayerHead(wantedPlayer.getName(), bounty.getPrize()), inventory,
										n, ChatColor.GREEN + wantedPlayer.getName(),
										new String[] { ChatColor.WHITE + "", Messages.getString(
												"mobhunting.commands.bounty.bounties", "bountyowner", "Random Bounty",
												"prize", MobHunting.getRewardManager().format(bounty.getPrize()),
												"wantedplayer", bounty.getWantedPlayer().getName(), "daysleft",
												(bounty.getEndDate() - System.currentTimeMillis()) / (86400000L)) });
							if (n < 53)
								n++;
						}
					}
					if (sender instanceof Player) {
						inventoryMap.put(sender, inventory);
						((Player) sender).openInventory(inventoryMap.get(sender));
					} else
						Bukkit.getConsoleSender()
								.sendMessage(ChatColor.RED + "This command can not used in the console");

				} else {
					sender.sendMessage(Messages.getString("mobhunting.commands.bounty.bounties-header"));
					sender.sendMessage("-----------------------------------");
					for (Bounty bounty : bountiesOnWantedPlayer) {
						if (bounty.isOpen())
							if (bounty.getBountyOwner() != null)
								sender.sendMessage(Messages.getString("mobhunting.commands.bounty.bounties",
										"bountyowner", bounty.getBountyOwner().getName(), "prize",
										MobHunting.getRewardManager().format(bounty.getPrize()), "wantedplayer",
										bounty.getWantedPlayer().getName(), "daysleft",
										(bounty.getEndDate() - System.currentTimeMillis()) / (86400000L)));
							else
								sender.sendMessage(Messages.getString("mobhunting.commands.bounty.bounties",
										"bountyowner", "Random Bounty", "prize",
										MobHunting.getRewardManager().format(bounty.getPrize()), "wantedplayer",
										bounty.getWantedPlayer().getName(), "daysleft",
										(bounty.getEndDate() - System.currentTimeMillis()) / (86400000L)));
					}
				}
			} else {
				sender.sendMessage(Messages.getString("mobhunting.commands.bounty.no-bounties-player", "wantedplayer",
						wantedPlayer.getName()));
			}
		} else {
			sender.sendMessage("[MobHunting] You cant use this command in the console");
		}
	}

	public static void showMostWanted(CommandSender sender, String worldGroupName, boolean useGui) {
		if (sender instanceof Player) {
			if (!mOpenBounties.isEmpty()) {
				if (useGui) {
					Inventory inventory = Bukkit.createInventory((InventoryHolder) sender, 54,
							ChatColor.BLUE + "" + ChatColor.BOLD + "MostWanted:");
					int n = 0;
					for (Bounty bounty : mOpenBounties) {
						if (bounty.getBountyOwner() != null)
							AchievementManager.addInventoryDetails(
									CustomItems.getPlayerHead(bounty.getWantedPlayer().getName(), bounty.getPrize()),
									inventory, n, ChatColor.GREEN + bounty.getWantedPlayer().getName(),
									new String[] { ChatColor.WHITE + "", Messages.getString(
											"mobhunting.commands.bounty.bounties", "bountyowner",
											bounty.getBountyOwner().getName(), "prize",
											MobHunting.getRewardManager().format(bounty.getPrize()), "wantedplayer",
											bounty.getWantedPlayer().getName(), "daysleft",
											(bounty.getEndDate() - System.currentTimeMillis()) / (86400000L)) });
						else
							AchievementManager.addInventoryDetails(
									CustomItems.getPlayerHead(bounty.getWantedPlayer().getName(), bounty.getPrize()),
									inventory, n, ChatColor.GREEN + bounty.getWantedPlayer().getName(),
									new String[] { ChatColor.WHITE + "", Messages.getString(
											"mobhunting.commands.bounty.bounties", "bountyowner", "Random Bounty",
											"prize", MobHunting.getRewardManager().format(bounty.getPrize()),
											"wantedplayer", bounty.getWantedPlayer().getName(), "daysleft",
											(bounty.getEndDate() - System.currentTimeMillis()) / (86400000L)) });
						if (n < 53)
							n++;
					}
					if (sender instanceof Player) {
						inventoryMap.put(sender, inventory);
						((Player) sender).openInventory(inventoryMap.get(sender));
					} else
						Bukkit.getConsoleSender()
								.sendMessage(ChatColor.RED + "This command can not used in the console");
				} else {
					sender.sendMessage(Messages.getString("mobhunting.commands.bounty.bounties-header"));
					sender.sendMessage("-----------------------------------");
					for (Bounty bounty : mOpenBounties) {
						if (bounty.getBountyOwner() != null)
							sender.sendMessage(Messages.getString("mobhunting.commands.bounty.bounties", "bountyowner",
									bounty.getBountyOwner().getName(), "prize",
									MobHunting.getRewardManager().format(bounty.getPrize()), "wantedplayer",
									bounty.getWantedPlayer().getName(), "daysleft",
									(bounty.getEndDate() - System.currentTimeMillis()) / (86400000L)));
						else
							sender.sendMessage(Messages.getString("mobhunting.commands.bounty.bounties", "bountyowner",
									"Random Bounty", "prize", MobHunting.getRewardManager().format(bounty.getPrize()),
									"wantedplayer", bounty.getWantedPlayer().getName(), "daysleft",
									(bounty.getEndDate() - System.currentTimeMillis()) / (86400000L)));
					}
				}
			} else {
				sender.sendMessage(Messages.getString("mobhunting.commands.bounty.no-bounties"));
			}
		} else {
			sender.sendMessage("[MobHunting] You cant use this command in the console");
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		final Inventory inv = event.getInventory();
		final Player player = (Player) event.getWhoClicked();
		if (ChatColor.stripColor(inv.getName()).startsWith("MostWanted:")
				|| ChatColor.stripColor(inv.getName()).startsWith("Wanted:")) {
			event.setCancelled(true);
			Bukkit.getScheduler().runTask(instance, new Runnable() {
				public void run() {
					player.closeInventory();
					inventoryMap.remove(player);
				}
			});
		}
	}

	// ***********************************************************
	// RANDOM BOUNTY
	// ***********************************************************

	public void createRandomBounty() {
		boolean createBounty = MobHunting.getMobHuntingManager().mRand
				.nextDouble() <= MobHunting.getConfigManager().chanceToCreateBounty;
		if (createBounty) {
			int noOfPlayers = MobHunting.getMobHuntingManager().getOnlinePlayersAmount();
			Player randomPlayer = null;
			if (MobHunting.getConfigManager().minimumNumberOfOnlinePlayers <= noOfPlayers) {

				int random = MobHunting.getMobHuntingManager().mRand.nextInt(noOfPlayers);
				int n = 0;
				for (Player player : MobHunting.getMobHuntingManager().getOnlinePlayers()) {
					if (n == random) {
						randomPlayer = player;
						break;
					} else
						n++;
				}
				if (randomPlayer != null) {
					String worldGroup = MobHunting.getWorldGroupManager().getCurrentWorldGroup(randomPlayer);
					Bounty randomBounty = new Bounty(worldGroup, randomPlayer, Misc.round(
							MobHunting.getConfigManager().getRandomPrice(MobHunting.getConfigManager().randomBounty)),
							"Random Bounty");
					save(randomBounty);
					for (Player player : MobHunting.getMobHuntingManager().getOnlinePlayers()) {
						if (player.getName().equals(randomPlayer.getName()))
							Messages.playerActionBarMessage(player,
									Messages.getString("mobhunting.bounty.randombounty.self", "prize",
											MobHunting.getRewardManager().format(randomBounty.getPrize())));
						else
							Messages.playerActionBarMessage(player,
									Messages.getString("mobhunting.bounty.randombounty", "prize",
											MobHunting.getRewardManager().format(randomBounty.getPrize()), "playername",
											randomPlayer.getName()));
					}
				}
			}
		}
	}

}