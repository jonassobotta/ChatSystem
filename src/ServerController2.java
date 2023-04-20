import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class ServerController2 {
    public static void main(String[] args) {
        ArrayList<Server2> serverList = new ArrayList<>();
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
                        serverList.get(i).start();
                    }
                } else if (tokens[0].equals("stop") && tokens[1].equals("all")) {
                    for (int i = 0; i < 3; i++){
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
