package org.ja13.eau.node.transparent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.ja13.eau.cable.CableRender;
import org.ja13.eau.cable.CableRenderDescriptor;
import org.ja13.eau.cable.CableRenderType;
import org.ja13.eau.client.ClientProxy;
import org.ja13.eau.misc.*;
import org.ja13.eau.sound.LoopedSound;
import org.ja13.eau.sound.LoopedSoundManager;
import org.ja13.eau.sound.SoundCommand;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import org.ja13.eau.misc.Coordonate;
import org.ja13.eau.misc.Direction;
import org.ja13.eau.misc.LRDU;
import org.ja13.eau.misc.LRDUMask;
import org.ja13.eau.misc.Utils;
import org.ja13.eau.misc.UtilsClient;
import org.lwjgl.opengl.GL11;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class TransparentNodeElementRender {
    public TransparentNodeEntity tileEntity;
    public Direction front;
    public boolean grounded;
    public TransparentNodeDescriptor transparentNodedescriptor;

    public TransparentNodeElementRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        this.tileEntity = tileEntity;
        this.transparentNodedescriptor = descriptor;
    }

    protected EntityItem unserializeItemStackToEntityItem(DataInputStream stream, EntityItem old) throws IOException {
        return Utils.unserializeItemStackToEntityItem(stream, old, tileEntity);

    }

    public void drawEntityItem(EntityItem entityItem, double x, double y, double z, float roty, float scale) {/*
        if(entityItem == null) return;
		


		entityItem.hoverStart = 0.0f;
		entityItem.rotationYaw = 0.0f;
		entityItem.motionX = 0.0;
		entityItem.motionY = 0.0;
		entityItem.motionZ =0.0;
		
		Render var10 = null;
		var10 = RenderManager.instance.getEntityRenderObject(entityItem);
		GL11.glPushMatrix();
			GL11.glTranslatef((float)x, (float)y, (float)z);
			GL11.glRotatef(roty, 0, 1, 0);
			GL11.glScalef(scale, scale, scale);
			var10.doRender(entityItem,0, 0, 0, 0, 0);	
		GL11.glPopMatrix();	
		*/
        UtilsClient.drawEntityItem(entityItem, x, y, z, roty, scale);

    }

    public void glCableTransforme(Direction inverse) {
        inverse.glTranslate(0.5f);
        inverse.glRotateXnRef();
    }

    public abstract void draw();

    public void networkUnserialize(DataInputStream stream) {
        try {
            byte b = stream.readByte();
            front = Direction.fromInt(b & 0x7);
            grounded = (b & 8) != 0;
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return null;
    }

    public IInventory getInventory() {
        return null;
    }

    public void preparePacketForServer(DataOutputStream stream) {
        tileEntity.preparePacketForServer(stream);
    }

    public void sendPacketToServer(ByteArrayOutputStream bos) {
        tileEntity.sendPacketToServer(bos);
    }


    public void clientSetGrounded(boolean value) {
        clientSendBoolean(TransparentNodeElement.unserializeGroundedId, value);
    }

    public void clientSendBoolean(Byte id, boolean value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeByte(value ? 1 : 0);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSendId(Byte id) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSendString(Byte id, String str) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeUTF(str);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSendFloat(Byte id, float str) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeFloat(str);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSendInt(Byte id, int str) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeInt(str);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public boolean cameraDrawOptimisation() {
        return true;
    }

    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {

        return null;
    }

    static final LRDUMask maskTempDraw = new LRDUMask();

    public CableRenderType drawCable(Direction side, CableRenderDescriptor render, LRDUMask connection, CableRenderType renderPreProcess) {
        if (render == null) return renderPreProcess;
        if (renderPreProcess == null) renderPreProcess = CableRender.connectionType(tileEntity, connection, side);
        GL11.glPushMatrix();
        glCableTransforme(side);
        render.bindCableTexture();

        for (LRDU lrdu : LRDU.values()) {
            Utils.setGlColorFromDye(renderPreProcess.otherdry[lrdu.toInt()]);
            if (connection.get(lrdu) == false) continue;
            maskTempDraw.set(1 << lrdu.toInt());
            CableRender.drawCable(render, maskTempDraw, renderPreProcess);
        }
        GL11.glPopMatrix();
        GL11.glColor3f(1f, 1f, 1f);
        return renderPreProcess;
    }

    public void notifyNeighborSpawn() {


    }

    public void serverPacketUnserialize(DataInputStream stream) {


    }

    protected Coordonate coordonate() {

        return new Coordonate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity.getWorldObj());
    }


    private int uuid = 0;

    public int getUuid() {
        if (uuid == 0) {
            uuid = UtilsClient.getUuid();
        }
        return uuid;
    }

    public boolean usedUuid() {
        return uuid != 0;
    }


    public void play(SoundCommand s) {
        s.addUuid(getUuid());
        s.set(tileEntity);
        s.play();
    }

    private final LoopedSoundManager loopedSoundManager = new LoopedSoundManager();

    @SideOnly(Side.CLIENT)
    protected void addLoopedSound(final LoopedSound loopedSound) {
        loopedSoundManager.add(loopedSound);
    }

    public void destructor() {
        if (usedUuid())
            ClientProxy.uuidManager.kill(uuid);

        loopedSoundManager.dispose();
    }

    public void refresh(double deltaT) {
        loopedSoundManager.process(deltaT);
    }
}
