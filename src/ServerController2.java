import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
//Für gute Steuerung der Server
public class ServerController2 {
    public static void main(String[] args) {
        ArrayList<Server2> serverList = new ArrayList<>();
        ArrayList<String> serverName = new ArrayList<>();
        serverName.add("Server1");
        serverName.add("Server2");
        serverName.add("Server3");

        serverList.add( new Server2("Server1","START"));
        serverList.add( new Server2("Server2","START"));
        serverList.add( new Server2("Server3","START"));
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            try {
                String command = br.readLine();

                String[] tokens = command.toLowerCase().split(" ");

                if(tokens[0].equals("start") && tokens[1].equals("all")){
                    for (int i = 0; i < 3; i++){
                        serverList.set(i, new Server2(serverName.get(i), "Start"));
                        serverList.get(i).start();
                    }
                } else if (tokens[0].equals("stop") && tokens[1].equals("all")) {
                    for (int i = 0; i < 3; i++){
                        serverList.get(i).serverSocket.close();
                        serverList.get(i).stopServer();
                    }
                }
                else if(tokens[0].equals("start")){
                    serverList.set(Integer.parseInt(tokens[2]) - 1, new Server2(getServerName(Integer.parseInt(tokens[2])), "START"));
                    serverList.get(Integer.parseInt(tokens[2]) - 1).start();
                } else if (tokens[0].equals("stop")) {
                    serverList.get(Integer.parseInt(tokens[2])-1).serverSocket.close();
                    serverList.get(Integer.parseInt(tokens[2])-1).stopServer();
                }else if (tokens[0].equals("interrupt")) {
                    serverList.get(Integer.parseInt(tokens[2])-1).interruptStatus = "interrupt";
                }

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
    public static String getServerName(int index){
        return "Server"+index;
    }
}
