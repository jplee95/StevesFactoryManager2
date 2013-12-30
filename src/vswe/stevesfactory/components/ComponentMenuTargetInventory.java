package vswe.stevesfactory.components;


import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.nbt.NBTTagCompound;
import vswe.stevesfactory.interfaces.ContainerManager;
import vswe.stevesfactory.interfaces.GuiManager;
import vswe.stevesfactory.network.DataBitHelper;
import vswe.stevesfactory.network.DataReader;
import vswe.stevesfactory.network.DataWriter;

import java.util.List;

public class ComponentMenuTargetInventory extends ComponentMenuTarget {
    public ComponentMenuTargetInventory(FlowComponent parent) {
        super(parent);

        textBoxes = new TextBoxNumberList();
        textBoxes.addTextBox(startTextBox = new TextBoxNumber(39 ,49, 2, false) {
            @Override
            public void onNumberChanged() {
                if (selectedDirectionId != -1 && getParent().getManager().worldObj.isRemote) {
                    writeData(DataTypeHeader.START_OR_TANK_DATA, getNumber());
                }
            }
        });
        textBoxes.addTextBox(endTextBox = new TextBoxNumber(60 ,49, 2, false) {
            @Override
            public void onNumberChanged() {
                if (selectedDirectionId != -1 && getParent().getManager().worldObj.isRemote) {
                    writeData(DataTypeHeader.END, getNumber());
                }
            }
        });
    }

    private TextBoxNumberList textBoxes;
    private TextBoxNumber startTextBox;
    private TextBoxNumber endTextBox;

    private int[] startRange = new int[directions.length];
    private int[] endRange = new int[directions.length];

    @Override
    protected Button getSecondButton() {
        return new Button(27) {
            @Override
            protected String getLabel() {
                return useAdvancedSetting(selectedDirectionId) ? "Use all slots" : "Use id range";
            }

            @Override
            protected String getMouseOverText() {
                return useAdvancedSetting(selectedDirectionId) ? "Click to use all slots for this side instead" : "Click to use a slot id range for this specific side";
            }

            @Override
            protected void onClicked() {
                writeData(DataTypeHeader.USE_ADVANCED_SETTING, useAdvancedSetting(selectedDirectionId) ? 0 : 1);
            }
        };
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected void drawAdvancedComponent(GuiManager gui, int mX, int mY) {
        textBoxes.draw(gui, mX, mY);
    }

    @Override
    protected void refreshAdvancedComponent() {
        if (selectedDirectionId != -1) {
            startTextBox.setNumber(startRange[selectedDirectionId]);
            endTextBox.setNumber(endRange[selectedDirectionId]);
        }
    }

    @Override
    protected void writeAdvancedSetting(DataWriter dw, int i) {
        dw.writeData(startRange[i], DataBitHelper.MENU_TARGET_RANGE);
        dw.writeData(endRange[i], DataBitHelper.MENU_TARGET_RANGE);
    }

    @Override
    protected void readAdvancedSetting(DataReader dr, int i) {
        startRange[i] = dr.readData(DataBitHelper.MENU_TARGET_RANGE);
        endRange[i] = dr.readData(DataBitHelper.MENU_TARGET_RANGE);
    }

    @Override
    protected void copyAdvancedSetting(ComponentMenu menu, int i) {
        ComponentMenuTargetInventory menuTarget = (ComponentMenuTargetInventory)menu;
        startRange[i] = menuTarget.startRange[i];
        endRange[i] = menuTarget.endRange[i];
    }

    @Override
    protected void onAdvancedClick(int mX, int mY, int button) {
        textBoxes.onClick(mX, mY, button);
    }

    private static final String NBT_START = "StartRange";
    private static final String NBT_END = "EndRange";

    @Override
    protected void loadAdvancedComponent(NBTTagCompound directionTag, int i) {
        startRange[i] = directionTag.getByte(NBT_START);
        endRange[i] = directionTag.getByte(NBT_END);
    }

    @Override
    protected void saveAdvancedComponent(NBTTagCompound directionTag, int i) {
        directionTag.setByte(NBT_START, (byte)getStart(i));
        directionTag.setByte(NBT_END, (byte)getEnd(i));
    }

    @Override
    protected void resetAdvancedSetting(int i) {
        startRange[i] =  endRange[i] = 0;
    }

    @Override
    protected void refreshAdvancedComponentData(ContainerManager container, ComponentMenu newData, int i) {
        ComponentMenuTargetInventory newDataTarget = (ComponentMenuTargetInventory)newData;

        if (startRange[i] != newDataTarget.startRange[i]) {
            startRange[i] =  newDataTarget.startRange[i];

            writeUpdatedData(container, i, DataTypeHeader.START_OR_TANK_DATA, startRange[i]);
        }

        if (endRange[i] != newDataTarget.endRange[i]) {
            endRange[i] =  newDataTarget.endRange[i];

            writeUpdatedData(container, i, DataTypeHeader.END, endRange[i]);
        }
    }

    @Override
    protected void readAdvancedNetworkComponent(DataTypeHeader header, int i, int data) {
        switch (header) {
            case START_OR_TANK_DATA:
                startRange[i] = data;
                refreshAdvancedComponent();
                break;
            case END:
                endRange[i] = data;
                refreshAdvancedComponent();
        }
    }


    public int getStart(int i) {
        return startRange[i];
    }

    public int getEnd(int i) {
        return endRange[i];
    }

    @Override
    public void addErrors(List<String> errors) {
        for (int i = 0; i < directions.length; i++) {
            if (isActive(i) && getStart(i) > getEnd(i)) {
                errors.add("The " + directions[i].toString().charAt(0) + directions[i].toString().toLowerCase().substring(1) + " range is invalid");
            }
        }

        for (int i = 0; i < directions.length; i++) {
            if (isActive(i)) {
                return;
            }
        }

        errors.add("No direction is active");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean onKeyStroke(GuiManager gui, char c, int k) {
        if (selectedDirectionId != -1 && useAdvancedSetting(selectedDirectionId)) {
            return textBoxes.onKeyStroke(gui, c, k);
        }


        return false;
    }
}