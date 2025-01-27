package net.prism.notra.shop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.prism.notra.config.NotraConfig;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShopNotifier {
    private static final List<ShopTime> SHOPS = Arrays.asList(
            new ShopTime("Buckstar", 6, 0, 11, 59),
            new ShopTime("Bakery", 12, 0, 17, 59),
            new ShopTime("Cocktail Bar", 18, 0, 19, 29),
            new ShopTime("Cheese Shop", 19, 30, 1, 59)
    );
    private static final NotraConfig CONFIG = NotraConfig.getInstance();
    private static final Set<String> lastOpenShops = new HashSet<>();

    private static final int WORLD_LOAD_TICK_THRESHOLD = 0;
    private static final int MAX_NO_CHANGE_TICKS = 200;
    private static final int TIME_JUMP_THRESHOLD = 1000;

    private static int lastNotifiedHour = -1;
    private static int lastNotifiedMinute = -1;
    private static int ticksSinceWorldLoad = 0;
    private static int noChangeCounter = 0;

    private static boolean hasJoinedWorld = false;
    private static boolean isTimeInitialized = false;
    private static boolean hasErrorToastShown = false;

    private static long lastTime = -1;
    private static World lastWorld = null;


    public static void tick(MinecraftClient client) {
        checkWorldChange(client);
        if (!isAllowedServer()) return;
        if (client.world == null || client.player == null) {
            resetState();
            return;
        }
        if (!hasJoinedWorld) {
            ticksSinceWorldLoad++;
            if (ticksSinceWorldLoad < WORLD_LOAD_TICK_THRESHOLD) return;
            hasJoinedWorld = true;
        }
        long t = getTimeOfDay(client) % 24000;
        handleTimeChange(t);
        if (!isTimeInitialized) return;
        int hour = (int) ((t + 6000) % 24000 / 1000);
        int minute = (int) ((t % 1000) * 60 / 1000);
        if (lastNotifiedHour == -1 && lastNotifiedMinute == -1) {
            lastNotifiedHour = hour;
            lastNotifiedMinute = minute;
            Set<String> nowOpen = getOpenShops(hour, minute);
            for (String shop : nowOpen) {
                showShopNotification(shop, shop + " is already open.");
            }
            lastOpenShops.clear();
            lastOpenShops.addAll(nowOpen);
            return;
        }
        if (hour != lastNotifiedHour || minute != lastNotifiedMinute) {
            lastNotifiedHour = hour;
            lastNotifiedMinute = minute;
            Set<String> nowOpen = getOpenShops(hour, minute);
            Set<String> newlyOpened = new HashSet<>(nowOpen);
            newlyOpened.removeAll(lastOpenShops);
            for (String shop : newlyOpened) {
                showShopNotification(shop, shop + " just opened.");
            }
            lastOpenShops.clear();
            lastOpenShops.addAll(nowOpen);
        }
    }

    private static void checkWorldChange(MinecraftClient client) {
        if (client.world == null) {
            resetState();
            lastWorld = null;
        } else if (client.world != lastWorld) {
            resetState();
            lastWorld = client.world;
        }
    }

    private static boolean isAllowedServer() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c == null) return false;
        ClientPlayNetworkHandler nh = c.getNetworkHandler();
        if (nh == null || nh.getConnection() == null) return false;
        SocketAddress addr = nh.getConnection().getAddress();
        if (addr == null) return false;
        String s = addr.toString().toLowerCase();
        return s.contains("play.minebox.co");
    }

    private static long getTimeOfDay(MinecraftClient client) {
        if (client.isIntegratedServerRunning() && client.getServer() != null && client.getServer().getOverworld() != null) {
            return client.getServer().getOverworld().getTimeOfDay();
        } else if (client.world != null) {
            return client.world.getTimeOfDay();
        }
        return 0;
    }

    private static void handleTimeChange(long t) {
        if (lastTime < 0) {
            lastTime = t;
            return;
        }
        long diff = Math.abs(t - lastTime);
        if (diff == 0) {
            noChangeCounter++;
            if (!hasErrorToastShown && noChangeCounter >= MAX_NO_CHANGE_TICKS) {
                showErrorToast();
                hasErrorToastShown = true;
            }
        } else if (diff > TIME_JUMP_THRESHOLD) {
            isTimeInitialized = true;
            noChangeCounter = 0;
            hasErrorToastShown = false;
        } else {
            isTimeInitialized = true;
            noChangeCounter = 0;
            hasErrorToastShown = false;
        }
        lastTime = t;
    }

    private static Set<String> getOpenShops(int hour, int minute) {
        Set<String> result = new HashSet<>();
        for (ShopTime st : SHOPS) {
            if (st.isOpen(hour, minute)) result.add(st.name());
        }
        return result;
    }

    private static void resetState() {
        hasJoinedWorld = false;
        ticksSinceWorldLoad = 0;
        isTimeInitialized = false;
        hasErrorToastShown = false;
        lastNotifiedHour = -1;
        lastNotifiedMinute = -1;
        lastOpenShops.clear();
        lastTime = -1;
        noChangeCounter = 0;
    }

    private static void showShopNotification(String shopName, String message) {
        if (!CONFIG.shopNotifications) return;
        if (!CONFIG.shopSettings.getOrDefault(shopName, true)) return;
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null) return;
        if (CONFIG.shopNotificationSound) c.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        c.getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.literal("Shop Info"), Text.literal(message)));
        if (CONFIG.shopChatMessages) sendChatMessage(message);
    }

    private static void sendChatMessage(String msg) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal(msg).formatted(Formatting.GREEN), false);
    }

    private static void showErrorToast() {
        if (!CONFIG.shopNotifications) return;
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null) return;
        if (CONFIG.shopNotificationSound) c.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f);
        c.getToastManager().add(new SystemToast(SystemToast.Type.CHUNK_LOAD_FAILURE, Text.literal("Warning"), Text.literal("Time not changing!")));
    }

    private record ShopTime(String name, int startHour, int startMinute, int endHour, int endMinute) {
        public boolean isOpen(int h, int m) {
            int cur = h * 60 + m;
            int st = startHour * 60 + startMinute;
            int en = endHour * 60 + endMinute;
            if (en < st) return cur >= st || cur <= en;
            return cur >= st && cur <= en;
        }
    }
}
