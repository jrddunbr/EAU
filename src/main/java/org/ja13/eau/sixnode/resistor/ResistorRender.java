package org.ja13.eau.sixnode.resistor;

import org.ja13.eau.cable.CableRenderType;
import org.ja13.eau.misc.Direction;
import org.ja13.eau.node.six.SixNodeDescriptor;
import org.ja13.eau.node.six.SixNodeElementInventory;
import org.ja13.eau.node.six.SixNodeElementRender;
import org.ja13.eau.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.ja13.eau.cable.CableRenderType;
import org.ja13.eau.misc.Direction;
import org.ja13.eau.node.six.SixNodeDescriptor;
import org.ja13.eau.node.six.SixNodeElementInventory;
import org.ja13.eau.node.six.SixNodeElementRender;
import org.ja13.eau.node.six.SixNodeEntity;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class ResistorRender extends SixNodeElementRender {

    public ResistorDescriptor descriptor;
    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);
    private CableRenderType renderPreProcess;

    private float wiperPos = 0;

    public ResistorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ResistorDescriptor) descriptor;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            if (descriptor.isRheostat) wiperPos = stream.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void draw() {
        GL11.glRotatef(90, 1, 0, 0);
        front.glRotateOnX();
        descriptor.draw(wiperPos);
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new ResistorGui(player, inventory, this);
    }
}
