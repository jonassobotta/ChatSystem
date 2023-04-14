import org.junit.jupiter.api.*;

class ServerTest {
    private static Server1 server1One;
    private static Server1 server1Two;
    private static ClientLogic clientLogic;

    @BeforeAll
    public static void beforeAll() {
        server1One = new Server1("Server1");
        server1Two = new Server1("Server2");
        clientLogic = new ClientLogic(null);
    }


    @Test
    public void serverSyncTest() {
        try {
            server1One.start();
            clientLogic.setUsername("joel");
            clientLogic.setPassword("joel");
            clientLogic.checkUserData(0);
            clientLogic.setReceiver("luca");
            clientLogic.sendMessage("Hallo");
            server1Two.start();
            wait(4000);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Assertions.assertEquals(true, server1One.messageStorage.equals(server1Two.messageStorage));
        Assertions.assertEquals(true, server1One.userPortStorage.equals(server1Two.userPortStorage));
    }

    @Test
    public void serverRestartSyncTest() {
        try {
            server1One.start();
            server1Two.start();
            clientLogic.setUsername("joel");
            clientLogic.setPassword("joel");
            clientLogic.checkUserData(0);
            clientLogic.setReceiver("luca");
            clientLogic.sendMessage("Hallo");
            server1Two.stop();
            clientLogic.sendMessage("Wie gehts");
            server1Two = new Server1("Server2");
            server1Two.start();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Assertions.assertEquals(true, server1One.messageStorage.equals(server1Two.messageStorage));
        Assertions.assertEquals(true, server1One.userPortStorage.equals(server1Two.userPortStorage));
    }

}