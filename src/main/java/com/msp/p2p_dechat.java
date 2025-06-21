package com.msp;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class p2p_dechat {
    private static final int PORT_NAMESERVER = 9001;
    private static final int PORT_CHAT_PUB = 9002;
    private static final int PORT_CHAT_PULL = 9003;
    private static final int PORT_DB = 9004;
    private static final AtomicBoolean shutdown = new AtomicBoolean(false);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: java com.msp.P2PChat <userName>");
            return;
        }

        System.out.println("starting p2p chatting program. ");

        String userName = args[0];
        String localIp = getLocalIp();
        String ipMask = localIp.substring(0, localIp.lastIndexOf('.'));

        System.out.println("searching for p2p server.");
        String nameServerIp = searchNameServer(ipMask, localIp);
        if (nameServerIp == null) {
            System.out.println("p2p server is not found, nd p2p server mode is activated.");
    
            new Thread(() -> beaconNameServer(localIp)).start();
            System.out.println("p2p beacon server is activated.");

            new Thread(() -> userManagerNameServer(localIp)).start();
            System.out.println("p2p subscriber database server is activated.");

            new Thread(() -> relayServer(localIp)).start();
            System.out.println("p2p relay server is activated.");
            nameServerIp = localIp;
        } else {
            System.out.println("p2p server found at " + nameServerIp + ", and p2p client mode is activated.");
        }

        System.out.println("starting user registration procedure.");
        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket dbReq = ctx.createSocket(SocketType.REQ);
            dbReq.connect("tcp://" + nameServerIp + ":" + PORT_DB);
            String reg = localIp + ":" + userName;
            dbReq.send(reg.getBytes(ZMQ.CHARSET), 0);
            String resp = dbReq.recvStr(0);
            if ("ok".equals(resp)) {
                System.out.println("user registration to p2p server completed.");
            } else {
                System.out.println("user registration to p2p server failed.");
            }
        }

        System.out.println("starting message transfer procedure.");

        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket sub = ctx.createSocket(SocketType.SUB);
            sub.connect("tcp://" + nameServerIp + ":" + PORT_CHAT_PUB);
            sub.subscribe("RELAY".getBytes(ZMQ.CHARSET));

            ZMQ.Poller poller = ctx.createPoller(1);
            poller.register(sub, ZMQ.Poller.POLLIN);

            ZMQ.Socket push = ctx.createSocket(SocketType.PUSH);
            push.connect("tcp://" + nameServerIp + ":" + PORT_CHAT_PULL);

            Random rand = new Random();
            System.out.println("starting autonomous message transmit and receive scenario.");
            while (!shutdown.get()) {
                // 폴링 대기 (100ms)
                poller.poll(100);
                if (poller.pollin(0)) {
                    String msg = sub.recvStr(0);
                    String[] parts = msg.split(":", 3);
                    System.out.println("p2p-recv::<<== " + parts[1] + ":" + parts[2]);
                } else {
                    int r = rand.nextInt(100) + 1;
                    if (r < 10) {
                        Thread.sleep(3000);
                        String m = "(" + userName + "," + localIp + ":ON)";
                        push.send(m.getBytes(ZMQ.CHARSET), 0);
                        System.out.println("p2p-send::==>> " + m);
                    } else if (r > 90) {
                        Thread.sleep(3000);
                        String m = "(" + userName + "," + localIp + ":OFF)";
                        push.send(m.getBytes(ZMQ.CHARSET), 0);
                        System.out.println("p2p-send::==>> " + m);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("closing p2p chatting program.");
    }

    private static String searchNameServer(String ipMask, String localIp) {
        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket sub = ctx.createSocket(SocketType.SUB);
            sub.setReceiveTimeOut(2000);
            sub.subscribe("NAMESERVER".getBytes(ZMQ.CHARSET));
            for (int i = 1; i < 255; i++) {
                String addr = "tcp://" + ipMask + "." + i + ":" + PORT_NAMESERVER;
                sub.connect(addr);
            }
            String res = sub.recvStr(0);
            if (res != null && res.startsWith("NAMESERVER:")) {
                return res.split(":", 2)[1];
            }
        }
        return null;
    }

    private static void beaconNameServer(String localIp) {
        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket pub = ctx.createSocket(SocketType.PUB);
            pub.bind("tcp://" + localIp + ":" + PORT_NAMESERVER);
            System.out.println("local p2p name server bind to tcp://" + localIp + ":" + PORT_NAMESERVER);
            while (!shutdown.get()) {
                Thread.sleep(1000);
                String m = "NAMESERVER:" + localIp;
                pub.send(m.getBytes(ZMQ.CHARSET), 0);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void userManagerNameServer(String localIp) {
        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket rep = ctx.createSocket(SocketType.REP);
            rep.bind("tcp://" + localIp + ":" + PORT_DB);
            System.out.println("local p2p db server activated at tcp://" + localIp + ":" + PORT_DB);
            while (!shutdown.get()) {
                String req = rep.recvStr(0);
                String[] parts = req.split(":", 2);
                System.out.println("user registration '" + parts[1] + "' from '" + parts[0] + "'.");
                rep.send("ok");
            }
        }
    }

    private static void relayServer(String localIp) {
        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket pub = ctx.createSocket(SocketType.PUB);
            pub.bind("tcp://" + localIp + ":" + PORT_CHAT_PUB);
            ZMQ.Socket pull = ctx.createSocket(SocketType.PULL);
            pull.bind("tcp://" + localIp + ":" + PORT_CHAT_PULL);
            System.out.println("local p2p relay server activated at tcp://" + localIp + ":" + PORT_CHAT_PUB + " & " + PORT_CHAT_PULL);
            while (!shutdown.get()) {
                String msg = pull.recvStr(0);
                System.out.println("p2p-relay:<==> " + msg);
                String out = "RELAY:" + msg;
                pub.send(out.getBytes(ZMQ.CHARSET), 0);
            }
        }
    }

    private static String getLocalIp() {
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.connect(InetAddress.getByName("8.8.8.8"), 80);
            return ds.getLocalAddress().getHostAddress();
        } catch (Exception ignored) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                return "127.0.0.1";
            }
        }
    }
}
