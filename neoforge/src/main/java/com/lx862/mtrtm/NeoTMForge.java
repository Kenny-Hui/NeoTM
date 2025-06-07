package com.lx862.mtrtm;

import com.lx862.mtrtm.loader.neoforge.LoaderImpl;
import com.lx862.mtrtm.mod.NeoTM;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.function.Consumer;

@Mod(NeoTM.MOD_ID)
public class NeoTMForge {
	static {
		NeoTM.init();
	}

	public NeoTMForge(IEventBus eventBus) {
		NeoForge.EVENT_BUS.register(Events.class);
	}

	public static class Events {
		@SubscribeEvent
		public static void registerCommand(RegisterCommandsEvent registerCommandsEvent) {
			for(Consumer<RegisterCommandsEvent> listener : LoaderImpl.commandsEventListeners) {
				listener.accept(registerCommandsEvent);
			}
		}
	}
}
