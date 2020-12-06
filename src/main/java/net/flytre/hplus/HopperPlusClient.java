package net.flytre.hplus;

import net.fabricmc.api.ClientModInitializer;

public class HopperPlusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RegistryHandler.onInitClient();
    }
}
