package me.kanuunankuulaspluginsMobs.actionrecorder.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (Screen parent) -> {
            try {
                return new ActionRecorderConfigScreen(parent);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        };
    }
}