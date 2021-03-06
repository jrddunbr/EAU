package org.ja13.eau.node;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.ja13.eau.EAU;
import org.ja13.eau.GuiHandler;
import org.ja13.eau.ghost.GhostBlock;
import org.ja13.eau.misc.Coordonate;
import org.ja13.eau.misc.Direction;
import org.ja13.eau.misc.LRDU;
import org.ja13.eau.misc.LRDUCubeMask;
import org.ja13.eau.misc.Utils;
import org.ja13.eau.node.six.SixNode;
import org.ja13.eau.sim.ElectricalConnection;
import org.ja13.eau.sim.ElectricalLoad;
import org.ja13.eau.sim.ThermalConnection;
import org.ja13.eau.sim.ThermalLoad;
import org.ja13.eau.sound.SoundCommand;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class NodeBase {
    // MASK_NONE is 0, do not use it.
    public static final int MASK_ELECTRIC = 1;
    public static final int MASK_THERMAL = 2;

    public static final int maskElectricalGate = MASK_ELECTRIC;
    public static final int maskElectricalAll = MASK_ELECTRIC;

    public static final int maskColorData = 0xF << 16;
    public static final int maskColorShift = 16;
    public static final int maskColorCareShift = 20;
    public static final int maskColorCareData = 1 << 20;

    public byte neighborOpaque;
    public byte neighborWrapable;

    public static int teststatic;

    public Coordonate coordonate;

    public ArrayList<NodeConnection> nodeConnectionList = new ArrayList<>(4);

    private boolean isAdded = false;

    private boolean needPublish = false;

    public boolean mustBeSaved() {
        return true;
    }

    public int getBlockMetadata() {
        return 0;
    }

    public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {}

    public void notifyNeighbor() {
        coordonate.world().notifyBlockChange(coordonate.x, coordonate.y, coordonate.z, coordonate.getBlock());
    }

    public abstract String getNodeUuid();

    public LRDUCubeMask lrduCubeMask = new LRDUCubeMask();

    public void neighborBlockRead() {
        int[] vector = new int[3];
        World world = coordonate.world();
        neighborOpaque = 0;
        neighborWrapable = 0;
        for (Direction direction : Direction.values()) {
            vector[0] = coordonate.x;
            vector[1] = coordonate.y;
            vector[2] = coordonate.z;
            direction.applyTo(vector, 1);
            Block b = world.getBlock(vector[0], vector[1], vector[2]);
            neighborOpaque |= 1 << direction.getInt();
            if (isBlockWrappable(b, world, coordonate.x, coordonate.y, coordonate.z))
                neighborWrapable |= 1 << direction.getInt();
        }
    }

    public boolean hasGui(Direction side) {
        return false;
    }

    public void onNeighborBlockChange() {
        neighborBlockRead();
        if (isAdded) {
            reconnect();
        }
    }

    public boolean isBlockWrappable(Direction direction) {
        return ((neighborWrapable >> direction.getInt()) & 1) != 0;
    }

    public boolean isBlockOpaque(Direction direction) {
        return ((neighborOpaque >> direction.getInt()) & 1) != 0;
    }

    public static boolean isBlockWrappable(Block block, World w, int x, int y, int z) {
        if (block.isReplaceable(w, x, y, z)) return true;
        if (block == Blocks.air) return true;
        if (block == EAU.sixNodeBlock) return true;
        if (block instanceof GhostBlock) return true;
        if (block == Blocks.torch) return true;
        if (block == Blocks.redstone_torch) return true;
        if (block == Blocks.unlit_redstone_torch) return true;
        return block == Blocks.redstone_wire;
    }

    public NodeBase() {
        coordonate = new Coordonate();
    }

    boolean destructed = false;

    public boolean isDestructing() {
        return destructed;
    }

    public void physicalSelfDestruction(float explosionStrength) {
        if (destructed) return;
        destructed = true;
        if (!EAU.explosionEnable) explosionStrength = 0;
        disconnect();
        coordonate.world().setBlockToAir(coordonate.x, coordonate.y, coordonate.z);
        NodeManager.instance.removeNode(this);
        if (explosionStrength != 0) {
            coordonate.world().createExplosion(null, coordonate.x, coordonate.y, coordonate.z, explosionStrength, true);
        }
    }

    public void onBlockPlacedBy(Coordonate coordonate, Direction front, EntityLivingBase entityLiving, ItemStack itemStack) {
        this.coordonate = coordonate;
        neighborBlockRead();
        NodeManager.instance.addNode(this);
        initializeFromThat(front, entityLiving, itemStack);
        if (itemStack != null)
            Utils.println("Node::constructor( meta = " + itemStack.getItemDamage() + ")");
    }

    abstract public void initializeFromThat(Direction front,
                                            EntityLivingBase entityLiving, ItemStack itemStack);

    public NodeBase getNeighbor(Direction direction) {
        int[] position = new int[3];
        position[0] = coordonate.x;
        position[1] = coordonate.y;
        position[2] = coordonate.z;
        direction.applyTo(position, 1);
        Coordonate nodeCoordonate = new Coordonate(position[0], position[1], position[2], coordonate.dimention);
        return NodeManager.instance.getNodeFromCoordonate(nodeCoordonate);
    }

    // leaf
    public void onBreakBlock() {
        destructed = true;
        disconnect();
        NodeManager.instance.removeNode(this);
        Utils.println("Node::onBreakBlock()");
    }

    public static SoundCommand beepUploaded = new SoundCommand("eau:beep_accept_2").smallRange();
    public static SoundCommand beepDownloaded = new SoundCommand("eau:beep_accept").smallRange();
    public static SoundCommand beepError = new SoundCommand("eau:beep_error").smallRange();

    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (!entityPlayer.worldObj.isRemote && entityPlayer.getCurrentEquippedItem() != null) {
            ItemStack equipped = entityPlayer.getCurrentEquippedItem();
            if (EAU.multiMeterElement.checkSameItemStack(equipped)) {
                String str = multiMeterString(side);
                if (str != null)
                    Utils.addChatMessage(entityPlayer, str);
                return true;
            }
            if (EAU.thermometerElement.checkSameItemStack(equipped)) {
                String str = thermoMeterString(side);
                if (str != null)
                    Utils.addChatMessage(entityPlayer, str);
                return true;
            }
            if (EAU.allMeterElement.checkSameItemStack(equipped)) {
                String str1 = multiMeterString(side);
                String str2 = thermoMeterString(side);
                String str = "";
                if (str1 != null)
                    str += str1;
                if (str2 != null)
                    str += str2;
                if (!str.equals(""))
                    Utils.addChatMessage(entityPlayer, str);
                return true;
            }
            if (EAU.configCopyToolElement.checkSameItemStack(equipped)) {
                if(!equipped.hasTagCompound()) {
                    equipped.setTagCompound(new NBTTagCompound());
                }
                String act;
                SoundCommand snd = beepError;
                if(entityPlayer.isSneaking() || EAU.playerManager.get(entityPlayer).getInteractEnable()) {
                    if(writeConfigTool(side, equipped.getTagCompound(), entityPlayer))
                        snd = beepDownloaded;
                    act = "write";
                } else {
                    if(readConfigTool(side, equipped.getTagCompound(), entityPlayer))
                        snd = beepUploaded;
                    act = "read";
                }
                snd.set(
                    entityPlayer.posX,
                    entityPlayer.posY,
                    entityPlayer.posZ,
                    entityPlayer.worldObj
                ).play();
                Utils.println(String.format("NB.oBA: act %s data %s", act, equipped.getTagCompound().toString()));
                return true;
            }
        }
        if (hasGui(side)) {
            entityPlayer.openGui(EAU.instance, GuiHandler.nodeBaseOpen + side.getInt(), coordonate.world(), coordonate.x, coordonate.y, coordonate.z);
            return true;
        }

        return false;
    }

    public void reconnect() {
        disconnect();
        connect();
    }

    public static void tryConnectTwoNode(NodeBase nodeA, Direction directionA, LRDU lrduA, NodeBase nodeB, Direction directionB, LRDU lrduB) {
        int mskA = nodeA.getSideConnectionMask(directionA, lrduA);
        int mskB = nodeB.getSideConnectionMask(directionB, lrduB);
        if (compareConnectionMask(mskA, mskB)) {
            ElectricalConnection eCon;
            ThermalConnection tCon;

            NodeConnection nodeConnection = new NodeConnection(nodeA, directionA, lrduA, nodeB, directionB, lrduB);

            nodeA.nodeConnectionList.add(nodeConnection);
            nodeB.nodeConnectionList.add(nodeConnection);

            nodeA.setNeedPublish(true);
            nodeB.setNeedPublish(true);

            nodeA.lrduCubeMask.set(directionA, lrduA, true);
            nodeB.lrduCubeMask.set(directionB, lrduB, true);

            nodeA.newConnectionAt(nodeConnection, true);
            nodeB.newConnectionAt(nodeConnection, false);

            ElectricalLoad eLoad;
            if ((eLoad = nodeA.getElectricalLoad(directionA, lrduA, mskB)) != null) {

                ElectricalLoad otherELoad = nodeB.getElectricalLoad(directionB, lrduB, mskA);
                if (otherELoad != null) {
                    eCon = new ElectricalConnection(eLoad, otherELoad);

                    EAU.simulator.addElectricalComponent(eCon);
                    nodeConnection.addConnection(eCon);
                }
            }
            ThermalLoad tLoad;
            if ((tLoad = nodeA.getThermalLoad(directionA, lrduA, mskB)) != null) {

                ThermalLoad otherTLoad = nodeB.getThermalLoad(directionB, lrduB, mskA);
                if (otherTLoad != null) {
                    tCon = new ThermalConnection(tLoad, otherTLoad);

                    EAU.simulator.addThermalConnection(tCon);
                    nodeConnection.addConnection(tCon);
                }

            }
        }
    }

    public abstract int getSideConnectionMask(Direction directionA, LRDU lrduA);

    public abstract ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA, int mask);

    public abstract ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB, int mask);

    public void checkCanStay(boolean onCreate) {

    }

    public void connectJob() {
        // EXTERNAL OTHERS SIXNODE
        {
            int[] emptyBlockCoord = new int[3];
            int[] otherBlockCoord = new int[3];
            for (Direction direction : Direction.values()) {
                if (isBlockWrappable(direction)) {
                    emptyBlockCoord[0] = coordonate.x;
                    emptyBlockCoord[1] = coordonate.y;
                    emptyBlockCoord[2] = coordonate.z;
                    direction.applyTo(emptyBlockCoord, 1);
                    for (LRDU lrdu : LRDU.values()) {
                        Direction elementSide = direction.applyLRDU(lrdu);
                        otherBlockCoord[0] = emptyBlockCoord[0];
                        otherBlockCoord[1] = emptyBlockCoord[1];
                        otherBlockCoord[2] = emptyBlockCoord[2];
                        elementSide.applyTo(otherBlockCoord, 1);
                        NodeBase otherNode = NodeManager.instance.getNodeFromCoordonate(new Coordonate(otherBlockCoord[0], otherBlockCoord[1], otherBlockCoord[2], coordonate.dimention));
                        if (otherNode == null) continue;
                        Direction otherDirection = elementSide.getInverse();
                        LRDU otherLRDU = otherDirection.getLRDUGoingTo(direction).inverse();
                        if (this instanceof SixNode || otherNode instanceof SixNode) {
                            tryConnectTwoNode(this, direction, lrdu, otherNode, otherDirection, otherLRDU);
                        }
                    }
                }
            }
        }

        {
            for (Direction dir : Direction.values()) {
                NodeBase otherNode = getNeighbor(dir);
                if (otherNode != null && otherNode.isAdded) {
                    for (LRDU lrdu : LRDU.values()) {
                        tryConnectTwoNode(this, dir, lrdu, otherNode, dir.getInverse(), lrdu.inverseIfLR());
                    }
                }

            }
        }

    }

    public void disconnectJob() {

        for (NodeConnection c : nodeConnectionList) {

            if (c.N1 != this) {
                c.N1.nodeConnectionList.remove(c);
                c.N1.setNeedPublish(true);
                c.N1.lrduCubeMask.set(c.dir1, c.lrdu1, false);
            }
            if(c.N2 != this) {
                c.N2.nodeConnectionList.remove(c);
                c.N2.setNeedPublish(true);
                c.N2.lrduCubeMask.set(c.dir2, c.lrdu2, false);
            }
            c.destroy();
        }

        lrduCubeMask.clear();

        nodeConnectionList.clear();
    }

    public static boolean compareConnectionMask(int mask1, int mask2) {
        if (((mask1 & 0xFFFF) & (mask2 & 0xFFFF)) == 0) return false;
        if (((mask1 & maskColorCareData) & (mask2 & maskColorCareData)) == 0) return true;
        return (mask1 & maskColorData) == (mask2 & maskColorData);
    }

    public void externalDisconnect(Direction side, LRDU lrdu) {
    }

    public void newConnectionAt(NodeConnection connection, boolean isA) {
    }

    public void connectInit() {
        lrduCubeMask.clear();
        nodeConnectionList.clear();
    }

    public void connect() {
        if (isAdded) {
            disconnect();
        }
        connectInit();
        connectJob();
        isAdded = true;
        setNeedPublish(true);
    }

    public void disconnect() {
        if (!isAdded) {
            Utils.println("Node destroy error already destroy");
            return;
        }
        disconnectJob();
        isAdded = false;
    }



    public void readFromNBT(NBTTagCompound nbt) {
        coordonate.readFromNBT(nbt, "c");
        neighborOpaque = nbt.getByte("NBOpaque");
        neighborWrapable = nbt.getByte("NBWrap");
    }

    public void writeToNBT(NBTTagCompound nbt) {
        coordonate.writeToNBT(nbt, "c");
        nbt.setByte("NBOpaque", neighborOpaque);
        nbt.setByte("NBWrap", neighborWrapable);
    }

    public String multiMeterString(Direction side) {
        return "";
    }

    public String thermoMeterString(Direction side) {
        return "";
    }

    public boolean readConfigTool(Direction side, NBTTagCompound tag, EntityPlayer invoker) { return false; }

    public boolean writeConfigTool(Direction side, NBTTagCompound tag, EntityPlayer invoker) { return false; }

    public void setNeedPublish(boolean needPublish) {
        this.needPublish = needPublish;
    }

    public boolean getNeedPublish() {
        return needPublish;
    }

    boolean needNotify = false;

    public void publishSerialize(DataOutputStream stream) {

    }

    public void preparePacketForClient(DataOutputStream stream) {
        try {
            stream.writeByte(EAU.packetForClientNode);
            stream.writeInt(coordonate.x);
            stream.writeInt(coordonate.y);
            stream.writeInt(coordonate.z);
            stream.writeByte(coordonate.dimention);
            stream.writeUTF(getNodeUuid());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPacketToClient(ByteArrayOutputStream bos, EntityPlayerMP player) {
        Utils.sendPacketToClient(bos, player);
    }

    public void sendPacketToAllClient(ByteArrayOutputStream bos) {
        sendPacketToAllClient(bos, 100000);
    }

    public void sendPacketToAllClient(ByteArrayOutputStream bos, double range) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        for (Object obj : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) obj;
            WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(player.dimension);
            PlayerManager playerManager = worldServer.getPlayerManager();
            if (player.dimension != this.coordonate.dimention) continue;
            if (!playerManager.isPlayerWatchingChunk(player, coordonate.x / 16, coordonate.z / 16)) continue;
            if (coordonate.distanceTo(player) > range) continue;
            Utils.sendPacketToClient(bos, player);
        }
    }

    public ByteArrayOutputStream getPublishPacket() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);
        try {
            stream.writeByte(EAU.packetNodeSingleSerialized);
            stream.writeInt(coordonate.x);
            stream.writeInt(coordonate.y);
            stream.writeInt(coordonate.z);
            stream.writeByte(coordonate.dimention);
            stream.writeUTF(getNodeUuid());
            publishSerialize(stream);
            return bos;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void publishToAllPlayer() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        for (Object obj : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) obj;
            WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(player.dimension);
            PlayerManager playerManager = worldServer.getPlayerManager();
            if (player.dimension != this.coordonate.dimention) continue;
            if (!playerManager.isPlayerWatchingChunk(player, coordonate.x / 16, coordonate.z / 16)) continue;
            Utils.sendPacketToClient(getPublishPacket(), player);
        }
        if (needNotify) {
            needNotify = false;
            notifyNeighbor();
        }
        needPublish = false;
    }

    public void publishToPlayer(EntityPlayerMP player) {
        Utils.sendPacketToClient(getPublishPacket(), player);
    }

    public void dropItem(ItemStack itemStack) {
        if (itemStack == null) return;
        if (coordonate.world().getGameRules().getGameRuleBooleanValue("doTileDrops")) {
            float var6 = 0.7F;
            double var7 = (double) (coordonate.world().rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            double var9 = (double) (coordonate.world().rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            double var11 = (double) (coordonate.world().rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            EntityItem var13 = new EntityItem(coordonate.world(), (double) coordonate.x + var7, (double) coordonate.y + var9, (double) coordonate.z + var11, itemStack);
            var13.delayBeforeCanPickup = 10;
            coordonate.world().spawnEntityInWorld(var13);
        }
    }

    public void dropInventory(IInventory inventory) {
        if (inventory == null) return;
        for (int idx = 0; idx < inventory.getSizeInventory(); idx++) {
            dropItem(inventory.getStackInSlot(idx));
        }
    }

    public abstract void initializeFromNBT();

    public void globalBoot() {}

    public void needPublish() {
        setNeedPublish(true);
    }

    public void unload() {
        disconnect();
    }
}
