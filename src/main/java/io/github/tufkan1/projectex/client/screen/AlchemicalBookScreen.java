package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.client.ProjectEXClient;
import io.github.tufkan1.projectex.network.AlchemicalBookAction;
import io.github.tufkan1.projectex.network.AlchemicalBookViewPayload;
import io.github.tufkan1.projectex.teleport.AlchemicalDestination;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Keyboard/narrator-friendly editor over the original ProjectExpansion tablet panel. */
@Environment(EnvType.CLIENT)
public final class AlchemicalBookScreen extends Screen {
    private static final int PAGE_SIZE = 6;
    private final List<Button> destinationButtons = new ArrayList<>();
    private EditBox name;
    private Button previous;
    private Button next;
    private Button create;
    private Button delete;
    private Button teleport;
    private Button back;
    private int page;
    private String selected;
    private long seenRequest = Long.MIN_VALUE;

    public AlchemicalBookScreen() {
        super(Component.translatable("screen.projectex.alchemical_book.title"));
    }

    @Override protected void init() {
        destinationButtons.clear();
        int left = (width - 300) / 2;
        int top = (height - 210) / 2;
        name = addRenderableWidget(new EditBox(font, left + 10, top + 28, 190, 20,
            Component.translatable("screen.projectex.alchemical_book.name")));
        name.setMaxLength(AlchemicalDestination.MAX_NAME_LENGTH);
        name.setHint(Component.translatable("screen.projectex.alchemical_book.name"));
        create = addRenderableWidget(Button.builder(Component.translatable("screen.projectex.alchemical_book.create"),
            ignored -> act(AlchemicalBookAction.CREATE, name.getValue()))
            .bounds(left + 205, top + 28, 85, 20).build());
        for (int index = 0; index < PAGE_SIZE; index++) {
            int slot = index;
            destinationButtons.add(addRenderableWidget(Button.builder(Component.empty(), ignored -> select(slot))
                .bounds(left + 10, top + 54 + index * 21, 280, 20).build()));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> changePage(-1))
            .bounds(left + 10, top + 184, 35, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> changePage(1))
            .bounds(left + 50, top + 184, 35, 20).build());
        teleport = addRenderableWidget(Button.builder(Component.translatable("screen.projectex.alchemical_book.teleport"),
            ignored -> act(AlchemicalBookAction.TELEPORT, selected == null ? "" : selected))
            .bounds(left + 90, top + 184, 62, 20).build());
        delete = addRenderableWidget(Button.builder(Component.translatable("screen.projectex.alchemical_book.delete"),
            ignored -> act(AlchemicalBookAction.DELETE, selected == null ? "" : selected))
            .bounds(left + 157, top + 184, 55, 20).build());
        back = addRenderableWidget(Button.builder(Component.translatable("screen.projectex.alchemical_book.back"),
            ignored -> act(AlchemicalBookAction.BACK, ""))
            .bounds(left + 217, top + 184, 73, 20).build());
        setInitialFocus(name);
        refresh();
    }

    @Override public void tick() {
        super.tick();
        ProjectEXClient.alchemicalBook().view().ifPresent(view -> {
            if (view.requestId() != seenRequest) {
                seenRequest = view.requestId();
                if (selected != null && view.entries().stream()
                    .noneMatch(entry -> entry.destination().name().equals(selected))) selected = null;
                refresh();
                triggerImmediateNarration(true);
            }
        });
    }

    @Override public void extractRenderState(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = (width - 300) / 2;
        int top = (height - 210) / 2;
        ProjectEXClient.alchemicalBook().view().ifPresent(view -> {
            graphics.text(font, title, left + 10, top + 8, 0xFFFFE3A0, false);
            graphics.text(font, Component.translatable("screen.projectex.alchemical_book.balance", view.balance()),
                left + 180, top + 8, 0xFFFFFFFF, false);
            if (!view.failure().isEmpty()) graphics.text(font, Component.translatable(
                "screen.projectex.alchemical_book.failure." + view.failure()), left + 10, top + 166,
                0xFFFF7070, false);
        });
    }

    @Override public void extractBackground(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        int left = (width - 300) / 2;
        int top = (height - 210) / 2;
        graphics.fill(left, top, left + 300, top + 210, 0xEE17120C);
        ProjectEXGuiTextures.draw(graphics, ProjectEXGuiTextures.ALCHEMICAL_BOOK,
            left + 22, top, 256, 210);
        graphics.fill(left, top, left + 300, top + 210, 0x3517120C);
        graphics.outline(left, top, 300, 210, 0xFFD5A94E);
    }

    @Override public Component getNarrationMessage() {
        return ProjectEXClient.alchemicalBook().view().<Component>map(view -> Component.translatable(
            "screen.projectex.alchemical_book.narration", view.balance(), view.entries().size(),
            view.failure().isEmpty() ? Component.translatable("screen.projectex.status.ready")
                : Component.translatable("screen.projectex.alchemical_book.failure." + view.failure())))
            .orElse(title);
    }

    @Override public void removed() {
        ProjectEXClient.sendAlchemicalBookAction(AlchemicalBookAction.CLOSE, "");
        ProjectEXClient.alchemicalBook().close();
        super.removed();
    }

    private void select(int slot) {
        List<AlchemicalBookViewPayload.Entry> entries = entries();
        int index = page * PAGE_SIZE + slot;
        if (index < entries.size()) {
            selected = entries.get(index).destination().name();
            refresh();
            triggerImmediateNarration(true);
        }
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(maxPage(), page + delta));
        selected = null;
        refresh();
    }

    private void act(AlchemicalBookAction action, String value) {
        ProjectEXClient.sendAlchemicalBookAction(action, value);
    }

    private List<AlchemicalBookViewPayload.Entry> entries() {
        return ProjectEXClient.alchemicalBook().view().map(AlchemicalBookViewPayload::entries).orElse(List.of());
    }

    private int maxPage() { return Math.max(0, (entries().size() - 1) / PAGE_SIZE); }

    private void refresh() {
        List<AlchemicalBookViewPayload.Entry> entries = entries();
        page = Math.min(page, maxPage());
        for (int slot = 0; slot < destinationButtons.size(); slot++) {
            Button button = destinationButtons.get(slot);
            int index = page * PAGE_SIZE + slot;
            if (index >= entries.size()) { button.visible = false; button.active = false; continue; }
            AlchemicalBookViewPayload.Entry entry = entries.get(index);
            button.visible = true; button.active = true;
            button.setMessage(Component.literal((entry.destination().name().equals(selected) ? "> " : "")
                + entry.destination().name() + " · " + entry.cost() + " EMC · "
                + entry.destination().dimension()));
        }
        var view = ProjectEXClient.alchemicalBook().view();
        boolean editable = view.map(AlchemicalBookViewPayload::editable).orElse(false);
        create.active = editable;
        delete.active = editable && selected != null;
        teleport.active = selected != null;
        back.active = view.flatMap(AlchemicalBookViewPayload::back).isPresent();
        previous.active = page > 0;
        next.active = page < maxPage();
    }
}
