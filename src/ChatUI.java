import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;


public class ChatUI extends JFrame {
    private JPanel panelCards;
    private JPanel headerPanel;
    private JPanel panelLogin;
    private JPanel panelChatList;
    private JPanel panelChatView;
    private ArrayList<String> chats;
    private JList<String> chatList;
    //private ClientLogic clientLogic;
    private ClientLogic2 clientLogic;
    private String currentChatPartner;
    private ChatUI ownInstance;

    public ChatUI() {
        this.ownInstance = this;

        //clientLogic = new ClientLogic(this);
        clientLogic = new ClientLogic2(this);
        clientLogic.start();

        // Set the size and title of the JFrame
        setSize(400, 300);
        setTitle("Chat Application");
        chats = new ArrayList<>();

        chatList = new JList<>(chats.toArray(new String[0]));

        // Create the login panel
        panelLogin = new JPanel(new BorderLayout());
        JLabel labelLogin = new JLabel("Login");
        panelLogin.add(labelLogin, BorderLayout.NORTH);
        JPanel panelLoginFields = new JPanel(new GridLayout(2, 2));
        panelLoginFields.add(new JLabel("Username:"));
        JTextField usernameField = new JTextField();
        panelLoginFields.add(usernameField);
        panelLoginFields.add(new JLabel("Password:"));
        JPasswordField passwordField = new JPasswordField();
        panelLoginFields.add(passwordField);
        panelLogin.add(panelLoginFields, BorderLayout.CENTER);
        JButton buttonLogin = new JButton("Login");
        panelLogin.add(buttonLogin, BorderLayout.SOUTH);

        // Create the chat list panel
        panelChatList = new JPanel(new BorderLayout());
        JLabel labelChatList = new JLabel("Chat List");
        panelChatList.add(labelChatList, BorderLayout.NORTH);

        JPanel panelHeader = new JPanel(new BorderLayout());
        JLabel labelHeader = new JLabel("Chats von: ");
        panelHeader.add(labelHeader, BorderLayout.WEST);
        panelChatList.add(panelHeader, BorderLayout.NORTH);


        JButton buttonLogout = new JButton("Logout");
        panelHeader.add(buttonLogout, BorderLayout.EAST);


        JScrollPane chatListScrollPane = new JScrollPane(chatList);
        panelChatList.add(chatListScrollPane, BorderLayout.CENTER);
        JButton buttonAddChat = new JButton("Add Chat");

        buttonLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("logout pls");
                passwordField.setText("");
                usernameField.setText("");
                clientLogic.setReceiver("");
                clientLogic.setPassword("");
                clientLogic.setUsername("");

                try {
                    clientLogic.serverSocket.close();
                    clientLogic.interrupt();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                clientLogic = new ClientLogic2(ownInstance);
                clientLogic.start();
                CardLayout cl = (CardLayout) panelCards.getLayout();
                cl.show(panelCards, "Login");

            }
        });

        buttonAddChat.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // create pop-up window
                JPanel popupPanel = new JPanel(new FlowLayout());
                JTextField textField = new JTextField(10);
                popupPanel.add(new JLabel("Enter chat partner name: "));
                popupPanel.add(textField);

                int result = JOptionPane.showConfirmDialog(ChatUI.this, popupPanel, "Add Chat", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    String newChat = textField.getText();
                    if (chats.contains(newChat)) {
                        showChatAlreadyExistsPopup();
                    }else if(clientLogic.getUsername().equals(newChat)){
                        showMessageToOwnNamePopup();
                    }
                    else{
                        chats.add(newChat);
                        chatList.setListData(chats.toArray(new String[0]));
                    }
                }
            }
        });
        panelChatList.add(buttonAddChat, BorderLayout.SOUTH);
        chatList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedChat = chatList.getSelectedValue();
                    openChat(selectedChat);
                }
            }
        });

        // Create the chat view panel
        panelChatView = new JPanel(new BorderLayout());
        headerPanel = new JPanel(new BorderLayout());
        JLabel labelChatView = new JLabel("Chat View");
        JButton buttonBack = new JButton("Back");
        headerPanel.add(buttonBack, BorderLayout.WEST);
        headerPanel.add(labelChatView, BorderLayout.CENTER);
        panelChatView.add(headerPanel, BorderLayout.NORTH);

        JTextArea chatTextArea = new JTextArea();
        JScrollPane chatScrollPane = new JScrollPane(chatTextArea);
        panelChatView.add(chatScrollPane, BorderLayout.CENTER);

