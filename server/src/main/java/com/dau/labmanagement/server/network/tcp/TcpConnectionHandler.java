package com.dau.labmanagement.server.network.tcp;

import java.io.IOException;
import java.net.Socket;

@FunctionalInterface
public interface TcpConnectionHandler {

    void handle(Socket socket) throws IOException;
}
