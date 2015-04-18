package com.nnarain.eseplatformsupervisor.client;

import java.io.IOException;

/**
 * Created by Natesh on 12/04/2015.
 */
public interface PacketStream
{

    public void connect();
    public void close();

    public void write(Packet packet) throws IOException;

}
