import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServerController1 {
    public static void main(String[] args) {
        Server1 server1 = new Server1("Server1");
        Server1 server2 = new Server1("Server2");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            try {
                String command = br.readLine();

                switch (command.toLowerCase()) {
                    case "start server 1":
                        server1 = new Server1("Server1");
                        server1.start();
                        break;
                    case "stop server 1":
                        server1.serverSocket.close();
                        server1.stop();
                        break;
                    case "interrupt server 1":
                        server1.interruptStatus = "interrupt";
                        break;
                    case "start server 2":
                        server2 = new Server1("Server2");
                        server2.start();
                        break;
                    case "stop server 2":
                        server2.serverSocket.close();
                        server2.stop();
                        break;
                    case "interrupt server 2":
                        server2.interruptStatus = "interrupt";
                        break;
                    default:
                        System.out.println("Ungültiger Befehl.");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
