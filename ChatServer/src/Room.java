import java.io.*;
import java.util.*;

// MESSAGE TO SEND TO CLIENT
// CHAT HISTORY: LOOP THROUGH MESSAGES IN THE CHAT HISTORY
// MEMBER LIST: <type: member-list> <member member member>
// SYSTEM MESSAGES: <type: system-message> <system-message-content>
// USER MESSAGES: <type: user-message> <user-message-sender> <user-message-timestamp> <user-message-content>


public class Room {

    private String name_;
    private ArrayList<ClientRunnable> clients_;
    private static ArrayList<Room> rooms_ = new ArrayList<>();
    private ArrayList<String> chatHistory_ = new ArrayList<>();
    private ArrayList<String> users_ = new ArrayList<>();

    // INSTANTIATE A ROOM WITH NAME AND CLIENT LIST
    public Room (String name) throws IOException {
        name_ = name;
        clients_ = new ArrayList<>();

        // Load the chat history from the permanent roomdata
        File chatHistory = new File ("resources/roomdata/" + name_ + ".txt");
        chatHistory.createNewFile(); // create a new log file if it didn't exist
        Scanner inChatHistory = new Scanner(new FileInputStream(chatHistory));

        while (inChatHistory.hasNextLine()) {
            String jsonMessage = inChatHistory.nextLine();
            if (jsonMessage.strip() != "") {
                chatHistory_.add(jsonMessage);
            }
        }

        // Load the user list from the permanent roomdata
        File userList = new File ("resources/roomdata/" + name_ + "-users.txt");
        userList.createNewFile(); // create a new log file if it didn't exist
        Scanner inUserList = new Scanner(new FileInputStream(userList));

        while (inUserList.hasNextLine()) {
            String user = inUserList.nextLine();
            if (user.strip() != "") {
                users_.add(user);
            }
        }
    }

    // FACTORY FUNCTION
    public synchronized static Room getRoom(String name) throws MyException, IOException {
        // Check if the room name is valid
        if (name.contains(" ") || !name.equals(name.toLowerCase())) {
            System.out.println("room name: '" + name + "'// roomname.lowercase: '" + name.toLowerCase() +"'");
            throw new MyException("Room name invalid.");
        }

        // If room already existed, return that room
        for (Room room: rooms_) {
            if (room.name_.equals(name)) {
                System.out.println("Room named '" + name + "' existed.");
                return room;
            }
        }

        // If not, create and return a new name
        Room newRoom = new Room(name);
        System.out.println("Creating new room named '" + name + "'...");
        rooms_.add(newRoom);
        return newRoom;

    }

    // ADD CLIENT TO A ROOM
    public synchronized void addClient(ClientRunnable curRunnable) throws MyException, IOException {

        String username = curRunnable.getUsername();
        System.out.println("@" + username + " has joined room #" + name_);

        if (!clients_.contains(curRunnable)) {
            // If client/runnable is not in a room, add runnable to the list
            clients_.add(curRunnable);
            curRunnable.addRoom(this);

            // Add the username to the users_ list if it wasn't there and send a new user notification to everyone
            // and send the updated member list to clients to update the sidebar
            if (!users_.contains(username)) {
                users_.add(username);
                storeUserList();
                String jsonSystemMessage = generateJsonSystemMessage("@" + username + " has joined room #" + name_);
                this.storeMessage(jsonSystemMessage);
                this.distributeMessage(jsonSystemMessage);

                String jsonMemberList = this.generateJsonMemberList();
                this.distributeMessage(jsonMemberList);
            }
            // Otherwise, send a message to only that user/client saying that the user is already in the chatroom.
            else {
                String jsonSystemMessage = generateJsonSystemMessage(username + " is already in room #" + this.name_);
                WebSocketResponse.sendMessageToClient(curRunnable, jsonSystemMessage);
            }
        }

        else {
            // If client socket is already in room, throw exception
            throw new MyException("@" + username + " is already in room #" + name_);
        }

    }

    // REMOVE A USER FROM A ROOM
    public synchronized void removeUsername(String userName) throws MyException, IOException {

        // Send a notification (system message) to all clients
        String jsonSystemMessage = generateJsonSystemMessage("@" + userName + " has left room #" + name_);
        this.storeMessage(jsonSystemMessage);
        this.distributeMessage(jsonSystemMessage);

        // Remove username from the username list
        users_.remove(userName);
        storeUserList();
        System.out.println("Remove username @" + userName + " from list of names");

        // Send an updated member list
        String jsonMemberList = generateJsonMemberList();
        this.distributeMessage(jsonMemberList);
    }

    // REMOVE A CLIENT/THREAD FROM A ROOM
    public synchronized void removeClient (ClientRunnable client) {
        client.removeRoom(this);
        clients_.remove(client);
    }

    // STORE MESSAGES IN A VARIABLE
    public synchronized  void storeMessage (String jsonMessage) throws FileNotFoundException {
        chatHistory_.add(jsonMessage);
        PrintWriter out = new PrintWriter(new FileOutputStream("resources/roomdata/" + name_ + ".txt", true));
        out.append(jsonMessage + "\n");
        out.close();
    }

