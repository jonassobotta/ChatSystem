import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UserTest {
    private static Server serverOne;
    private static Server serverTwo;
    private static ClientLogic clientLogic;

    @BeforeAll
    public static void beforeAll(){
        serverOne = new Server(7777);
        serverTwo = new Server(8888);
        clientLogic = new ClientLogic(null);
    }

    @Test
    public void testUserLoginSuccess(){
        try{
            serverOne.start();
            clientLogic.setUsername("Joel");
            clientLogic.setPassword("Joel");
            clientLogic.checkUserData(0);

        } catch (Exception e){
            System.out.println(e.getMessage());
        }
        //Assertions .assertEquals(true,);
    }

}
