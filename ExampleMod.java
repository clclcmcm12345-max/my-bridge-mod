package com.github.KINGkorea.mybridgemod;

import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.lang.reflect.Field;

@Mod(modid = "mybridgemod", useMetadata = true)
public class ExampleMod {

    public enum BridgeMode {
        SCAFFOLD("스카폴드 (Scaffold)"),
        SPEED_TELLY("스피드 텔리 (Speed Telly)"),
        TELLY("일반 텔리 (Telly Bridge)"),
        DAO_TELLY("다오 텔리 (0 CPS Dao Telly)");

        public final String displayName;
        BridgeMode(String name) { this.displayName = name; }
    }

    public static boolean enabled = false;
    public static BridgeMode currentMode = BridgeMode.TELLY;

    private final Minecraft mc = Minecraft.getMinecraft();
    private int airTicks = 0;
    private Field rightClickDelayField;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        try {
            rightClickDelayField = Minecraft.class.getDeclaredField("field_71467_ac");
        } catch (NoSuchFieldException e) {
            try {
                rightClickDelayField = Minecraft.class.getDeclaredField("rightClickDelayTimer");
            } catch (NoSuchFieldException ignored) {}
        }
        if (rightClickDelayField != null) {
            rightClickDelayField.setAccessible(true);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState()) {
            int key = Keyboard.getEventKey();
            if (key == Keyboard.KEY_RSHIFT) {
                mc.displayGuiScreen(new BridgeGuiScreen());
            } else if (key == Keyboard.KEY_V) {
                toggleMod();
            }
        }
    }

    public static void toggleMod() {
        enabled = !enabled;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            if (enabled) {
                mc.thePlayer.addChatMessage(new ChatComponentText("§a[AutoBridge] 활성화됨 - 모드: §e" + currentMode.displayName));
            } else {
                mc.thePlayer.addChatMessage(new ChatComponentText("§c[AutoBridge] 비활성화됨"));
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!enabled || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        if (!ensureHoldingBlock()) return;

        switch (currentMode) {
            case SCAFFOLD:
                runScaffold();
                break;
            case TELLY:
                runTelly(false);
                break;
            case SPEED_TELLY:
                runTelly(true);
                break;
            case DAO_TELLY:
                runDaoTelly();
                break;
        }
    }

    private boolean ensureHoldingBlock() {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held != null && held.getItem() instanceof ItemBlock) return true;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                mc.thePlayer.inventory.currentItem = i;
                return true;
            }
        }
        return false;
    }

    private void runScaffold() {
        BlockPos below = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0D, mc.thePlayer.posZ);
        if (mc.theWorld.getBlockState(below).getBlock() instanceof BlockAir) {
            mc.thePlayer.rotationPitch = 82.5f;
            placeBlockUnder(below);
        }
    }

    private void runTelly(boolean isSpeed) {
        BlockPos below = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0D, mc.thePlayer.posZ);
        boolean overAir = mc.theWorld.getBlockState(below).getBlock() instanceof BlockAir;

        if (mc.thePlayer.onGround) {
            airTicks = 0;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);

            if (overAir) {
                mc.thePlayer.jump();
            }
        } else {
            airTicks++;
            int switchTick = isSpeed ? 1 : 2;
            int returnTick = isSpeed ? 5 : 6;

            if (airTicks >= switchTick && airTicks <= returnTick) {
                mc.thePlayer.rotationPitch = isSpeed ? 78.5f : 79.8f;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
            } else if (airTicks > returnTick) {
                mc.thePlayer.rotationPitch = 75.0f;
            }
        }
    }

    private void runDaoTelly() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
        if (rightClickDelayField != null) {
            try {
                rightClickDelayField.setInt(mc, 0);
            } catch (IllegalAccessException ignored) {}
        }

        BlockPos below = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0D, mc.thePlayer.posZ);
        boolean overAir = mc.theWorld.getBlockState(below).getBlock() instanceof BlockAir;

        if (mc.thePlayer.onGround) {
            airTicks = 0;
            mc.thePlayer.rotationPitch = 77.2f;
            if (overAir) {
                mc.thePlayer.jump();
            }
        } else {
            airTicks++;
            if (airTicks >= 1 && airTicks <= 5) {
                mc.thePlayer.rotationPitch = 79.2f;
            } else {
                mc.thePlayer.rotationPitch = 76.5f;
            }
        }
    }

    private void placeBlockUnder(BlockPos pos) {
        for (EnumFacing facing : EnumFacing.values()) {
            if (facing == EnumFacing.UP) continue;
            BlockPos neighbor = pos.offset(facing);
            if (!(mc.theWorld.getBlockState(neighbor).getBlock() instanceof BlockAir)) {
                Vec3 hitVec = new Vec3(neighbor.getX() + 0.5, neighbor.getY() + 0.5, neighbor.getZ() + 0.5);
                if (mc.playerController.onPlayerRightClick(
                        mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(),
                        neighbor, facing.getOpposite(), hitVec)) {
                    mc.thePlayer.swingItem();
                    break;
                }
            }
        }
    }

    public static class BridgeGuiScreen extends GuiScreen {
        @Override
        public void initGui() {
            this.buttonList.clear();
            int centerX = this.width / 2 - 100;
            int startY = this.height / 2 - 70;

            this.buttonList.add(new GuiButton(0, centerX, startY, 200, 20, getToggleText()));
            this.buttonList.add(new GuiButton(1, centerX, startY + 30, 200, 20, getModeText(BridgeMode.SCAFFOLD)));
            this.buttonList.add(new GuiButton(2, centerX, startY + 55, 200, 20, getModeText(BridgeMode.SPEED_TELLY)));
            this.buttonList.add(new GuiButton(3, centerX, startY + 80, 200, 20, getModeText(BridgeMode.TELLY)));
            this.buttonList.add(new GuiButton(4, centerX, startY + 105, 200, 20, getModeText(BridgeMode.DAO_TELLY)));
        }

        private String getToggleText() {
            return "오토 브릿지 상태: " + (enabled ? "§a켜짐 (ON)" : "§c꺼짐 (OFF)");
        }

        private String getModeText(BridgeMode mode) {
            return (currentMode == mode ? "§b▶ " : "§7") + mode.displayName;
        }

        @Override
        protected void actionPerformed(GuiButton button) throws IOException {
            if (button.id == 0) toggleMod();
            else if (button.id == 1) currentMode = BridgeMode.SCAFFOLD;
            else if (button.id == 2) currentMode = BridgeMode.SPEED_TELLY;
            else if (button.id == 3) currentMode = BridgeMode.TELLY;
            else if (button.id == 4) currentMode = BridgeMode.DAO_TELLY;
            this.initGui();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRendererObj, "§l§6[ 1.8.9 AutoBridge 설정 메뉴 ]", this.width / 2, this.height / 2 - 95, 0xFFFFFF);
            this.drawCenteredString(this.fontRendererObj, "§7단축키: [R-SHIFT] 메뉴 열기  /  [V] 즉시 켜기·끄기", this.width / 2, this.height / 2 + 65, 0xAAAAAA);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}
