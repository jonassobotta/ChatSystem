import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class ChatUI extends JFrame {
    private JPanel panelCards;
    private JPanel panelLogin;
    private JPanel panelChatList;
    private JPanel panelChatView;
    private ArrayList<String> chats;
    private JList<String> chatList;


    public ChatUI() {
        // Set the size and title of the JFrame
        setSize(400, 300);
        setTitle("Chat Application");
        chats = new ArrayList<>();
        chats.add("Alice");
        chats.add("Bob");
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
        DefaultListModel<String> chatListModel = new DefaultListModel<>();
        chatListModel.addElement("Chat 1");
        chatListModel.addElement("Chat 2");
        chatListModel.addElement("Chat 3");
        JList<String> chatList = new JList<>(chatListModel);
        JScrollPane chatListScrollPane = new JScrollPane(chatList);
        panelChatList.add(chatListScrollPane, BorderLayout.CENTER);
        JButton buttonAddChat = new JButton("Add Chat");
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
                    if (!chats.contains(newChat)) {
                        chats.add(newChat);
                        chatList.setListData(chats.toArray(new String[0]));
                    }
                }
            }
        });
        panelChatList.add(buttonAddChat, BorderLayout.SOUTH);

        // Create the chat view panel
        panelChatView = new JPanel(new BorderLayout());
        JLabel labelChatView = new JLabel("Chat View");
        panelChatView.add(labelChatView, BorderLayout.NORTH);
        JTextArea chatTextArea = new JTextArea();
        JScrollPane chatScrollPane = new JScrollPane(chatTextArea);
        panelChatView.add(chatScrollPane, BorderLayout.CENTER);
        JButton buttonBack = new JButton("Back");
        panelChatView.add(buttonBack, BorderLayout.SOUTH);

        // Create a CardLayout to switch between the panels
        panelCards = new JPanel(new CardLayout());
        panelCards.add(panelLogin, "Login");
        panelCards.add(panelChatList, "ChatList");
        panelCards.add(panelChatView, "ChatView");

        // Add action listeners to the buttons
        buttonLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: Perform login validation
                CardLayout cl = (CardLayout) panelCards.getLayout();
                cl.show(panelCards, "ChatList");
            }
        });

        buttonAddChat.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: Implement chat creation
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

    public static void main(String[] args) {
        new ChatUI();
    }
}
