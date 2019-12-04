package edu.utep.developerjose.arstudy.network.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PacketMessage extends Packet {
    public String mStrMessage;

    public PacketMessage(ObjectInputStream input) throws IOException {
        super(PacketID.MESSAGE);
        mStrMessage = input.readUTF();
    }

    public PacketMessage(String message) {
        super(PacketID.MESSAGE);
        mStrMessage = message;
    }

    @Override
    public void send(ObjectOutputStream output) throws Exception {
        output.writeInt(PacketID.MESSAGE);
        output.writeUTF(mStrMessage);
    }
}
