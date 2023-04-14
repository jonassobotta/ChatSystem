import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UserTest {
    private static Server1 server1One;
    private static Server1 server1Two;
    private static ClientLogic clientLogic;

    @BeforeAll
    public static void beforeAll(){
        server1One = new Server1("Server1");
        server1Two = new Server1("Server2");
        clientLogic = new ClientLogic(null);
    }

    @Test
    public void testUserLoginSuccess(){
        try{
            server1One.start();
            clientLogic.setUsername("Joel");
            clientLogic.setPassword("Joel");
            clientLogic.checkUserData(0);

        } catch (Exception e){
            System.out.println(e.getMessage());
        }
        //Assertions .assertEquals(true,);
    }

}
