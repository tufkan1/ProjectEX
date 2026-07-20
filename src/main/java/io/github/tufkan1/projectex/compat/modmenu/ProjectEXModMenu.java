package io.github.tufkan1.projectex.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.tufkan1.projectex.client.screen.ProjectEXConfigScreen;

/** Optional Mod Menu bridge; ProjectEX remains fully usable without Mod Menu. */
public final class ProjectEXModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ProjectEXConfigScreen::new;
    }
}
