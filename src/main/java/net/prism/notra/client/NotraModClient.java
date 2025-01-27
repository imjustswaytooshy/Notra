package net.prism.notra.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.prism.notra.config.NotraConfig;
import net.prism.notra.shop.ShopNotifier;

public class NotraModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NotraConfig.getInstance();
        ClientTickEvents.END_CLIENT_TICK.register(ShopNotifier::tick);
    }
}