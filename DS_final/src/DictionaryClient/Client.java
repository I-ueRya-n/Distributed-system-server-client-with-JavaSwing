package DictionaryClient;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URLEncoder;
import org.json.*;

public class Client {

    private JFrame frame;
    private JTextField wordField;
    private JTextField meaningField;
    private JTextField existingMeaningField;
    private JLabel meaningLabel;
    private JLabel existingMeaningLabel;
    private JTextArea resultArea;
    private JComboBox<String> actionBox;
    private JButton sendButton, disconnectButton, searchCambridgeButton;
    private JPanel inputPanel;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private String serverAddress;
    private int serverPort;

    private DefaultComboBoxModel<String> actionModel;

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 9092;
        // set up port and dictionary loading logic
        if (args.length > 0) {
            try {
                serverAddress = args[0];
                if (args.length > 1) {
                    serverPort = Integer.parseInt(args[1]);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default port " + serverPort);
            }
        }
        new Client(serverAddress, serverPort);
    }


    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        guiStructure();
        connectToServer();
    }

    private void connectToServer() {
        try {
            // setting up the connection between server and client
            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            resultArea.append("Connected to server.\n");
        } catch (IOException e) {
            // pop out the message box when failed to connect
            JOptionPane.showMessageDialog(frame, "Connection failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // initialise the GUI interface and function logic
    private void guiStructure() {
        frame = new JFrame("1537312 YU-WEI LIN Distributed System assignment 1");

        // Set University of Melbourne logo as the frame icon
        ImageIcon uniMelbIcon = new ImageIcon("C:\\Users\\ryan0\\Downloads\\uniMelbIcon.png");  // Update with correct path to the logo
        Image img = uniMelbIcon.getImage();
        Image largeIcon = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
        frame.setIconImage(uniMelbIcon.getImage());


        frame.setSize(700, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(Color.decode("#F5F5F5"));

        // set up the panel color and label and the size of GUI window
        inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        inputPanel.setBackground(Color.decode("#00205B"));  // UniMelb dark blue color
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        wordField = new JTextField(20);
        meaningField = new JTextField(20);
        existingMeaningField = new JTextField(20);
        meaningLabel = new JLabel("New Meaning:");
        meaningLabel.setForeground(Color.white);
        existingMeaningLabel = new JLabel("Existing Meaning:");
        existingMeaningLabel.setForeground(Color.white);

        // let meaningField and existingMeaningField input panel invisible
        meaningField.setVisible(false);
        meaningLabel.setVisible(false);
        existingMeaningField.setVisible(false);
        existingMeaningLabel.setVisible(false);

        JLabel wordLabel = new JLabel("Word:");
        wordLabel.setForeground(Color.white);
        inputPanel.add(wordLabel);
        inputPanel.add(wordField);
        inputPanel.add(existingMeaningLabel);
        inputPanel.add(existingMeaningField);
        inputPanel.add(meaningLabel);
        inputPanel.add(meaningField);
         // set up toggle box
        actionModel = new DefaultComboBoxModel<>(new String[]{"query", "add", "remove", "addmeaning", "updatemeaning"});
        actionBox = new JComboBox<>(actionModel);
        JLabel actionLabel = new JLabel("Action:");
        actionLabel.setForeground(Color.white);

        inputPanel.add(actionLabel);
        inputPanel.add(actionBox);

        // set up logic for detected the field of word is empnty or not
        wordField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateActions();
                updateCambridgeButton();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateActions();
                updateCambridgeButton();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateActions();
                updateCambridgeButton();
            }
        });

        // Only when word field not be empty, thus the invisible button can be visible
        actionBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedAction = (String) actionBox.getSelectedItem();
                boolean showMeaning = selectedAction.equals("add") || selectedAction.equals("addmeaning") || selectedAction.equals("updatemeaning");
                boolean showExistingMeaning = selectedAction.equals("updatemeaning");

                meaningField.setVisible(showMeaning);
                meaningLabel.setVisible(showMeaning);

                existingMeaningField.setVisible(showExistingMeaning);
                existingMeaningLabel.setVisible(showExistingMeaning);
                frame.revalidate();
                frame.repaint();
            }
        });

        frame.add(inputPanel, BorderLayout.NORTH);

        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        resultArea.setBackground(Color.WHITE);
        resultArea.setForeground(Color.BLACK);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        sendButton = new JButton("Send");
        disconnectButton = new JButton("Disconnect");
        searchCambridgeButton = new JButton("Search Cambridge");
        searchCambridgeButton.setEnabled(false);

        // Set button styles
        for (JButton button : new JButton[]{sendButton, disconnectButton, searchCambridgeButton}) {
            button.setBackground(Color.decode("#0077C8"));  // UniMelb accent blue color
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setFont(new Font("Arial", Font.BOLD, 12));
            button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }

        buttonPanel.setBackground(Color.decode("#00205B"));
        buttonPanel.add(sendButton);
        buttonPanel.add(disconnectButton);
        buttonPanel.add(searchCambridgeButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> userActionHandler());
        disconnectButton.addActionListener(e -> disconnect());

        searchCambridgeButton.addActionListener(e -> {
            String word = wordField.getText().trim();
            if (!word.isEmpty()) {
                try {
                    String url = "https://dictionary.cambridge.org/dictionary/english/" + URLEncoder.encode(word, "UTF-8");
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    resultArea.append("Failed to open browser: " + ex.getMessage() + "\n");
                }
            }
        });

        frame.setVisible(true);
    }


    private void updateActions() {
        String word = wordField.getText().trim();
        if (word.isEmpty()) {
            if (actionModel.getIndexOf("updatemeaning") != -1) actionModel.removeElement("updatemeaning");
            if (actionModel.getIndexOf("addmeaning") != -1) actionModel.removeElement("addmeaning");
            return;
        }
        // detected
        boolean wordExists = checkIfWordExists(word);
        // logic for show up actions in toggle box
        if (wordExists) {
            if (actionModel.getIndexOf("addmeaning") == -1) actionModel.addElement("addmeaning");
            if (actionModel.getIndexOf("updatemeaning") == -1) actionModel.addElement("updatemeaning");
        } else {
            if (actionModel.getIndexOf("addmeaning") != -1) actionModel.removeElement("addmeaning");
            if (actionModel.getIndexOf("updatemeaning") != -1) actionModel.removeElement("updatemeaning");
        }
    }

    private void updateCambridgeButton() {
        searchCambridgeButton.setEnabled(!wordField.getText().trim().isEmpty());
    }

    // Logic of adding a new word
    private boolean checkIfWordExists(String word) {
        JSONObject request = new JSONObject();
        request.put("request", "query");
        request.put("word", word);
        // send the inquiry to server
        output.println(request.toString());

        // Wait for server to determine the action
        try {
            String response = input.readLine();
            if (response != null) {
                JSONObject jsonResponse = new JSONObject(response);
                return jsonResponse.optString("status").equals("success");
            }
        } catch (IOException e) {
            resultArea.append("Error reading server response: " + e.getMessage() + "\n");
        }
        return false;
    }
    // Handle the request from client and send the response to server
    private void userActionHandler() {
        String word = wordField.getText().trim();
        String action = (String) actionBox.getSelectedItem();
        String meaning = meaningField.getText().trim();
        String existingMeaning = existingMeaningField.getText().trim();

        if (word.isEmpty() && !action.equals("addmeaning") && !action.equals("updatemeaning")) {
            resultArea.append("Word field cannot be blank.\n");
            return;
        }

        JSONObject request = new JSONObject();
        request.put("request", action.toLowerCase());
        request.put("word", word);

        switch (action.toLowerCase()) {
            case "add":
                if (meaning.isEmpty()) {
                    resultArea.append("Meaning field cannot be blank for 'add'.\n");
                    return;
                }
                JSONArray meanings = new JSONArray();
                meanings.put(meaning);
                request.put("meanings", meanings);
                break;
            case "addmeaning":
                if (meaning.isEmpty()) {
                    resultArea.append("Meaning field cannot be blank for 'addmeaning'.\n");
                    return;
                }
                request.put("meaning", meaning);
                break;
            case "updatemeaning":
                if (meaning.isEmpty() || existingMeaning.isEmpty()) {
                    resultArea.append("Meaning fields cannot be blank for 'updatemeaning'.\n");
                    return;
                }
                request.put("old", existingMeaning);
                request.put("new", meaning);
                break;
        }

        output.println(request.toString());
        // send the request object which changed into json file to server
        try {
            String response = input.readLine();
            if (response != null) {
                JSONObject jsonResponse = new JSONObject(response);
                resultArea.append("Server: " + jsonResponse.toString(2) + "\n");
            } else {
                resultArea.append("Server closed the connection.\n");
            }
        } catch (IOException e) {
            resultArea.append("Error reading server response: " + e.getMessage() + "\n");
        }

        wordField.setText("");
        meaningField.setText("");
        existingMeaningField.setText("");
        updateCambridgeButton();
    }

    private void disconnect() {
        JSONObject request = new JSONObject();
        request.put("request", "disconnect");
        output.println(request.toString());
        try {
            String response = input.readLine();
            if (response != null) {
                resultArea.append("Server: " + new JSONObject(response).toString(2) + "\n");
            }
        } catch (IOException e) {
            resultArea.append("Error disconnecting: " + e.getMessage() + "\n");
        }

        try {
            socket.close();
        } catch (IOException e) {
            // Ignore
        }

        sendButton.setEnabled(false);
        disconnectButton.setEnabled(false);
        searchCambridgeButton.setEnabled(false);
        resultArea.append("Disconnected from server.\n");
    }
}