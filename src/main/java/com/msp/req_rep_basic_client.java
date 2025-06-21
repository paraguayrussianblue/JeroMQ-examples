package com.msp; 
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;

public class req_rep_basic_client {
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.REQ);
            System.out.println("Connecting to hello world server…");
            socket.connect("tcp://localhost:5555");

            for (int request = 0; request < 10; request++) {
                System.out.println("Sending request " + request + " …");
                socket.send("Hello".getBytes(ZMQ.CHARSET), 0);

                byte[] replyBytes = socket.recv(0);
                if (replyBytes != null) {
                    String reply = new String(replyBytes, ZMQ.CHARSET);
                    System.out.println("Received reply " + request + " [ " + reply + " ]");
                }
            }
        }
    }
}
