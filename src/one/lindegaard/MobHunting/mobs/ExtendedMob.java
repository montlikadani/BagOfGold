package one.lindegaard.MobHunting.mobs;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import net.citizensnpcs.api.npc.NPC;
import one.lindegaard.MobHunting.Messages;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.CitizensCompat;
import one.lindegaard.MobHunting.compatibility.CustomMobsCompat;
import one.lindegaard.MobHunting.compatibility.MysteriousHalloweenCompat;
import one.lindegaard.MobHunting.compatibility.MythicMobsCompat;
import one.lindegaard.MobHunting.compatibility.SmartGiantsCompat;
import one.lindegaard.MobHunting.compatibility.TARDISWeepingAngelsCompat;

public class ExtendedMob {

	private Integer mob_id; // The unique mob_id from mh_Mobs
	private MobPlugin mobPlugin; // Plugin_id from mh_Plugins
	private String mobtype; // mobtype NOT unique

	public ExtendedMob(Integer mob_id, MobPlugin mobPlugin, String mobtype) {
		this.mob_id = mob_id;
		this.mobPlugin = mobPlugin;
		this.mobtype = mobtype;
	}

	public ExtendedMob(MobPlugin mobPlugin, String mobtype) {
		this.mobPlugin = mobPlugin;
		this.mobtype = mobtype;
	}

	/**
	 * @return the mob_id
	 */
	public Integer getMob_id() {
		return mob_id;
	}

	/**
	 * @return the plugin_id
	 */
	public MobPlugin getMobPlugin() {
		return mobPlugin;
	}

	/**
	 * @return the mobtype
	 */
	public String getMobtype() {
		return mobtype;
	}

	/**
	 * @param mobtype
	 *            the mobtype to set
	 */
	public void setMobtype(String mobtype) {
		this.mobtype = mobtype;
	}

	@Override
	public int hashCode() {
		return mob_id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ExtendedMob))
			return false;

		ExtendedMob other = (ExtendedMob) obj;

		return mob_id.equals(other.mob_id);
	}

	@Override
	public String toString() {
		return String.format("MobStore: {mob_id: %s, plugin_id: %s, mobtype: %s}", this.mob_id, mobPlugin.name(),
				mobtype);
	}

	public String getName() {
		switch (mobPlugin) {
		case MythicMobs:
			String name = MythicMobsCompat.getMobRewardData().get(mobtype).getMobName();
			if (name == null || name.equals(""))
				name = MythicMobsCompat.getMobRewardData().get(mobtype).getMobType();
			return name;
		case Citizens:
			NPC npc = CitizensCompat.getCitizensPlugin().getNPCRegistry().getById(Integer.valueOf(mobtype));
			if (npc != null)
				return npc.getName();
			else
				return "";
		case TARDISWeepingAngels:
			return TARDISWeepingAngelsCompat.getMobRewardData().get(mobtype).getMobName();
		case CustomMobs:
			return CustomMobsCompat.getMobRewardData().get(mobtype).getMobName();
		case MysteriousHalloween:
			return MysteriousHalloweenCompat.getMobRewardData().get(mobtype).getMobName();
		case Minecraft:
			return mobtype;
		case SmartGiants:
			return "SmartGiant";
		}
		return null;
	}

	public String getFriendlyName() {
		if (mobPlugin == MobPlugin.Minecraft)
			return Messages.getString("mobs." + getName() + ".name");
		else
			return Messages.getString("mobs." + mobPlugin.name() + "_" + getMobtype() + ".name");
	}

	public int getProgressAchievementLevel1() {
		switch (mobPlugin) {
		case Minecraft:
			return MinecraftMob.getMinecraftMobType(mobtype).getProgressAchievementLevel1();
		case MythicMobs:
			return MythicMobsCompat.getProgressAchievementLevel1(mobtype);
		case Citizens:
			return CitizensCompat.getProgressAchievementLevel1(mobtype);
		case MysteriousHalloween:
			return MysteriousHalloweenCompat.getProgressAchievementLevel1(mobtype);
		case TARDISWeepingAngels:
			return TARDISWeepingAngelsCompat.getProgressAchievementLevel1(mobtype);
		case CustomMobs:
			return CustomMobsCompat.getProgressAchievementLevel1(mobtype);
		case SmartGiants:
			return SmartGiantsCompat.getProgressAchievementLevel1(mobtype);
		}
		return 0;
	}

	public ItemStack getCustomHead(String name, int amount, int money) {
		switch (mobPlugin) {
		case Minecraft:
			return MinecraftMob.getMinecraftMobType(name).getCustomHead(name, amount, money);
		default:
			return new ItemStack(Material.IRON_INGOT, amount);
		}
	}

	public boolean matches(Entity entity) {
		ExtendedMob mob = MobHunting.getExtendedMobManager().getExtendedMobFromEntity(entity);
		return mobtype.equalsIgnoreCase(mob.mobtype);
	}
}
