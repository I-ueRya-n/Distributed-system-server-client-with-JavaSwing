package DictionaryServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ServerSocketFactory;
import org.json.*;

public class Server {
    // Pre-set necessary port number for connection and listening
    private static final int PORT_NUMBER = 9092;
    // Using ConcurrentHashMap for looking up the word in dictionary >>> save time complexity
    private static Map<String, Set<String>> dictionary = new ConcurrentHashMap<>();
    // Deal with jason file and make it json-like
    private static String dictionaryFile = "dictionary.json";  // Default dictionary file


    public static void main(String[] args) {
        int port = PORT_NUMBER;

        // Load dictionary file from command args
        if(args.length > 0){
            try{
                port = Integer.parseInt(args[0].trim());
                System.out.println(port);
                if(args.length > 1){
                    dictionaryFile = args[1];
                }
            } catch (NumberFormatException e){
                System.err.println("Invalid port number, using PORT_NUMBER"+ PORT_NUMBER);
            }
        }


        loadDictionary();

        // Ensuring when the connection shut down, the content would be saved in the dictionary automatically
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveDictionary();
            System.out.println("Dictionary saved. Server shutting down.");
        }));

        // Multithread holding
        // Create a scalable pool to handle incoming client connection.
        ExecutorService pool = Executors.newCachedThreadPool();
        // Socket for listening specific port, and pass to handleClient method once connection established
        try (ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket client = server.accept();
                pool.execute(() -> handleClient(client));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Tackle the action from client requriements
    private static void handleClient(Socket client) {
        // Handle the communication between server and client
        try (BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter output = new PrintWriter(client.getOutputStream(), true)) {

            // Transfer the format of the content from client side into json
            String line;
            while ((line = input.readLine()) != null) {
                JSONObject request = new JSONObject(line);
                String action = request.optString("request");
                String word = request.optString("word");
                String response;

                // Declare variables outside the switch
                String newMeaning = null;
                String oldMeaning = null;

                switch (action.toLowerCase()) {
                    case "query":
                        response = queryWord(word);
                        break;
                    case "add":
                        response = addWord(word, request.optJSONArray("meanings"));
                        break;
                    case "remove":
                        response = removeWord(word);
                        break;
                    case "addmeaning":
                        newMeaning = request.optString("meaning");
                        response = addMeaning(word, newMeaning);
                        break;
                    case "updatemeaning":
                        oldMeaning = request.optString("old");
                        newMeaning = request.optString("new");
                        response = updateMeaning(word, oldMeaning, newMeaning);
                        break;
                    case "disconnect":
                        output.println("{\"status\":\"success\",\"message\":\"Disconnected.\"}");
                        return;
                    default:
                        response = "{\"status\":\"error\",\"message\":\"Invalid request type.\"}";
                }

                output.println(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            try{
                client.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    // Handle the add meaning logic
    private static String addMeaning(String word, String newMeaning) {
        word = word.toLowerCase();
        if (!dictionary.containsKey(word)) {
            return new JSONObject().put("word", word).put("status", "error").put("message", "Word not found.").toString();
        }

        // Check if the word already existed or not
        Set<String> meanings = dictionary.get(word);
        if (meanings.contains(newMeaning)) {
            return new JSONObject().put("word", word).put("status", "error").put("message", "Meaning already exists.").toString();
        }

        meanings.add(newMeaning);
        saveDictionary();
        return new JSONObject().put("word", word).put("status", "success").put("message", "Meaning added.").toString();
    }

    // Handle the logic for updating meaning
    private static String updateMeaning(String word, String oldMeaning, String newMeaning) {
        word = word.toLowerCase();
        if (!dictionary.containsKey(word)) {
            return new JSONObject().put("word", word).put("status", "error").put("message", "Word not found.").toString();
        }

        // Check if the meaning of the word already existed or not
        Set<String> meanings = dictionary.get(word);
        if (!meanings.contains(oldMeaning)) {
            return new JSONObject().put("word", word).put("status", "error").put("message", "Old meaning not found.").toString();
        }
        // If existed, then remove it and replace it with new meaning
        meanings.remove(oldMeaning);
        meanings.add(newMeaning);
        saveDictionary();
        return new JSONObject().put("word", word).put("status", "success").put("message", "Meaning updated.").toString();
    }

    // Find if the word is already in dictionary or not
    private static String queryWord(String word) {
        word = word.toLowerCase();
        if (dictionary.containsKey(word)) {
            JSONArray meanings = new JSONArray(dictionary.get(word));
            return new JSONObject().put("word", word).put("status", "success").put("meanings", meanings).toString();
        } else {
            return new JSONObject().put("word", word).put("status", "error").put("message", "Word not found.").toString();
        }
    }

    private static String addWord(String word, JSONArray meaningsArray) {
        word = word.toLowerCase();
        if (word == null || word.isEmpty() || !word.matches("[a-zA-Z]+")) {
            return new JSONObject().put("status", "error").put("message", "Invalid word. Only alphabetic characters are allowed.").toString();
        }

        if (meaningsArray == null || meaningsArray.length() == 0) {
            return new JSONObject().put("word", word).put("status", "error").put("message", "Meanings cannot be empty.").toString();
        }

        Set<String> meaningsSet = new HashSet<>();
        for (int i = 0; i < meaningsArray.length(); i++) {
            String meaning = meaningsArray.optString(i);
            if (meaning != null && !meaning.trim().isEmpty()) {
                meaningsSet.add(meaning.trim());
            }
        }

        if (meaningsSet.isEmpty()) {
            return new JSONObject().put("word", word).put("status", "error").put("message", "Meanings cannot be empty.").toString();
        }

        if (dictionary.containsKey(word)) {
            return new JSONObject().put("word", word).put("status", "error").put("message", "Duplicate word.").toString();
        }

        dictionary.put(word, meaningsSet);
        saveDictionary();
        return new JSONObject().put("word", word).put("status", "success").put("message", "Word added.").toString();
    }

    private static String removeWord(String word) {
        word = word.toLowerCase();
        if (dictionary.remove(word) != null) {
            saveDictionary();
            return new JSONObject().put("word", word).put("status", "success").put("message", "Word removed.").toString();
        } else {
            return new JSONObject().put("word", word).put("status", "error").put("message", "Word not found.").toString();
        }
    }

    // Loading the dictionary everytime when the client starts the connection
    private static void loadDictionary() {
        File file = new File(dictionaryFile);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            JSONObject json = new JSONObject(sb.toString());
            for (String key : json.keySet()) {
                JSONArray meaningsArray = json.getJSONArray(key);
                Set<String> meanings = ConcurrentHashMap.newKeySet();
                for (int i = 0; i < meaningsArray.length(); i++) {
                    meanings.add(meaningsArray.getString(i));
                }
                dictionary.put(key, meanings);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Transfer the word into json style when saving to dictionary
    private static synchronized void saveDictionary() {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, Set<String>> entry : dictionary.entrySet()) {
            json.put(entry.getKey(), new JSONArray(entry.getValue()));
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(dictionaryFile))) {
            writer.println(json.toString(2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
