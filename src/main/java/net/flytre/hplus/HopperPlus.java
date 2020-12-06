package net.flytre.hplus;

import net.fabricmc.api.ModInitializer;


public class HopperPlus implements ModInitializer {
	@Override
	public void onInitialize() {
		System.out.println("Loading Hopper+");
		RegistryHandler.onInit();
	}


}
