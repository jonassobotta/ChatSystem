import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Server2Test {

    @Test
    public void testServerReboot() {
        Server2 server1 = new Server2("Server1", "START");
        Server2 server2 = new Server2("Server2", "START");
        Server2 server3 = new Server2("Server3", "START");

        server1.start();
        server2.start();
        server3.start();

        synchronized (this) {
            try {
                wait(20000);
                server3.stopServer();
                wait(20000);
                server3 = new Server2("Server3", "REBOOT");
                server3.start();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        while (true){

        }


    }


}