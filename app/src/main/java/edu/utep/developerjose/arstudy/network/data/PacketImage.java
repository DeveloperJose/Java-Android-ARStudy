package edu.utep.developerjose.arstudy.network.data;

import android.graphics.Bitmap;

import com.google.ar.sceneform.math.Vector3;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import edu.utep.developerjose.arstudy.serializable.ProxyBitmap;

public class PacketImage extends Packet {
    public Vector3 mPosition;
    public Bitmap mBitmap;

    public PacketImage(ObjectInputStream input) throws Exception {
        super(PacketID.IMAGE);
        float x = input.readFloat();
        float y = input.readFloat();
        float z = input.readFloat();
        mPosition = new Vector3(x, y, z);

        ProxyBitmap proxyBitmap = (ProxyBitmap) input.readObject();
        mBitmap = proxyBitmap.getBitmap();
    }

    public PacketImage(Vector3 position, Bitmap bitmap) {
        super(PacketID.IMAGE);
        mPosition = position;
        mBitmap = bitmap;
    }

    @Override
    public void send(ObjectOutputStream output) throws Exception {
        output.writeInt(PacketID.IMAGE);
        output.writeFloat(mPosition.x);
        output.writeFloat(mPosition.y);
        output.writeFloat(mPosition.z);

        output.writeObject(new ProxyBitmap(mBitmap));
    }
}
