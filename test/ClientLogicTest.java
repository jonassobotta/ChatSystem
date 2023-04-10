import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ClientLogicTest {

    private static ClientLogic clientLogicInstance;
    @BeforeAll
    public static void beforeAll(){
        clientLogicInstance = new ClientLogic(null);
    }
    @Test
    public void testBadServerConnection(){
        String status = "";
        try {
            status = clientLogicInstance.checkUserData(0);
        } catch (Exception e){
            System.out.println("komischer fehelr" + e.getMessage());
        }
        Assertions.assertEquals("FAILED", status);
    }

}