package org.ja13.eau.sixnode.electricaldatalogger;

import org.ja13.eau.EAU;
import org.ja13.eau.sim.IProcess;
import net.minecraft.item.ItemStack;
import org.ja13.eau.EAU;
import org.ja13.eau.sim.IProcess;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ElectricalDataLoggerProcess implements IProcess {

    ElectricalDataLoggerElement e;

    public ElectricalDataLoggerProcess(ElectricalDataLoggerElement e) {
        this.e = e;
    }

    @Override
    public void process(double time) {
        //Profiler p = new Profiler();
        //p.add("A");
        if (!e.pause) {
            e.timeToNextSample -= time;
            byte value = (byte) (e.inputGate.getNormalized() * 255.5 - 128);
            e.sampleStack += value;
            e.sampleStackNbr++;
        }
        //p.add("B");
        if (e.printToDo) {
            ItemStack paperStack = e.inventory.getStackInSlot(ElectricalDataLoggerContainer.paperSlotId);
            ItemStack printStack = e.inventory.getStackInSlot(ElectricalDataLoggerContainer.printSlotId);
            if (paperStack != null && printStack == null) {
                e.inventory.decrStackSize(ElectricalDataLoggerContainer.paperSlotId, 1);
                ItemStack print = EAU.dataLogsPrintDescriptor.newItemStack(1);
                EAU.dataLogsPrintDescriptor.initializeStack(print, e.logs);
                e.inventory.setInventorySlotContents(ElectricalDataLoggerContainer.printSlotId, print);
            }
            e.printToDo = false;
        }
        //p.add("C");
        if (e.timeToNextSample <= 0.0) {
            e.timeToNextSample += e.logs.samplingPeriod;
            byte value = (byte) (e.sampleStack / e.sampleStackNbr);
            e.sampleStackReset();
            e.logs.write(value);

            ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
            DataOutputStream packet = new DataOutputStream(bos);

            e.preparePacketForClient(packet);

            try {
                packet.writeByte(ElectricalDataLoggerElement.toClientLogsAdd);
                packet.write(value);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            //p.add("D");
            e.sendPacketToAllClient(bos);
        }
        //p.stop();
        //Utils.println(p);
    }
}
