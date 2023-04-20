import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class MessageStorage implements Serializable{

    private Map<String, Chat> storage;
    private ArrayList<String> userList;

    public MessageStorage() {
        storage = new TreeMap<>();
        userList = new ArrayList<>();

    }

    public MessageStorage getChatsForUser(String user) {
        MessageStorage result = new MessageStorage();
        for (Map.Entry<String, Chat> entry : storage.entrySet()) {
            String userCombination = entry.getKey();
            Chat chat = entry.getValue();

            String[] users = userCombination.split(":");
            if (users.length != 2) {
                continue;
            }

            if (users[0].equals(user) || users[1].equals(user)) {
                result.storage.put(userCombination, chat);
            }
        }

        return result;
    }

    public void addMessage(Message message, String serverName) {
        if(message.getReciver().contains("Server") == false){
            String userCombination = getUserCombinationKey(message.getSender(), message.getReciver());
            Chat chat = storage.getOrDefault(userCombination, new Chat());
            chat.addMessage(message);
            storage.put(userCombination, chat);

            if (userList.contains(message.getSender()) == false) userList.add(message.getSender());
            if (userList.contains(message.getReciver()) == false) userList.add(message.getReciver());
            //Nur Nachrichten hinzufügen, die von Usern sind, und auf einen Server Storage geschrieben werden
            if(serverName.contains("Server") && message.getMessageText()!=null && serverName.equals("NONE") == false){
                addMessageToTextFile(message, serverName);
            }
        }
    }
    private void addMessageToTextFile(Message message, String serverName) {
        String os = System.getProperty("os.name").toLowerCase();
        String trenner;

        if (os.contains("win")) {
            // Windows
            trenner = "\\\\";
        } else {
            // Unix-basierte Systeme (z.B. Mac OS X, Linux)
            trenner = "/";
        }
        String filename = "src" + trenner + "messageData" + trenner  + serverName + "Messages.txt";
        FileWriter fileWriter = null; // FileWriter zum Schreiben in die Datei (true bedeutet, dass die Datei angehängt wird)
        try {
            fileWriter = new FileWriter(filename, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter); // BufferedWriter zum Puffern der Ausgabe

            // Schreibe Nachricht mit Metadaten in Textdatei
            bufferedWriter.write(message.getSender() + ";");
            bufferedWriter.write(message.getReciver() + ";");
            bufferedWriter.write(message.getTimestamp() + ";");
            bufferedWriter.write(message.getMessageText());
            bufferedWriter.newLine(); // Füge eine neue Zeile hinzu, um die Nachrichten zu trennen

            // Schließe den BufferedWriter
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public TreeMap<UniqueTimestamp, Message> getMessages(String userOne, String userTwo) {
        String userCombination = getUserCombinationKey(userOne, userTwo);
        Chat chat = storage.get(userCombination);
        if (chat == null) {
            return null;
        }
        return chat.getMessages();
    }

    private String getUserCombinationKey(String userOne, String userTwo) {
        return userOne.compareTo(userTwo) < 0 ? userOne + ":" + userTwo : userTwo + ":" + userOne;
    }

    public class Chat implements Serializable {
        private TreeMap<UniqueTimestamp, Message> chat;

        public Chat() {
            chat = new TreeMap<>();
        }

        public void addMessage(Message message) {
            UniqueTimestamp uniqueTimestamp = new UniqueTimestamp(message.getSender(), message.getTimestamp());
            chat.put(uniqueTimestamp, message);
        }

        public TreeMap<UniqueTimestamp, Message> getMessages() {
            return chat;
        }
    }
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" \n === Message Storage ===\n");
        for (Map.Entry<String, Chat> entry : storage.entrySet()) {
            String userCombination = entry.getKey();
            Chat chat = entry.getValue();

            stringBuilder.append("Users: ").append(userCombination).append("\n");
            TreeMap<UniqueTimestamp, Message> messages = chat.getMessages();
            for (Map.Entry<UniqueTimestamp, Message> msgEntry : messages.entrySet()) {
                UniqueTimestamp uniqueTimestamp = msgEntry.getKey();
                Message message = msgEntry.getValue();
                Timestamp timestamp = new Timestamp(message.getTimestamp());
                stringBuilder.append(String.format("  [%s] %s: %s\n", timestamp.toString(), uniqueTimestamp.user, message.getMessageText()));
            }
        }
        stringBuilder.append("=======================\n");
        return stringBuilder.toString();
    }
    public ArrayList<String> getChatPartnersForUser(String username) {
        ArrayList<String> chatPartners = new ArrayList<>();
        for (String key : storage.keySet()) {
            String[] users = key.split(":");
            if (users.length != 2) {
                continue;
            }
            if (users[0].equals(username)) {
                chatPartners.add(users[1]);
            } else if (users[1].equals(username)) {
                chatPartners.add(users[0]);
            }
        }
        return chatPartners;
    }
    public void join(MessageStorage otherStorage) {
        if(otherStorage != null){
            // Join the chat messages
            for (Map.Entry<String, Chat> entry : otherStorage.storage.entrySet()) {
                String key = entry.getKey();
                Chat otherChat = entry.getValue();
                Chat chat = storage.getOrDefault(key, new Chat());
                for (Map.Entry<UniqueTimestamp, Message> msgEntry : otherChat.chat.entrySet()) {
                    UniqueTimestamp uniqueTimestamp = msgEntry.getKey();
                    Message message = msgEntry.getValue();
                    chat.addMessage(message);
                }
                storage.put(key, chat);
            }

            // Join the user list
            for (String user : otherStorage.userList) {
                if (!userList.contains(user)) {
                    userList.add(user);
                }
            }
        }
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MessageStorage other = (MessageStorage) obj;
        if (!Objects.equals(this.storage, other.storage)) {
            return false;
        }
        if (!Objects.equals(this.userList, other.userList)) {
            return false;
        }
        return true;
    }
    public static MessageStorage readFromTextFile(String serverName) {
        MessageStorage messageStorage = new MessageStorage();
        String os = System.getProperty("os.name").toLowerCase();
        String trenner;
        if (os.contains("win")) {
            // Windows
            trenner = "\\\\";
        } else {
            // Unix-basierte Systeme (z.B. Mac OS X, Linux)
            trenner = "/";
        }

        String filename = "src" + trenner + "messageData" + trenner  + serverName + "Messages.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(";");
                if (tokens.length < 4) {
                    continue;
                }
                String sender = tokens[0];
                String receiver = tokens[1];
                long timestamp = Long.parseLong(tokens[2]);
                String messageText = "";
                for(int i = 3; i < tokens.length; i++){
                    messageText += tokens[i];
                }
                Message message = new Message(sender, "RELOADED_FROM_FILE" , receiver, messageText, timestamp);
                messageStorage.addMessage(message, "NONE");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return messageStorage;
    }

}

// Empty message
