/**
 *
 * This software is part of the MobileTools
 *
 * MobileTools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or 
 * any later version.
 *
 * MobileTools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MobileTools. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package me.cybermaxke.mobiletools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.bukkit.Bukkit;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftBrewingStand;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftFurnace;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;

import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.ChatMessage;
import net.minecraft.server.v1_8_R3.ContainerAnvil;
import net.minecraft.server.v1_8_R3.ContainerEnchantTable;
import net.minecraft.server.v1_8_R3.ContainerWorkbench;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IInventory;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.PacketPlayOutOpenWindow;
import net.minecraft.server.v1_8_R3.TileEntityBrewingStand;
import net.minecraft.server.v1_8_R3.TileEntityFurnace;

public class MobilePlayer {
	private final MobilePlayerData data;
	private final MobileConfiguration config;

	private final Player player;
	private final EntityPlayer handle;

	private Inventory chest;
	private EntityFurnace furnace;
	private EntityBrewingStand brewingStand;

	public MobilePlayer(MobileTools plugin, Player player) {
		this.player = player;
		this.handle = ((CraftPlayer) player).getHandle();
		this.chest = Bukkit.createInventory(player, this.getChestSize());
		this.furnace = new EntityFurnace(this.handle);
		this.brewingStand = new EntityBrewingStand(this.handle);

		this.config = plugin.getConfiguration();
		this.data = plugin.getPlayerData(player);

		this.load();
	}

	public Inventory getChest() {
		return this.chest;
	}

	public EntityFurnace getFurnace() {
		return this.furnace;
	}

	public EntityBrewingStand getBrewingStand() {
		return this.brewingStand;
	}

	public void clearFurnace() {
		for (int slot = 0; slot < this.furnace.getSize(); slot++) {
			this.furnace.setItem(slot, null);
		}
	}

	public void clearBrewingStand() {
		for (int slot = 0; slot < this.brewingStand.getSize(); slot++) {
			this.brewingStand.setItem(slot, null);
		}
	}

	public void openEnderChest() {
		this.player.openInventory(this.player.getEnderChest());
	}

	public void openWorkbench() {
		WorkbenchContainer container = new WorkbenchContainer(this.handle);

		int c = this.handle.nextContainerCounter();
		this.handle.playerConnection.sendPacket(new PacketPlayOutOpenWindow(c, "minecraft:crafting_table ", new ChatMessage("Crafting"), 0));
		this.handle.activeContainer = container;
		this.handle.activeContainer.windowId = c;
		this.handle.activeContainer.addSlotListener(this.handle);
	}

	public void openEnchantingTable() {
		EnchantTableContainer container = new EnchantTableContainer(this.config, this.handle);

		int c = this.handle.nextContainerCounter();
		this.handle.playerConnection.sendPacket(new PacketPlayOutOpenWindow(c, "minecraft:enchanting_table", new ChatMessage("Enchanting"), 0));
		this.handle.activeContainer = container;
		this.handle.activeContainer.windowId = c;
		this.handle.activeContainer.addSlotListener(this.handle);
	}

	public void openAnvil() {
		AnvilContainer container = new AnvilContainer(this.handle);

		int c = this.handle.nextContainerCounter();
		this.handle.playerConnection.sendPacket(new PacketPlayOutOpenWindow(c, "minecraft:anvil", new ChatMessage("Repairing"), 0));
		this.handle.activeContainer = container;
		this.handle.activeContainer.windowId = c;
		this.handle.activeContainer.addSlotListener(this.handle);
	}

	public void updateChestSize() {
		int newSize = this.getChestSize();
		if (this.chest.getSize() == newSize) {
			return;
		}

		org.bukkit.inventory.ItemStack[] items = this.chest.getContents();
		this.chest = Bukkit.createInventory(this.player, newSize);

		for (int i = 0; i < (items.length > this.chest.getSize() ? this.chest.getSize() : items.length); i++) {
			this.chest.setItem(i, items[i]);
		}
	}

	public int getChestSize() {
		int maxSize = 54;
		int size = 9;
		for (int i = 1; i <= (maxSize / 9); i++) {
			if (this.player.hasPermission(new Permission("mobiletools.chestsize." + (i * 9), PermissionDefault.OP))) {
				size = i * 9;
			}
		}
		return size;
	}

	public void openChest() {
		this.player.openInventory(this.chest);
	}

	public void openFurnace() {
		this.handle.openTileEntity(this.furnace);
	}

	public void openBrewingStand() {
		this.handle.openTileEntity(this.brewingStand);
	}

	public void save() {
		this.data.saveInventory("Chest", this.chest);
		this.data.saveInventory("Furnace", this.furnace);
		this.data.saveInventory("BrewingStand", this.brewingStand);

		this.data.save();
	}

	public void load() {
		this.data.load();

		this.data.loadInventory("Chest", this.chest);
		this.data.loadInventory("Furnace", this.furnace);
		this.data.loadInventory("BrewingStand", this.brewingStand);
	}

	public void remove() {
		this.save();
	}

	public class EnchantTableContainer extends ContainerEnchantTable {
		private final MobileConfiguration config;
		private final Player player;

		public EnchantTableContainer(MobileConfiguration config, EntityPlayer entity) {
			super(entity.inventory, entity.world, new BlockPosition(0, 0, 0));
			this.config = config;
			this.player = entity.getBukkitEntity();
		}

		@Override
		public void a(IInventory iinventory) {
			if (iinventory == this.enchantSlots) {
				ItemStack itemstack = iinventory.getItem(0);

				if (itemstack != null) {
					this.costs[0] = this.config.getRandom("enchant.levels.line1").getRandom();
					this.costs[1] = this.config.getRandom("enchant.levels.line2").getRandom();
					this.costs[2] = this.config.getRandom("enchant.levels.line3").getRandom();

					CraftItemStack item = CraftItemStack.asCraftMirror(itemstack);

					PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(this.player, this.getBukkitView(), null, item, this.costs, 0);
					event.setCancelled(!itemstack.x());

					this.player.getServer().getPluginManager().callEvent(event);

					if (!event.isCancelled()) {
						return;
					}
				}

				this.costs[0] = 0;
				this.costs[1] = 0;
				this.costs[2] = 0;
			}
		}

		@Override
		public boolean a(EntityHuman entityhuman) {
			return true;
		}
	}

	public class WorkbenchContainer extends ContainerWorkbench {

		public WorkbenchContainer(EntityHuman entity) {
			super(entity.inventory, entity.world, new BlockPosition(0, 0, 0));
		}

		@Override
		public boolean a(EntityHuman entityhuman) {
			return true;
		}
	}

	public class AnvilContainer extends ContainerAnvil {

		public AnvilContainer(EntityHuman entity) {
			super(entity.inventory, entity.world, new BlockPosition(0, 0, 0), entity);
		}

		@Override
		public boolean a(EntityHuman entityhuman) {
			return true;
		}
	}

	public class EntityBrewingStand extends TileEntityBrewingStand {

		public EntityBrewingStand(EntityHuman entity) {
			this.world = entity.world;
		}

		@Override
		public boolean a(EntityHuman entityhuman) {
			return true;
		}

		@Override
		public int u() {
			return 0;
		}

		@Override
		public void update() {

		}

		@Override
		public Block w() {
			return Blocks.BREWING_STAND;
		}

		@Override
		public InventoryHolder getOwner() {
			BrewingStand brew = new CraftBrewingStand(this.world.getWorld().getBlockAt(0, 0, 0));

			/**
			 * Setting the tile we will use, this is the only good way!
			 */
			try {
				Field field = CraftBrewingStand.class.getDeclaredField("brewingStand");
				field.setAccessible(true);

				Field mfield = Field.class.getDeclaredField("modifiers");
				mfield.setAccessible(true);
				mfield.set(field, field.getModifiers() & ~Modifier.FINAL);

				field.set(brew, this);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return brew;
		}
	}

	private static Field furnaceField;

	public class EntityFurnace extends TileEntityFurnace {
		public EntityFurnace(EntityHuman entity) {
			if (furnaceField == null) {
				try {
					furnaceField = CraftFurnace.class.getDeclaredField("furnace");
					furnaceField.setAccessible(true);

					Field mfield = Field.class.getDeclaredField("modifiers");
					mfield.setAccessible(true);
					mfield.set(furnaceField, furnaceField.getModifiers() & ~Modifier.FINAL);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			this.world = entity.world;
		}

		@Override
		public boolean a(EntityHuman entityhuman) {
			return true;
		}

		@Override
		public int u() {
			return 0;
		}

		@Override
		public void update() {

		}

		@Override
		public Block w() {
			return Blocks.FURNACE;
		}

		@Override
		public InventoryHolder getOwner() {
			Furnace furnace = new CraftFurnace(this.world.getWorld().getBlockAt(0, 0, 0));
			try {
				furnaceField.set(furnace, this);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			return furnace;
		}
	}
}
