package net.prism.notra;

import net.fabricmc.api.ModInitializer;

import net.prism.notra.config.NotraConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Notra implements ModInitializer {
	public static final String MOD_ID = "notra";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		NotraConfig.getInstance();
		LOGGER.info("NotraConfig oInitialized");
	}
}