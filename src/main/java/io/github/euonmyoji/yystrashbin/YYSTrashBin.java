package io.github.euonmyoji.yystrashbin;

import com.google.inject.Inject;
import org.bstats.sponge.Metrics2;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * @author yinyangshi
 */
@Plugin(id = "yystrashbin", name = "YYSTrashBin",
        version = "@spongeVersion@", description = "throw trash", authors = "yinyangshi")
public class YYSTrashBin {
    private static final HashMap<UUID, ItemStack> map = new HashMap<>();
    @Inject
    private Metrics2 metrics;

    @Listener
    public void onStarted(GameStartedServerEvent ignore) {
        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .permission("yystrashbin.command.trashbin")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        ItemStack barrier = ItemStack.of(ItemTypes.BARRIER);
                        barrier.offer(Keys.DISPLAY_NAME, Text.of(""));
                        Inventory inv = Inventory.builder()
                                .of(InventoryArchetypes.CHEST)
                                .listener(ClickInventoryEvent.class, event -> {
                                    for (SlotTransaction transaction : event.getTransactions()) {
                                        transaction.getSlot().getProperty(SlotIndex.class, SlotIndex.getDefaultKey(SlotIndex.class))
                                                .ifPresent(slotIndex -> {
                                                    if (slotIndex.getValue() != null && slotIndex.getValue() < 27) {
                                                        if (slotIndex.getValue() != null && slotIndex.getValue() != 13) {
                                                            event.setCancelled(true);
                                                        } else if (event instanceof ClickInventoryEvent.Primary && transaction.getOriginal().getType() != ItemTypes.AIR) {
                                                            event.getCursorTransaction().setCustom(ItemStack.empty().createSnapshot());
                                                        }
                                                    }
                                                });
                                        if (event.isCancelled()) {
                                            return;
                                        }
                                        if (event instanceof ClickInventoryEvent.Shift && transaction.getDefault().getType() != ItemTypes.AIR) {
                                            if (transaction.getDefault() == transaction.getOriginal() && transaction.getOriginal() == transaction.getFinal()) {
                                                transaction.setCustom(ItemStack.empty());
                                                event.getTargetInventory().query(QueryOperationTypes.INVENTORY_PROPERTY.of(SlotIndex.of(8 + 5)))
                                                        .set(transaction.getOriginal().createStack());
                                            }
                                        }
                                    }
                                })
                                .listener(InteractInventoryEvent.Close.class, event -> {
                                    Optional<ItemStack> item = event.getTargetInventory()
                                            .query(QueryOperationTypes.INVENTORY_PROPERTY.of(SlotIndex.of(8 + 5)))
                                            .peek();
                                    if (item.isPresent()) {
                                        map.put(((Player) src).getUniqueId(), item.get());
                                    } else {
                                        map.remove(((Player) src).getUniqueId());
                                    }
                                })
                                .build(this);
                        for (int i = 0; i < 3 * 9; ++i) {
                            if (i == 8 + 5) {
                                inv.query(QueryOperationTypes.INVENTORY_PROPERTY.of(SlotIndex.of(i)))
                                        .set(map.getOrDefault(((Player) src).getUniqueId(), ItemStack.empty()));
                            } else {
                                inv.query(QueryOperationTypes.INVENTORY_PROPERTY.of(SlotIndex.of(i)))
                                        .set(barrier);
                            }
                        }
                        ((Player) src).openInventory(inv);
                    }
                    return CommandResult.success();
                })
                .build(), "yystrashbin", "trashbin", "trash", "bin", "tb", "wastebin");
    }

    public void onDisconnect(ClientConnectionEvent.Disconnect event) {
        map.remove(event.getTargetEntity().getUniqueId());
    }

}
