package com.msp;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;

public class pub_sub_basic_client {
    public static void main(String[] args) {

        try (ZContext context = new ZContext()) {
            System.out.println("Collecting updates from weather server...");
            ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
            subscriber.connect("tcp://localhost:5556");
                    
            String zipFilter = (args.length > 0) ? args[0] : "10001";
            subscriber.subscribe(zipFilter.getBytes(ZMQ.CHARSET));

            int totalTemp = 0;
            int updateNbr;

            for (updateNbr = 0; updateNbr < 20; updateNbr++) {
                String message = subscriber.recvStr(0);
                if (message == null) {
                    continue;
                }

                String[] parts = message.split("\\s+");
                int temperature = Integer.parseInt(parts[1]);
                totalTemp += temperature;

                System.out.printf(
                    "Receive temperature for zipcode '%s' was %d F%n",
                    zipFilter, temperature
                );
            }

            double average = (double) totalTemp / (updateNbr + 1);
            System.out.printf(
                "Average temperature for zipcode '%s' was %.2f F%n",
                zipFilter, average
            );
        }
    }
}
