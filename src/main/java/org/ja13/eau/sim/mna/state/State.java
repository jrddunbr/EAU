package org.ja13.eau.sim.mna.state;

import org.ja13.eau.sim.mna.RootSystem;
import org.ja13.eau.sim.mna.SubSystem;
import org.ja13.eau.sim.mna.component.Component;
import org.ja13.eau.sim.mna.component.IAbstractor;
import org.ja13.eau.sim.mna.RootSystem;
import org.ja13.eau.sim.mna.SubSystem;

import java.util.ArrayList;

public class State {

    private int id = -1;

    public double state;
    SubSystem subSystem;

    ArrayList<Component> components = new ArrayList<Component>();

    boolean isPrivateSubSystem = false;
    boolean mustBeFarFromInterSystem = false;

    public IAbstractor abstractedBy;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addedTo(SubSystem s) {
        this.subSystem = s;
    }

    public SubSystem getSubSystem() {
        if (isAbstracted()) return abstractedBy.getAbstractorSubSystem();
        return subSystem;
    }

    public void quitSubSystem() {
        subSystem = null;
    }

    public ArrayList<Component> getConnectedComponents() {
        return components;
    }

    public ArrayList<Component> getConnectedComponentsNotAbstracted() {
        ArrayList<Component> list = new ArrayList<Component>();
        for (Component c : components) {
            if (c.isAbstracted()) continue;
            list.add(c);
        }
        return list;
    }

    public void add(Component c) {
        components.add(c);
        //System.out.println("ADD " + c + " To " +  this);
    }

    public void remove(Component c) {
        components.remove(c);
    }

    public boolean canBeSimplifiedByLine() {
        return false;
    }

    public State setAsPrivate() {
        isPrivateSubSystem = true;
        return this;
    }

    public State setAsMustBeFarFromInterSystem() {
        mustBeFarFromInterSystem = true;
        return this;
    }

    public boolean mustBeFarFromInterSystem() {
        return mustBeFarFromInterSystem;
    }

    public boolean isPrivateSubSystem() {
        return isPrivateSubSystem;
    }

    public void returnToRootSystem(RootSystem root) {
        root.addStates.add(this);
    }

    public boolean isAbstracted() {
        return abstractedBy != null;
    }

    public boolean isNotSimulated() {
        return subSystem == null && abstractedBy == null;
    }

    @Override
    public String toString() {
        return "(" + this.getId() + "," + this.getClass().getSimpleName() + ")";
    }
}
