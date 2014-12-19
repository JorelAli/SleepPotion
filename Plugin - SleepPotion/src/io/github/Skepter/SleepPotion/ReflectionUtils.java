package io.github.Skepter.SleepPotion;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** ReflectionUtils used to get the packets and send them */
public class ReflectionUtils {

	final private Object getConnection;
	final private String packageName;
	final private Class<?> packetClass;
	final public Object emptyPacketPlayOutBed;
	final public Object emptyPacketPlayOutAnimation;
	
	public ReflectionUtils(final Player player) throws Exception {
		Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
		getConnection = getField(nmsPlayer, "playerConnection");

		packageName = getPrivateField(Bukkit.getServer(), "console").getClass().getPackage().getName();
		packetClass = getNMSClass("Packet");

		emptyPacketPlayOutBed = getNMSClass("PacketPlayOutBed").newInstance();
		emptyPacketPlayOutAnimation = getNMSClass("PacketPlayOutAnimation").newInstance();
	}

	public Object getPrivateField(final Object object, final String fieldName) throws Exception {
		final Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(object);
	}

	public Object getField(final Object object, final String fieldName) throws Exception {
		return object.getClass().getDeclaredField(fieldName).get(object);
	}

	public void setPrivateField(final Object object, final String fieldName, final Object data) throws Exception {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(object, data);
	}
	
	public Class<?> getNMSClass(String className) throws ClassNotFoundException {
		return (Class.forName(packageName + "." + className));
	}

	public void sendOutgoingPacket(Object packet) throws Exception {
		getConnection.getClass().getMethod("sendPacket", packetClass).invoke(getConnection, packet);
	}
}
