package com.stiggles.smp5.entity.npc.shopnpcs;

import com.stiggles.smp5.main.SMP5;
import de.studiocode.invui.gui.builder.GUIBuilder;
import de.studiocode.invui.gui.builder.guitype.GUIType;
import de.studiocode.invui.item.ItemProvider;
import de.studiocode.invui.item.builder.ItemBuilder;
import de.studiocode.invui.item.impl.BaseItem;
import de.studiocode.invui.item.impl.SimpleItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Random;





public class Starry extends ShopNPC {


    private class CountItem extends BaseItem {

        private int count;

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.DIAMOND).setDisplayName("Count: " + count);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType.isLeftClick()) {
                count++; // increment if left click
            } else {
                count--; // else decrement
            }

            notifyWindows(); // this will update the ItemStack that is displayed to the player
        }

    }

    SMP5 main;

    public Starry (SMP5 main) {
        super (main, "Starry");

        SetSkin (
                "ewogICJ0aW1lc3RhbXAiIDogMTY3MTY0NTIwOTYxMCwKICAicHJvZmlsZUlkIiA6ICJjZDc1OTJmNzBlYmE0MGIyODFiNjRhNDk3YzAzYTdhMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTdGFycnlfUGhvZW5peCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lMDFkNGE0ZmE3NzRmMTY2NjljYmFkYmRmMjFiZjNlOGFkNDgxMGRiZThmZmFhZDU3YWE1MmM5NzA4MTlkMDciCiAgICB9LAogICAgIkNBUEUiIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzIzNDBjMGUwM2RkMjRhMTFiMTVhOGIzM2MyYTdlOWUzMmFiYjIwNTFiMjQ4MWQwYmE3ZGVmZDYzNWNhN2E5MzMiCiAgICB9CiAgfQp9",
                "p8OLoYFlwB1OOERA6obVq+km/EfyEV/qTEpc6yayvRxXdN4eE9j45aBmap5W0ulJoL0By4yd5AhySLJHCBFCUt9fCuBZQkxT7foI2ZRSMRk3U/f4Pm6oH10Gi8OqOtJc+aH3A7L3HdCZXijjZtIUsN2/fF4Jvs01JQuN+e0PHP10KcwBygdR93U3euDD+xheURYx7DNb3wbSIpPELj6Wp4TpGHL83LmPcKBbSJag9dIA80jiScjNq74whrDmzyObsDR1Umo77pkiCjqvGV9pIZIMIwNzU112DHENPJFcFhTyuNO7W+dpjYh+pSpBAXa06xBwzoXsMeqEVwUqpFLFy+BV/mgL4wnXTbPZfDxXNVneUs+2y+/nY3Fgbk9sKwf952xff9ProEXI9tjAMAbA3/ZUHmFv9b5quHJ9YhnfqIf48Fw4GX/pfw0eMhRyN8LwyKB97Y7arjOWm7h4BNoYJIwvZvSP+nGDiNKl2FqbWAFGpLPRf145pD0vIQ/CewAg/tV2tfoEC01d8HvpnnotWoRmCgIWVq0j9OcBVFxme8c9u4+b50XEpbiRLPGpBPkfk4EW4oF6tLw6SuhdcOTfKx9yjY4tOZeWiBDp1yX3/ZByzclIdfbhIqbmsoAu7zojRX0rfRrLVxaWLhrO225eC1SHSfGnZ7iLIcn5h4g2D40="
        );

        SetPos (-4.5, -59, 3.5);
    }

    @Override
    public void createGUI () {
        gui = new GUIBuilder<>(GUIType.NORMAL)
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . ! . . . #",
                        "# . . . . . . . #",
                        "# # # # # # # # #")
                .addIngredient ('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
                .addIngredient('!', new CountItem())
                .build ();

    }

    @Override
    public void InteractDialogue(Player p) {
        String msg = "Welcome to the Spectral Saloon!";

        Random rand = new Random();
        int ni = rand.nextInt () % 9;

        if (ni <= 2)
            msg += " Stay as long as you'd like";
        else if (ni <= 5)
            msg += " Be sure to get some of our famous Moonshine!";
        else
            msg += " We're happy to have you!";

        p.sendMessage ("<" + getName () + "> " + msg);
    }
}