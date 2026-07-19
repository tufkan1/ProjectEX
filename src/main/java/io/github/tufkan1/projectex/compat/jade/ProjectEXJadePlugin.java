package io.github.tufkan1.projectex.compat.jade;

import io.github.tufkan1.projectex.content.AlchemyStorageBlock;
import io.github.tufkan1.projectex.content.AutomationBlock;
import io.github.tufkan1.projectex.content.EmcMachineBlock;
import io.github.tufkan1.projectex.content.automation.AutomationBlockEntity;
import io.github.tufkan1.projectex.content.machine.EmcMachineBlockEntity;
import io.github.tufkan1.projectex.content.storage.AlchemyStorageBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;

/** Optional Jade entrypoint. Fabric Loader never resolves this class when Jade is absent. */
public final class ProjectEXJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ProjectEXMachineProvider.Server.INSTANCE,
            EmcMachineBlockEntity.class);
        registration.registerBlockDataProvider(ProjectEXStorageProvider.Server.INSTANCE,
            AlchemyStorageBlockEntity.class);
        registration.registerBlockDataProvider(ProjectEXAutomationProvider.Server.INSTANCE,
            AutomationBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ProjectEXMachineProvider.Client.INSTANCE,
            EmcMachineBlock.class);
        registration.registerBlockComponent(ProjectEXStorageProvider.Client.INSTANCE,
            AlchemyStorageBlock.class);
        registration.registerBlockComponent(ProjectEXAutomationProvider.Client.INSTANCE,
            AutomationBlock.class);
    }
}
