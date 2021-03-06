package org.ja13.eau.sixnode.electricalgatesource;

import org.ja13.eau.EAU;
import org.ja13.eau.cable.CableRenderDescriptor;
import org.ja13.eau.misc.Direction;
import org.ja13.eau.misc.LRDU;
import org.ja13.eau.misc.RcInterpolator;
import org.ja13.eau.misc.UtilsClient;
import org.ja13.eau.misc.VoltageTier;
import org.ja13.eau.node.six.SixNodeDescriptor;
import org.ja13.eau.node.six.SixNodeElementRender;
import org.ja13.eau.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.ja13.eau.EAU;
import org.ja13.eau.cable.CableRenderDescriptor;
import org.ja13.eau.misc.Direction;
import org.ja13.eau.misc.LRDU;
import org.ja13.eau.misc.RcInterpolator;
import org.ja13.eau.misc.UtilsClient;
import org.ja13.eau.misc.VoltageTier;
import org.ja13.eau.node.six.SixNodeDescriptor;
import org.ja13.eau.node.six.SixNodeElementRender;
import org.ja13.eau.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalGateSourceRender extends SixNodeElementRender {

    ElectricalGateSourceDescriptor descriptor;

    LRDU front;

    RcInterpolator interpolator;

    float voltageSyncValue = 0;
    boolean voltageSyncNew = false;
    boolean boot = true;

    public ElectricalGateSourceRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalGateSourceDescriptor) descriptor;
        interpolator = new RcInterpolator(this.descriptor.render.speed);
    }

    @Override
    public void draw() {
        super.draw();
        drawSignalPin(front, new float[]{3, 3, 3, 3});

        if (side.isY()) {
            front.glRotateOnX();
        } else {
            LRDU.Down.glRotateOnX();
        }
        descriptor.draw((float)interpolator.get(), UtilsClient.distanceFromClientPlayer(this.tileEntity), tileEntity);
    }

    @Override
    public void refresh(float deltaT) {
        interpolator.setTarget((float) (voltageSyncValue / VoltageTier.TTL.getVoltage()));
        interpolator.step(deltaT);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            Byte b;
            b = stream.readByte();
            front = LRDU.fromInt((b >> 4) & 3);
            float readF;
            readF = stream.readFloat();
            if (voltageSyncValue != readF) {
                voltageSyncValue = readF;
                voltageSyncNew = true;
            }

            if (boot) {
                boot = false;
                interpolator.setValue((float) (voltageSyncValue / VoltageTier.TTL.getVoltage()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return EAU.smallInsulationLowCurrentRender;
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new ElectricalGateSourceGui(player, this);
    }
}