    public synchronized void storeUserList () throws IOException {
        File userFile = new File ("resources/roomdata/" + name_ + "-users.txt");
        userFile.createNewFile();
        PrintWriter out = new PrintWriter(new FileOutputStream(userFile));
        for (String u: users_) {
            out.print(u + "\n");
        }
        out.close();
    }

    // SEND A SYSTEM MESSAGE/USER MESSAGE TO ALL CLIENTS
    // SYSTEM MESSAGE: A NOTIFICATION FROM THE SYSTEM (NEW USER JOINING, SERVER STATUS, ETC.)
    // USER MESSAGE: AN ACTUAL MESSAGE FROM THE END-USER
    public synchronized void distributeMessage (String jsonResponse) throws IOException, MyException {
        System.out.println("Sending to all clients: " + jsonResponse);
        for (ClientRunnable clientRunnable: clients_) {
            WebSocketResponse.sendMessageToClient(clientRunnable, jsonResponse);
        }
    }

    // HANDLE USER MESSAGES
    public synchronized void handleUserMessage(String sender, String timestamp, String content) throws MyException, IOException {
        String jsonUserMessage = generateJsonUserMessage(sender, timestamp, content);
        this.storeMessage(jsonUserMessage);
        this.distributeMessage(jsonUserMessage);
    }

    // HANDLE LOAD ROOM REQUEST
    public void handleLoadRoom(ClientRunnable curRunnable) throws MyException, IOException {
        // Send chat history
        for (String jsonUserMessage: chatHistory_) {
            WebSocketResponse.sendMessageToClient(curRunnable, jsonUserMessage);
        }

        // Send an updated member list
        String jsonMemberList = generateJsonMemberList();
        this.distributeMessage(jsonMemberList);
    }

    // HANDLE MEMBER LIST REQUEST
    public void handleMemberListOnly(ClientRunnable curRunnable_) throws MyException, IOException {
        // Send an updated member list
        String jsonMemberList = generateJsonMemberListOnly();
        WebSocketResponse.sendMessageToClient(curRunnable_, jsonMemberList);
    }

    // FUNCTIONS BELOW TO CREATE JSON TO SEND BACK TO CLIENT

    private String generateJsonSystemMessage(String message) {
        HashMap<String, String> jsonHashMap = new HashMap<>();
        jsonHashMap.put("type", "system-message");
        jsonHashMap.put("system-message-content", message);
        jsonHashMap.put("room", name_);
        return hashmapToJsonString(jsonHashMap);
    }

    private String generateJsonUserMessage(String sender, String timestamp, String content) {
        HashMap<String, String> jsonHashMap = new HashMap<>();
        jsonHashMap.put("type", "user-message");
        jsonHashMap.put("user-message-sender", sender);
        jsonHashMap.put("user-message-timestamp", timestamp);
        jsonHashMap.put("user-message-content", content);
        jsonHashMap.put("room", name_);
        return hashmapToJsonString(jsonHashMap);
    }

    private String generateJsonMemberList () {
        HashMap<String, String> jsonHashMap = new HashMap<>();
        jsonHashMap.put("type", "member-list");
        jsonHashMap.put("member-list", arrayListToJsonString(users_));
        jsonHashMap.put("room", name_);
        return hashmapToJsonString(jsonHashMap);
    }

    private String generateJsonMemberListOnly () {
        HashMap<String, String> jsonHashMap = new HashMap<>();
        jsonHashMap.put("type", "member-list-only");
        jsonHashMap.put("member-list", arrayListToJsonString(users_));
        jsonHashMap.put("room", name_);
        return hashmapToJsonString(jsonHashMap);
    }

    private String hashmapToJsonString(Map<String, String> hashMap) {
        StringBuilder jsonStringBuilder = new StringBuilder("{");

        for (Map.Entry<String, String> entry: hashMap.entrySet()) {
            jsonStringBuilder.append("\"" + entry.getKey() + "\": ");
            if (entry.getValue().startsWith("[")) {
                // Put a list [item, item, etc] in a JSON - no double quotes necessary
                jsonStringBuilder.append(entry.getValue() + ", ");
            }
            else {
                jsonStringBuilder.append("\"" + entry.getValue() + "\", ");
            }
        }

        // Remove the trailing comma
        jsonStringBuilder.deleteCharAt(jsonStringBuilder.lastIndexOf(", "));
        // Append the ending curly brace
        jsonStringBuilder.append("}");
        return jsonStringBuilder.toString();
    }

    private String arrayListToJsonString(ArrayList<String> arrList) {
        StringBuilder jsonStringBuilder = new StringBuilder("[");
        for (String item: arrList) {
            jsonStringBuilder.append("\"" + item + "\", ");
        }
        // Remove the trailing comma
        if (jsonStringBuilder.lastIndexOf(",") != -1) {
            jsonStringBuilder.deleteCharAt(jsonStringBuilder.lastIndexOf(","));
        }
        // Append the ending square bracket
        jsonStringBuilder.append("]");
        return jsonStringBuilder.toString();
    }
}
