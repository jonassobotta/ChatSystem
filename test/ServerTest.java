import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    private static Server serverOne;
    private static Server serverTwo;
    private static ClientLogic clientLogic;

    @BeforeAll
    public static void beforeAll() {
        serverOne = new Server(7777);
        serverTwo = new Server(8888);
        clientLogic = new ClientLogic(null);
    }


    @Test
    public void serverSyncTest() {
        try {
            serverOne.start();
            clientLogic.setUsername("joel");
            clientLogic.setPassword("joel");
            clientLogic.checkUserData(0);
            clientLogic.setReceiver("luca");
            clientLogic.sendMessage("Hallo");
            serverTwo.start();
            wait(4000);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Assertions.assertEquals(true, serverOne.messageStorage.equals(serverTwo.messageStorage));
        Assertions.assertEquals(true, serverOne.userPortStorage.equals(serverTwo.userPortStorage));
    }

    @Test
    public void serverRestartSyncTest() {
        try {
            serverOne.start();
            serverTwo.start();
            clientLogic.setUsername("joel");
            clientLogic.setPassword("joel");
            clientLogic.checkUserData(0);
            clientLogic.setReceiver("luca");
            clientLogic.sendMessage("Hallo");
            serverTwo.stop();
            clientLogic.sendMessage("Wie gehts");
            serverTwo = new Server(8888);
            serverTwo.start();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Assertions.assertEquals(true, serverOne.messageStorage.equals(serverTwo.messageStorage));
        Assertions.assertEquals(true, serverOne.userPortStorage.equals(serverTwo.userPortStorage));
    }

}