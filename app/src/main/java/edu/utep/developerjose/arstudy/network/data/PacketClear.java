package edu.utep.developerjose.arstudy.network.data;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PacketClear extends Packet {
    public PacketClear(ObjectInputStream input) {
        super(PacketID.CLEAR);
    }

    public PacketClear() {
        super(PacketID.CLEAR);
    }

    @Override
    public void send(ObjectOutputStream output) throws Exception {
        output.writeInt(PacketID.CLEAR);
    }
}
