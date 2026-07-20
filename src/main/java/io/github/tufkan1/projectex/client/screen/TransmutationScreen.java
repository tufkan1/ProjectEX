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
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import io.github.tufkan1.projectex.client.InputCompat;

/** Accessible transmutation screen using the original ProjectE panel and authoritative caches. */
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
        super(menu, inventory, title, 408, 196);
        inventoryLabelX = 44;
        inventoryLabelY = 140;
    }

    @Override
    protected void init() {
        String previousSearch = search == null ? "" : search.getValue();
        resultButtons.clear();
        super.init();
        search = addRenderableWidget(new EditBox(
            font, leftPos + 236, topPos + 20, 164, 18,
            Component.translatable("screen.projectex.search")
        ));
        search.setMaxLength(AlchemyNetworkProtocol.MAX_SEARCH_LENGTH);
        search.setHint(Component.translatable("screen.projectex.search"));
        search.setValue(previousSearch);
        search.setResponder(value -> searchDelay = SEARCH_DEBOUNCE_TICKS);

        for (int index = 0; index < PAGE_SIZE; index++) {
            int buttonIndex = index;
            int column = index % 2;
            int row = index / 2;
            Button button = Button.builder(Component.empty(), ignored -> select(buttonIndex))
                .bounds(leftPos + 236 + column * 83, topPos + 43 + row * 22, 80, 20)
                .build();
            resultButtons.add(addRenderableWidget(button));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> changePage(-1))
            .bounds(leftPos + 236, topPos + 111, 36, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> changePage(1))
            .bounds(leftPos + 275, topPos + 111, 36, 20).build());
        create = addRenderableWidget(Button.builder(
            Component.translatable("screen.projectex.create"), ignored -> createSelected())
            .bounds(leftPos + 314, topPos + 111, 86, 20).build());
        favorite = addRenderableWidget(Button.builder(
            Component.translatable("screen.projectex.favorite"), ignored -> favoriteSelected())
            .bounds(leftPos + 236, topPos + 135, 80, 20).build());
        addRenderableWidget(Button.builder(
            Component.translatable("screen.projectex.learn"), ignored -> actOnHeld(0))
            .bounds(leftPos + 320, topPos + 135, 80, 20).build());
        addRenderableWidget(Button.builder(
            Component.translatable("screen.projectex.burn"), ignored -> actOnHeld(1))
            .bounds(leftPos + 320, topPos + 159, 80, 20).build());
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
    public boolean keyPressed(KeyEvent event) {
        if (moveGridFocus(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        ProjectEXGuiTextures.draw(graphics, ProjectEXGuiTextures.TRANSMUTATION,
            leftPos, topPos, 228, 196);
        graphics.fill(leftPos + 228, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE17120C);
        graphics.outline(leftPos + 228, topPos, imageWidth - 228, imageHeight, 0xFF8A6A2F);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 236, 7, 0xFFE8D5A5, false);
        ClientAlchemySessionState.Snapshot alchemy = ProjectEXClient.alchemy().snapshot();
        ClientKnowledgeBrowserState.Snapshot browser = ProjectEXClient.knowledge().snapshot();
        graphics.text(font, Component.translatable(
            "screen.projectex.balance", alchemy.balance().toString()), 236, 183, 0xFFFFFFFF, false);
        graphics.text(font, Component.translatable(
            "screen.projectex.page", browser.totalPages() == 0 ? 0 : browser.page() + 1,
            browser.totalPages(), browser.totalEntries()), 236, 100, 0xFFE8D5A5, false);
        alchemy.lastFailure().or(() -> browser.lastFailure()).ifPresent(failure ->
            graphics.text(font, Component.translatable(
                "screen.projectex.failure", failureMessage(failure)),
                236, 88, 0xFFFF7070, false));
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
                .<Component>map(TransmutationScreen::failureMessage)
                .orElseGet(() -> Component.translatable("screen.projectex.status.ready"))
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

    private boolean moveGridFocus(int key) {
        int current = resultButtons.indexOf(getFocused());
        if (current < 0) {
            return false;
        }
        int visible = ProjectEXClient.knowledge().snapshot().entries().size();
        int target = -1;
        if (key == InputCompat.KEY_LEFT) target = current % 3 > 0 ? current - 1 : -1;
        else if (key == InputCompat.KEY_RIGHT)
            target = current % 3 < 2 && current + 1 < visible ? current + 1 : -1;
        else if (key == InputCompat.KEY_UP) target = current >= 3 ? current - 3 : -1;
        else if (key == InputCompat.KEY_DOWN) target = current + 3 < visible ? current + 3 : -1;
        if (target < 0) {
            return false;
        }
        setFocused(resultButtons.get(target));
        triggerImmediateNarration(true);
        return true;
    }

    private static Component failureMessage(io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure failure) {
        return Component.translatable(
            "screen.projectex.failure." + failure.name().toLowerCase(Locale.ROOT));
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
