package edu.utep.developerjose.arstudy.network;


import edu.utep.developerjose.arstudy.network.data.Packet;

public interface NetInterface {
    void onConnect();

    void onDisconnect();

    void onReceiveData(final Packet p);
}
