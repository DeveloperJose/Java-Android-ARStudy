package edu.utep.developerjose.arstudy.network.data;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public abstract class Packet {
    public int ID;

    protected Packet(int ID) {
        this.ID = ID;
    }

    public abstract void send(ObjectOutputStream output) throws Exception;

    public static Packet read(ObjectInputStream input) throws Exception {
        int ID = input.readInt();

        if (ID == PacketID.IMAGE)
            return new PacketImage(input);
        else if (ID == PacketID.MESSAGE)
            return new PacketMessage(input);
        else if (ID == PacketID.CLEAR)
            return new PacketClear(input);
        return null;
    }
}