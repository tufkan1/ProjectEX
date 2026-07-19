package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.client.ClientAlchemySessionState;
import io.github.tufkan1.projectex.client.ClientKnowledgeBrowserState;
import io.github.tufkan1.projectex.client.ProjectEXClient;
import io.github.tufkan1.projectex.menu.TransmutationMenu;
import io.github.tufkan1.projectex.network.AlchemyKnowledgePagePayload;
import io.github.tufkan1.projectex.network.AlchemyNetworkProtocol;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Accessible button-based transmutation screen backed only by authoritative client caches. */
@Environment(EnvType.CLIENT)
public final class TransmutationScreen extends AbstractContainerScreen<TransmutationMenu> {
    private static final int PAGE_SIZE = 6;
    private static final int SEARCH_DEBOUNCE_TICKS = 5;

    private final List<Button> resultButtons = new ArrayList<>();
    private EditBox search;
    private Button previous;
    private Button next;
    private Button create;
    private Button favorite;
    private String selectedItem;
    private int searchDelay = -1;
    private long seenPageResponse = -2;
    private long seenActionResponse = -2;

    public TransmutationScreen(TransmutationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 248, 232);
        inventoryLabelX = 44;
        inventoryLabelY = 140;
    }

    @Override
    protected void init() {
        String previousSearch = search == null ? "" : search.getValue();
        resultButtons.clear();
        super.init();
        search = addRenderableWidget(new EditBox(
            font, leftPos + 8, topPos + 20, 232, 18,
            Component.translatable("screen.projectex.search")
        ));
        search.setMaxLength(AlchemyNetworkProtocol.MAX_SEARCH_LENGTH);
        search.setHint(Component.translatable("screen.projectex.search"));
        search.setValue(previousSearch);
        search.setResponder(value -> searchDelay = SEARCH_DEBOUNCE_TICKS);

        for (int index = 0; index < PAGE_SIZE; index++) {
            int buttonIndex = index;
            int column = index % 3;
            int row = index / 3;
            Button button = Button.builder(Component.empty(), ignored -> select(buttonIndex))
                .bounds(leftPos + 8 + column * 78, topPos + 44 + row * 22, 74, 20)
                .build();
            resultButtons.add(addRenderableWidget(button));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> changePage(-1))
            .bounds(leftPos + 8, topPos + 90, 36, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> changePage(1))
            .bounds(leftPos + 48, topPos + 90, 36, 20).build());
        create = addRenderableWidget(Button.builder(
            Component.translatable("screen.projectex.create"), ignored -> createSelected())
            .bounds(leftPos + 88, topPos + 90, 48, 20).build());
        favorite = addRenderableWidget(Button.builder(
            Component.translatable("screen.projectex.favorite"), ignored -> favoriteSelected())
            .bounds(leftPos + 140, topPos + 90, 48, 20).build());
        addRenderableWidget(Button.builder(
            Component.translatable("screen.projectex.learn"), ignored -> actOnHeld(0))
            .bounds(leftPos + 192, topPos + 90, 48, 20).build());
        addRenderableWidget(Button.builder(
            Component.translatable("screen.projectex.burn"), ignored -> actOnHeld(1))
            .bounds(leftPos + 192, topPos + 114, 48, 20).build());
        setInitialFocus(search);
        requestPage(0);
        refreshButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (searchDelay > 0) {
            searchDelay--;
        } else if (searchDelay == 0) {
            searchDelay = -1;
            selectedItem = null;
            requestPage(0);
        }
        ClientKnowledgeBrowserState.Snapshot browser = ProjectEXClient.knowledge().snapshot();
        if (browser.lastResponseId() != seenPageResponse) {
            seenPageResponse = browser.lastResponseId();
            if (selectedItem != null && browser.entries().stream()
                .noneMatch(entry -> entry.itemId().equals(selectedItem))) {
                selectedItem = null;
            }
            refreshButtons();
            triggerImmediateNarration(true);
        }
        ClientAlchemySessionState.Snapshot alchemy = ProjectEXClient.alchemy().snapshot();
        if (alchemy.lastResponseId() != seenActionResponse) {
            seenActionResponse = alchemy.lastResponseId();
            requestPage(browser.page());
            triggerImmediateNarration(true);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0101010);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF8A6A2F);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFE8D5A5, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE8D5A5, false);
        ClientAlchemySessionState.Snapshot alchemy = ProjectEXClient.alchemy().snapshot();
        ClientKnowledgeBrowserState.Snapshot browser = ProjectEXClient.knowledge().snapshot();
        graphics.text(font, Component.translatable(
            "screen.projectex.balance", alchemy.balance().toString()), 100, 6, 0xFFFFFFFF, false);
        graphics.text(font, Component.translatable(
            "screen.projectex.page", browser.totalPages() == 0 ? 0 : browser.page() + 1,
            browser.totalPages(), browser.totalEntries()), 8, 116, 0xFFE8D5A5, false);
        alchemy.lastFailure().or(() -> browser.lastFailure()).ifPresent(failure ->
            graphics.text(font, Component.translatable(
                "screen.projectex.failure", failure.name().toLowerCase(Locale.ROOT)),
                8, 128, 0xFFFF7070, false));
    }

    @Override
    public void removed() {
        super.removed();
        ProjectEXClient.alchemy().close();
        ProjectEXClient.knowledge().close();
    }

    @Override
    public Component getNarrationMessage() {
        ClientAlchemySessionState.Snapshot alchemy = ProjectEXClient.alchemy().snapshot();
        ClientKnowledgeBrowserState.Snapshot browser = ProjectEXClient.knowledge().snapshot();
        return Component.translatable(
            "screen.projectex.narration",
            alchemy.balance().toString(),
            browser.totalPages() == 0 ? 0 : browser.page() + 1,
            browser.totalPages(),
            browser.totalEntries(),
            alchemy.lastFailure().or(() -> browser.lastFailure())
                .map(failure -> failure.name().toLowerCase(Locale.ROOT))
                .orElse("none")
        );
    }

    private void select(int index) {
        List<AlchemyKnowledgePagePayload.Entry> entries = ProjectEXClient.knowledge().snapshot().entries();
        if (index < entries.size()) {
            selectedItem = entries.get(index).itemId();
            refreshButtons();
            triggerImmediateNarration(true);
        }
    }

    private void createSelected() {
        if (selectedItem != null) {
            ProjectEXClient.sendAction(2, selectedItem, 1);
        }
    }

    private void favoriteSelected() {
        if (selectedItem != null) {
            ProjectEXClient.toggleFavorite(selectedItem);
            refreshButtons();
            triggerImmediateNarration(true);
        }
    }

    private void actOnHeld(int operation) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        ItemStack held = minecraft.player.getInventory().getSelectedItem();
        if (held.isEmpty() || !held.getComponentsPatch().isEmpty()) {
            return;
        }
        BuiltInRegistries.ITEM.getResourceKey(held.getItem()).ifPresent(key ->
            ProjectEXClient.sendAction(operation, key.identifier().toString(), 1));
    }

    private void changePage(int direction) {
        ClientKnowledgeBrowserState.Snapshot browser = ProjectEXClient.knowledge().snapshot();
        int target = browser.page() + direction;
        if (target >= 0 && target < browser.totalPages()) {
            selectedItem = null;
            requestPage(target);
        }
    }

    private void requestPage(int page) {
        ProjectEXClient.requestKnowledge(search == null ? "" : search.getValue(), page, PAGE_SIZE);
    }

    private void refreshButtons() {
        ClientKnowledgeBrowserState.Snapshot browser = ProjectEXClient.knowledge().snapshot();
        for (int index = 0; index < resultButtons.size(); index++) {
            Button button = resultButtons.get(index);
            if (index >= browser.entries().size()) {
                button.visible = false;
                button.active = false;
                continue;
            }
            AlchemyKnowledgePagePayload.Entry entry = browser.entries().get(index);
            boolean selected = entry.itemId().equals(selectedItem);
            boolean starred = browser.favorites().contains(entry.itemId());
            button.setMessage(Component.literal(
                (selected ? "> " : "") + (starred ? "* " : "")
                    + entry.itemId() + " · " + entry.emc()));
            button.visible = true;
            button.active = true;
        }
        previous.active = browser.page() > 0;
        next.active = browser.page() + 1 < browser.totalPages();
        create.active = selectedItem != null;
        favorite.active = selectedItem != null;
        favorite.setMessage(Component.translatable(
            selectedItem != null && browser.favorites().contains(selectedItem)
                ? "screen.projectex.unfavorite"
                : "screen.projectex.favorite"));
    }
}
