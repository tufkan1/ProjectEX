package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.client.ClientAlchemySessionState;
import io.github.tufkan1.projectex.client.ClientKnowledgeBrowserState;
import io.github.tufkan1.projectex.client.InputCompat;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Source-sized ProjectE transmutation panel backed by authoritative Fabric payloads. */
@Environment(EnvType.CLIENT)
public final class TransmutationScreen extends AbstractContainerScreen<TransmutationMenu> {
    private static final int PAGE_SIZE = 16;
    private static final int SEARCH_DEBOUNCE_TICKS = 5;
    private static final int[][] RESULT_POSITIONS = {
        {158, 9}, {176, 13}, {193, 30}, {199, 50}, {193, 70}, {176, 87}, {158, 91}, {140, 87},
        {123, 70}, {116, 50}, {123, 30}, {140, 13}, {158, 31}, {177, 50}, {158, 69}, {139, 50}
    };

    private final List<ResultButton> resultButtons = new ArrayList<>();
    private EditBox search;
    private Button previous;
    private Button next;
    private String selectedItem;
    private int searchDelay = -1;
    private long seenPageResponse = -2;
    private long seenActionResponse = -2;

    public TransmutationScreen(TransmutationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 228, 196);
        titleLabelX = 6;
        titleLabelY = 8;
        inventoryLabelY = -10_000;
    }

    @Override protected void init() {
        String previousSearch = search == null ? "" : search.getValue();
        resultButtons.clear();
        super.init();
        search = addRenderableWidget(new EditBox(font, leftPos + 83, topPos + 8, 55, 10, Component.empty()));
        search.setMaxLength(AlchemyNetworkProtocol.MAX_SEARCH_LENGTH);
        search.setValue(previousSearch);
        search.setResponder(value -> searchDelay = SEARCH_DEBOUNCE_TICKS);
        for (int index = 0; index < PAGE_SIZE; index++) {
            int buttonIndex = index;
            int[] position = RESULT_POSITIONS[index];
            resultButtons.add(addRenderableWidget(new ResultButton(
                leftPos + position[0], topPos + position[1], ignored -> create(buttonIndex))));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> changePage(-1))
            .bounds(leftPos + 125, topPos + 100, 14, 14).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> changePage(1))
            .bounds(leftPos + 193, topPos + 100, 14, 14).build());
        addRenderableWidget(new ActionSlotButton(leftPos + 43, topPos + 23,
            Component.translatable("screen.projectex.learn"), ignored -> actOnHeld(0)));
        addRenderableWidget(new ActionSlotButton(leftPos + 107, topPos + 97,
            Component.translatable("screen.projectex.burn"), ignored -> actOnHeld(1)));
        if (io.github.tufkan1.projectex.config.ProjectEXConfig.focusTransmutationSearch()) {
            setInitialFocus(search);
        }
        requestPage(0);
        refreshButtons();
    }

    @Override protected void containerTick() {
        super.containerTick();
        if (searchDelay > 0) searchDelay--;
        else if (searchDelay == 0) {
            searchDelay = -1;
            selectedItem = null;
            requestPage(0);
        }
        ClientKnowledgeBrowserState.Snapshot browser = ProjectEXClient.knowledge().snapshot();
        if (browser.lastResponseId() != seenPageResponse) {
            seenPageResponse = browser.lastResponseId();
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

    @Override public boolean keyPressed(KeyEvent event) {
        if (moveGridFocus(event.key())) return true;
        return super.keyPressed(event);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                              float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        ProjectEXGuiTextures.draw(graphics, ProjectEXGuiTextures.TRANSMUTATION,
            leftPos, topPos, imageWidth, imageHeight);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        ClientAlchemySessionState.Snapshot alchemy = ProjectEXClient.alchemy().snapshot();
        graphics.text(font, Component.translatable("screen.projectex.balance", alchemy.balance().toString()),
            6, imageHeight - 104, 0xFF404040, false);
        alchemy.lastFailure().or(() -> ProjectEXClient.knowledge().snapshot().lastFailure())
            .ifPresent(failure -> graphics.text(font, failureMessage(failure), 6,
                imageHeight - 84, 0xFFB02020, false));
    }

    @Override public void removed() {
        super.removed();
        ProjectEXClient.alchemy().close();
        ProjectEXClient.knowledge().close();
    }

    @Override public Component getNarrationMessage() {
        ClientAlchemySessionState.Snapshot alchemy = ProjectEXClient.alchemy().snapshot();
        ClientKnowledgeBrowserState.Snapshot browser = ProjectEXClient.knowledge().snapshot();
        return Component.translatable("screen.projectex.narration", alchemy.balance().toString(),
            browser.totalPages() == 0 ? 0 : browser.page() + 1, browser.totalPages(),
            browser.totalEntries(), alchemy.lastFailure().or(() -> browser.lastFailure())
                .<Component>map(TransmutationScreen::failureMessage)
                .orElseGet(() -> Component.translatable("screen.projectex.status.ready")));
    }

    private void create(int index) {
        List<AlchemyKnowledgePagePayload.Entry> entries = ProjectEXClient.knowledge().snapshot().entries();
        if (index < entries.size()) {
            selectedItem = entries.get(index).itemId();
            ProjectEXClient.sendAction(2, selectedItem, 1);
        }
    }

    private void actOnHeld(int operation) {
        if (minecraft == null || minecraft.player == null) return;
        ItemStack held = minecraft.player.getInventory().getSelectedItem();
        if (held.isEmpty() || !held.getComponentsPatch().isEmpty()) return;
        BuiltInRegistries.ITEM.getResourceKey(held.getItem()).ifPresent(key ->
            ProjectEXClient.sendAction(operation, key.identifier().toString(), 1));
    }

    private void changePage(int direction) {
        ClientKnowledgeBrowserState.Snapshot browser = ProjectEXClient.knowledge().snapshot();
        int target = browser.page() + direction;
        if (target >= 0 && target < browser.totalPages()) requestPage(target);
    }

    private void requestPage(int page) {
        ProjectEXClient.requestKnowledge(search == null ? "" : search.getValue(), page, PAGE_SIZE);
    }

    private boolean moveGridFocus(int key) {
        int current = resultButtons.indexOf(getFocused());
        if (current < 0) return false;
        int visible = ProjectEXClient.knowledge().snapshot().entries().size();
        int target = key == InputCompat.KEY_LEFT ? current - 1
            : key == InputCompat.KEY_RIGHT ? current + 1 : -1;
        if (target < 0 || target >= visible) return false;
        setFocused(resultButtons.get(target));
        return true;
    }

    private void refreshButtons() {
        ClientKnowledgeBrowserState.Snapshot browser = ProjectEXClient.knowledge().snapshot();
        for (int index = 0; index < resultButtons.size(); index++) {
            ResultButton button = resultButtons.get(index);
            if (index >= browser.entries().size()) {
                button.setEntry(null);
            } else {
                button.setEntry(browser.entries().get(index));
            }
        }
        previous.active = browser.page() > 0;
        next.active = browser.page() + 1 < browser.totalPages();
    }

    private static Component failureMessage(io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure failure) {
        return Component.translatable("screen.projectex.failure."
            + failure.name().toLowerCase(Locale.ROOT));
    }

    private static class ActionSlotButton extends Button {
        ActionSlotButton(int x, int y, Component message, OnPress press) {
            super(x, y, 18, 18, message, press, DEFAULT_NARRATION);
        }
        @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                 float partialTick) {
            if (isHoveredOrFocused()) graphics.outline(getX(), getY(), width, height, 0xFFFFFFFF);
        }
    }

    private static final class ResultButton extends ActionSlotButton {
        private ItemStack stack = ItemStack.EMPTY;
        ResultButton(int x, int y, OnPress press) { super(x, y, Component.empty(), press); }
        void setEntry(AlchemyKnowledgePagePayload.Entry entry) {
            if (entry == null) {
                stack = ItemStack.EMPTY;
                setMessage(Component.empty());
                visible = false;
                active = false;
                return;
            }
            Identifier id = Identifier.parse(entry.itemId());
            stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
            setMessage(Component.literal(entry.itemId() + " · " + entry.emc()));
            visible = true;
            active = true;
        }
        @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                 float partialTick) {
            if (!stack.isEmpty()) graphics.fakeItem(stack, getX() + 1, getY() + 1);
            super.extractContents(graphics, mouseX, mouseY, partialTick);
            if (isHovered()) graphics.setTooltipForNextFrame(getMessage(), mouseX, mouseY);
        }
    }
}
