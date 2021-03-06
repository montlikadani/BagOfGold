package one.lindegaard.BagOfGold.bank;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.BagOfGold.Reward;
import one.lindegaard.BagOfGold.util.Misc;

public class BankSign implements Listener {

	private BagOfGold plugin;

	public BankSign(BagOfGold plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	// ****************************************************************************'
	// Events
	// ****************************************************************************'
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.isCancelled())
			return;

		if (Misc.isMC19OrNewer() && (event.getHand() == null || event.getHand().equals(EquipmentSlot.OFF_HAND)))
			return;

		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock != null && isBankSign(clickedBlock) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player player = event.getPlayer();
			if (player.hasPermission("bagofgold.banksign.use")) {
				if (player.getGameMode() == GameMode.SURVIVAL) {
					Sign sign = ((Sign) clickedBlock.getState());
					String signType = sign.getLine(1);
					double money = 0;
					double moneyInHand = 0;
					double moneyOnSign = 0;
					// deposit BankSign
					// -----------------------------------------------------------------------
					if (signType.equalsIgnoreCase(plugin.getMessages().getString("bagofgold.banksign.line2.deposit"))) {
						if (player.getItemInHand().getType().equals(Material.SKULL_ITEM)
								&& Reward.isReward(player.getItemInHand())) {
							Reward reward = Reward.getReward(player.getItemInHand());

							moneyInHand = reward.getMoney();
							money = moneyInHand;
							if (moneyInHand == 0) {
								plugin.getMessages().playerSendMessage(player, plugin.getMessages().getString(
										"bagofgold.banksign.item_has_no_value", "itemname", reward.getDisplayname()));
								return;
							}

							if (sign.getLine(2).isEmpty() || sign.getLine(2).equalsIgnoreCase(
									plugin.getMessages().getString("bagofgold.banksign.line3.everything"))) {
								money = moneyInHand;
								moneyOnSign = moneyInHand;
							} else {
								try {
									moneyOnSign = Double.valueOf(sign.getLine(2));
									money = moneyInHand <= moneyOnSign ? moneyInHand : moneyOnSign;
								} catch (NumberFormatException e) {
									plugin.getMessages().playerSendMessage(player, plugin.getMessages().getString(
											"bagofgold.banksign.line3.not_a_number", "number", sign.getLine(2),
											"everything",
											plugin.getMessages().getString("bagofgold.banksign.line3.everything")));
									return;
								}
							}

							plugin.getEconomyManager().bankDeposit(player.getUniqueId().toString(), money);
							plugin.getEconomyManager().withdrawPlayer(player, money);

							plugin.getMessages().debug("%s deposit %s %s into Bank", player.getName(),
									Misc.format(money), reward.getDisplayname());
							plugin.getMessages().playerSendMessage(player,
									plugin.getMessages().getString("bagofgold.banksign.deposit", "money",
											Misc.format(money), "rewardname",
											ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
													+ reward.getDisplayname().trim()));
						} else {
							plugin.getMessages().playerSendMessage(player, plugin.getMessages().getString(
									"bagofgold.banksign.hold_bag_in_hand", "rewardname",
									ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
											+ plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim()));
						}

						// Withdraw BankSign
						// -----------------------------------------------------------------------
					} else if (signType
							.equalsIgnoreCase(plugin.getMessages().getString("bagofgold.banksign.line2.withdraw"))) {
						if (sign.getLine(2).isEmpty() || sign.getLine(2).equalsIgnoreCase(
								plugin.getMessages().getString("bagofgold.banksign.line3.everything"))) {
							moneyOnSign = plugin.getEconomyManager()
									.bankBalance(player.getUniqueId().toString()).balance;
						} else {
							try {
								moneyOnSign = Double.valueOf(sign.getLine(2));
							} catch (NumberFormatException e) {
								plugin.getMessages().playerSendMessage(player,
										plugin.getMessages().getString("bagofgold.banksign.line3.not_a_number",
												"number", sign.getLine(2), "everything", plugin.getMessages()
														.getString("bagofgold.banksign.line3.everything")));
								return;
							}
						}
						if (plugin.getEconomyManager()
								.bankBalance(player.getUniqueId().toString()).balance >= moneyOnSign) {

							plugin.getEconomyManager().bankWithdraw(player.getUniqueId().toString(), moneyOnSign);
							plugin.getEconomyManager().depositPlayer(player, moneyOnSign);

							plugin.getMessages().playerSendMessage(player, plugin.getMessages().getString(
									"bagofgold.banksign.withdraw", "money", Misc.format(moneyOnSign), "rewardname",
									ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
											+ plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim()));
						} else {
							plugin.getMessages().playerSendMessage(player,
									plugin.getMessages().getString("bagofgold.banksign.not_enough_money"));
						}

						// Balance BankSign
						// -----------------------------------------------------------------------
					} else if (signType
							.equalsIgnoreCase(plugin.getMessages().getString("bagofgold.banksign.line2.balance"))) {
						plugin.getMessages().playerSendMessage(player,
								plugin.getMessages().getString("bagofgold.banksign.balance", "money",
										Misc.format(plugin.getEconomyManager()
												.bankBalance(player.getUniqueId().toString()).balance),
										"rewardname",
										ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
												+ plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim()));
					}
				} else {
					plugin.getMessages().playerSendMessage(player, plugin.getMessages()
							.getString("bagofgold.banksign.only_survival"));
				}
			} else {
				plugin.getMessages().playerSendMessage(player, plugin.getMessages()
						.getString("bagofgold.banksign.no_permission_to_use", "perm", "bagofgold.banksign.use"));
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onSignChangeEvent(SignChangeEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();
		if (isBagOfGoldSign(event.getLine(0))) {
			if (event.getPlayer().hasPermission("bagofgold.banksign.create")) {

				// Check line 2
				if (ChatColor.stripColor(event.getLine(1)).equalsIgnoreCase(
						ChatColor.stripColor(plugin.getMessages().getString("bagofgold.banksign.line2.deposit")))) {
					event.setLine(1, plugin.getMessages().getString("bagofgold.banksign.line2.deposit"));

				} else if (ChatColor.stripColor(event.getLine(1)).equalsIgnoreCase(
						ChatColor.stripColor(plugin.getMessages().getString("bagofgold.banksign.line2.withdraw")))) {
					event.setLine(1, plugin.getMessages().getString("bagofgold.banksign.line2.withdraw"));
				} else if (ChatColor.stripColor(event.getLine(1)).equalsIgnoreCase(
						ChatColor.stripColor(plugin.getMessages().getString("bagofgold.banksign.line2.balance")))) {
					event.setLine(1, plugin.getMessages().getString("bagofgold.banksign.line2.balance"));
				} else {
					plugin.getMessages().playerSendMessage(player, plugin.getMessages()
							.getString("bagofgold.banksign.line2.mustbe_deposit_or_withdraw_or_balance"));
					event.setLine(3,
							plugin.getMessages().getString("bagofgold.banksign.line4.error_on_sign", "line", "2"));
					return;
				}

				// Check line 3
				if (ChatColor.stripColor(event.getLine(1)).equalsIgnoreCase(
						ChatColor.stripColor(plugin.getMessages().getString("bagofgold.banksign.line2.balance")))) {
					event.setLine(1, plugin.getMessages().getString("bagofgold.banksign.line2.balance"));
					event.setLine(2, "");
				} else if (event.getLine(2).isEmpty() || ChatColor.stripColor(event.getLine(2)).equalsIgnoreCase(
						ChatColor.stripColor(plugin.getMessages().getString("bagofgold.banksign.line3.everything")))) {
					event.setLine(2, plugin.getMessages().getString("bagofgold.banksign.line3.everything"));
				} else {
				
					try {
						if (Double.valueOf(event.getLine(2)) > 0) {
							plugin.getMessages().debug("%s created a Bag of gold Sign", event.getPlayer().getName());
						}
					} catch (NumberFormatException e) {
						plugin.getMessages().playerSendMessage(player,
								plugin.getMessages().getString("bagofgold.banksign.line3.not_a_number", "number",
										event.getLine(2), "everything",
										plugin.getMessages().getString("bagofgold.banksign.line3.everything")));
						event.setLine(3,
								plugin.getMessages().getString("bagofgold.banksign.line4.error_on_sign", "line", "3"));
						return;
					}
				}

				event.setLine(0, plugin.getMessages().getString("bagofgold.banksign.line1", "bankname",
						plugin.getConfigManager().bankname.trim()));
				event.setLine(3, plugin.getMessages().getString("bagofgold.banksign.line4.ok"));

			} else {
				plugin.getMessages().playerSendMessage(player, plugin.getMessages()
						.getString("bagofgold.banksign.no_permission", "perm", "bagofgold.banksign.create"));
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onBlockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled())
			return;

		Block b = event.getBlock();
		if (isBankSign(b)) {
			if (event.getPlayer().hasPermission("bagofgold.banksign.destroy")) {
				plugin.getMessages().debug("%s destroyed a BagOfGold sign", event.getPlayer().getName());
			} else {
				plugin.getMessages().debug("%s tried to destroy a BagOfGold sign without permission",
						event.getPlayer().getName());
				event.getPlayer().sendMessage(plugin.getMessages().getString("bagofgold.banksign.no_permission", "perm",
						"bagofgold.banksign.destroy"));
				event.setCancelled(true);
			}
		}
	}

	// ************************************************************************************
	// TESTS
	// ************************************************************************************

	private boolean isBankSign(Block block) {
		if (Misc.isSign(block)) {
			return ChatColor.stripColor(((Sign) block.getState()).getLine(0)).equalsIgnoreCase(
					ChatColor.stripColor(BagOfGold.getInstance().getMessages().getString("bagofgold.banksign.line1",
							"bankname", BagOfGold.getInstance().getConfigManager().bankname.trim())))
					|| ChatColor.stripColor(((Sign) block.getState()).getLine(0)).equalsIgnoreCase("[bank]");
		}
		return false;
	}

	private boolean isBagOfGoldSign(String line) {
		return ChatColor.stripColor(line).equalsIgnoreCase(
				ChatColor.stripColor(BagOfGold.getInstance().getMessages().getString("bagofgold.banksign.line1",
						"bankname", BagOfGold.getInstance().getConfigManager().bankname.trim())))
				|| ChatColor.stripColor(line).equalsIgnoreCase("[bank]");
	}

}
