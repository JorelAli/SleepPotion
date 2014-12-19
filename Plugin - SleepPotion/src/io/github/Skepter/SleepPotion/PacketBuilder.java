package io.github.Skepter.SleepPotion;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/** A simple way to create custom packets - modified from AllAssets
 * so it only works for going to bed/waking up packets */
public class PacketBuilder {

	public enum PacketType {
		PLAY_OUT_BED, PLAY_OUT_ANIMATION;
	}

	private ReflectionUtils utils;
	private Object packet;
	private PacketBuilder builder;

	public PacketBuilder(final Player player, PacketType type) {
		try {
			this.utils = new ReflectionUtils(player);
			this.packet = null;
			this.builder = this;

			switch (type) {
			case PLAY_OUT_ANIMATION:
				packet = utils.emptyPacketPlayOutAnimation;
				break;
			case PLAY_OUT_BED:
				packet = utils.emptyPacketPlayOutBed;
				break;
			}
			packet = packet.getClass().getConstructor().newInstance();
		} catch (Exception e) {
		}
	}

	public PacketBuilder set(String name, Object data) {
		try {
			utils.setPrivateField(packet, name, data);
		} catch (Exception e) {
		}
		return builder;
	}

	public PacketBuilder setInt(String name, int data) {
		try {
			utils.setPrivateField(packet, name, Integer.valueOf(data));
		} catch (Exception e) {
		}
		return builder;
	}

	public PacketBuilder setLocation(String name1, String name2, String name3, Location data) {
		setInt(name1, (int) data.getX());
		setInt(name2, (int) data.getY());
		setInt(name3, (int) data.getZ());
		return builder;
	}

	public void send() {
		try {
			utils.sendOutgoingPacket(packet);
		} catch (Exception e) {
		}
	}
}
