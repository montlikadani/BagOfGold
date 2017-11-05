package one.lindegaard.BagOfGold.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.BagOfGold.Messages;
import one.lindegaard.BagOfGold.update.UpdateStatus;
import one.lindegaard.BagOfGold.update.Updater;
import one.lindegaard.MobHunting.MobHunting;

public class UpdateCommand implements ICommand {

	private BagOfGold plugin;

	public UpdateCommand(BagOfGold plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "update";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "bagofgold.update";
	}

	@Override
	public String[] getUsageString(String label, CommandSender sender) {
		return new String[] { ChatColor.GOLD + label + ChatColor.WHITE + " - to download and update the plugin." };
	}

	@Override
	public String getDescription() {
		return Messages.getString("bagofgold.commands.update.description");
	}

	@Override
	public boolean canBeConsole() {
		return true;
	}

	@Override
	public boolean canBeCommandBlock() {
		return false;
	}

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) {
		if (Updater.getUpdateAvailable() == UpdateStatus.AVAILABLE) {
			if (Updater.downloadAndUpdateJar()) {
				MobHunting.getInstance().getMessages().senderSendMessage(sender,
						ChatColor.GREEN + Messages.getString("bagofgold.commands.update.complete"));
			} else {
				MobHunting.getInstance().getMessages().senderSendMessage(sender,
						ChatColor.GREEN + Messages.getString("bagofgold.commands.update.could-not-update"));
			}
		} else if (Updater.getUpdateAvailable() == UpdateStatus.RESTART_NEEDED) {
			MobHunting.getInstance().getMessages().senderSendMessage(sender,
					ChatColor.GREEN + Messages.getString("bagofgold.commands.update.complete"));
		} else {
			Updater.pluginUpdateCheck(sender, true, false);
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
		return null;
	}

}