// Create the message input panel
        JPanel messageInputPanel = new JPanel(new BorderLayout());
        JTextField messageInputField = new JTextField();
        messageInputPanel.add(messageInputField, BorderLayout.CENTER);
        JButton buttonSend = new JButton("Send");
        messageInputPanel.add(buttonSend, BorderLayout.EAST);
        panelChatView.add(messageInputPanel, BorderLayout.SOUTH);


// Add action listener to the send button
        buttonSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = messageInputField.getText();
                System.out.println("Message: " + message);
                messageInputField.setText("");
                try {
                    Message answer = clientLogic.sendMessage(message);
                    if(answer.getStatus().equals("FAILED")){
                        showBadConnectionPopup();
                    }
                } catch (Exception error){
                    System.out.println(error.getMessage());
                }

            }
        });


        // Create a CardLayout to switch between the panels
        panelCards = new JPanel(new CardLayout());
        panelCards.add(panelLogin, "Login");
        panelCards.add(panelChatList, "ChatList");
        panelCards.add(panelChatView, "ChatView");

        // Add action listeners to the buttons
        buttonLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                System.out.println("Username: " + username);
                clientLogic.setUsername(username);
                System.out.println("Password: " + password);
                clientLogic.setPassword(password);

                try {
                    String stateOfConnection = clientLogic.checkUserData(0);
                    System.out.println(stateOfConnection);
                    if (stateOfConnection.equals("OK")) {
                        //weiter
                        JLabel labelHeaderUserName = new JLabel(username);
                        panelHeader.add(labelHeaderUserName, BorderLayout.CENTER);
                        chats = clientLogic.getAllChatPartners(username);
                        chatList.setListData(chats.toArray(new String[0]));
                        CardLayout cl = (CardLayout) panelCards.getLayout();
                        cl.show(panelCards, "ChatList");
                    } else if (stateOfConnection.equals("INVALID_USER")){
                        showInvalidPasswordPopup();
                    }else {
                        //zurück
                        showBadConnectionPopup();
                    }

                } catch (Exception error) {
                    System.out.println(error.getMessage());
                }
            }
        });


        buttonBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CardLayout cl = (CardLayout) panelCards.getLayout();
                cl.show(panelCards, "ChatList");
            }
        });

        // Add the card panel to the JFrame
        add(panelCards);

        // Show the JFrame
        setVisible(true);
    }

    private void showInvalidPasswordPopup() {
        JOptionPane.showMessageDialog(this, "Invalid password. Please try again.", "Login Error", JOptionPane.ERROR_MESSAGE);
    }
    private void showMessageToOwnNamePopup() {
        JOptionPane.showMessageDialog(this, "Messages to your self is not possible", "Chat Error", JOptionPane.ERROR_MESSAGE);
    }
    private void showChatAlreadyExistsPopup() {
        JOptionPane.showMessageDialog(this, "Chat already exists", "Chat Error", JOptionPane.ERROR_MESSAGE);
    }
    private void showBadConnectionPopup() {
        JOptionPane.showMessageDialog(this, "Bad Connection, try again later", "Connection Error", JOptionPane.ERROR_MESSAGE);
    }
    public void updateChatList(){
        chats = clientLogic.getAllChatPartners(clientLogic.getUsername());
        chatList.setListData(chats.toArray(new String[0]));
    }
    private void openChat(String chatPartner) {

        clientLogic.setReceiver(chatPartner);
        System.out.println(chatPartner);
        // Set the chat view panel title
        JLabel labelChatView = (JLabel) headerPanel.getComponent(1);
        labelChatView.setText("Chat View: " + chatPartner);
        currentChatPartner = chatPartner;

        // Initialize the chat view panel
        initializeChatView();

        // Show the chat view panel
        CardLayout cl = (CardLayout) panelCards.getLayout();
        cl.show(panelCards, "ChatView");
    }

    public void initializeChatView() {
        if (currentChatPartner != null) {
            TreeMap<UniqueTimestamp, Message> messages = clientLogic.printHistoryOfChat(currentChatPartner);
            JTextArea chatTextArea = (JTextArea) ((JScrollPane) panelChatView.getComponent(1)).getViewport().getView();
            chatTextArea.setText("");
            if (messages != null) {
                for (Map.Entry<UniqueTimestamp, Message> entry : messages.entrySet()) {
                    Message message = entry.getValue();
                    String sender = message.getSender();
                    if(sender.equals(clientLogic.getUsername())){
                        sender= "Ich";
                    }
                    String messageText = message.getMessageText();
                    String messageTime = message.getFormatChatMessageTime();
                    chatTextArea.append("(" + messageTime + ")" + " " + sender + ": " + messageText + "\n");
                    System.out.println(messageText);
                }
            }
        }
    }

    public static void main(String[] args) {
        new ChatUI();
    }
}
