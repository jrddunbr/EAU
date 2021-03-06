package org.ja13.eau.transparentnode.electricalantennarx;

import org.ja13.eau.sim.IProcess;
import org.ja13.eau.sim.IProcess;

public class ElectricalAntennaRxSlowProcess implements IProcess {

    ElectricalAntennaRxElement element;

    public ElectricalAntennaRxSlowProcess(ElectricalAntennaRxElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        if (element.powerSrc.getP() > element.descriptor.electricalMaximalPower) {
            element.node.physicalSelfDestruction(2.0f);
        } else if (element.powerOut.getU() > element.descriptor.electricalMaximalVoltage) {
            element.node.physicalSelfDestruction(2.0f);
        }
    }
}
