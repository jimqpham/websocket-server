import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class WebSocketResponse {
    
    private ClientRunnable curRunnable_;
    private HttpRequest request_;

    public WebSocketResponse(ClientRunnable curRunnable, HttpRequest request) {
        curRunnable_ = curRunnable;
        request_ = request;
    }

    /*********************************************************
     *************** HANDLE WEBSOCKET REQUESTS ***************
     *********************************************************/

    // RESPOND HANDSHAKE
    public void respondHandshake() throws IOException, NoSuchAlgorithmException, MyException {

        // IF THE REQUEST IS A WEBSOCKET HTTP HANDSHAKE
        // START HANDLING THE HANDSHAKE
        Socket client = curRunnable_.getClientSocket();
        OutputStream out = client.getOutputStream();
        PrintWriter pw = new PrintWriter(out);

        pw.print( "HTTP/1.1 101 Switching Protocols\r\n" );
        pw.print( "Upgrade: websocket\r\n" );
        pw.print( "Connection: Upgrade\r\n" );
        pw.print( "Sec-WebSocket-Accept: " + generateAcceptKey() + "\r\n" );
        pw.print( "\r\n");

        pw.flush();
        out.flush();

        System.out.println("Done handshake.");
    }

    // GENERATE THE KEY AND RETURN IT TO CLIENT TO ACCEPT HANDSHAKE
    private String generateAcceptKey() throws IOException, NoSuchAlgorithmException {
        String firstPart = request_.getHeaders().get("Sec-WebSocket-Key");
        String secondPart = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        String acceptKey = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-1").digest((firstPart + secondPart).getBytes()));
        return acceptKey;
    }

    // AFTER HANDSHAKE, START ENCODING/DECODING MESSAGES USING WEBSOCKET PROTOCOL
    // DECODE THE BYTE STREAM FROM CLIENT AND GET THE MESSAGE
    private String decodeClientMessage () throws IOException, MyException {

        System.out.println("Start decoding client message");

        Socket client = curRunnable_.getClientSocket();
        InputStream in = client.getInputStream();
        DataInputStream dis = new DataInputStream(in);

        // Read the 1st byte including opCode
        byte b1 = dis.readByte();
        byte opCode = (byte) (b1 & 0x0F);
        // Check the opCode value
        if (opCode == 1) {
            System.out.println("Text message coming...");
        } else if (opCode == 8) {
            closeConnection();
        }

        System.out.println("Checkpoint 1");

        // Read the 2nd byte and check if the message is masked
        // If not, throw an exception
        byte b2 = dis.readByte();
        boolean masked = ((b2 & 0x80) != 0);
        if (!masked) {
            throw new MyException("Messages from client to server should be masked");
        }

        // Read the length of the message in subsequent bytes
        long length = b2 & 0x7F;
        if (length == 126) {
            length = dis.readUnsignedShort();
        } else if (length == 127) {
            length = dis.readLong();
        }
        System.out.println("Message Length: " + length);

        // Read the mask, the remaining bytes and decode
        byte[] mask = dis.readNBytes(4);
        byte[] encoded = dis.readNBytes((int) length);
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < encoded.length; i++) {
            char newChar = (char) (encoded[i] ^ mask[i % 4]);
            decoded.append(newChar);
        }

        // Store the message in the member variable and print it out to the screen
        String decodedMessage = decoded.toString();
        System.out.println("Message Content: " + decodedMessage);
        return decodedMessage;
    }

    // SEND MESSAGE BACK TO CLIENT
    public static void sendMessageToClient (ClientRunnable clientRunnable, String jsonMessage) throws IOException, MyException {

        Socket client = clientRunnable.getClientSocket();
        OutputStream out = client.getOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        // Write the first byte
        dos.writeByte(0x81); // 0x1000 0001 means FIN bit = 1 and opCode bit = 1

        // Write the 2nd byte and subsequent bytes containing payload length info
        if (jsonMessage.length() < 126) {
            dos.writeByte(jsonMessage.length());
        }
        else if (jsonMessage.length() <= (Short.MAX_VALUE - Short.MIN_VALUE)) {
            // Because there is no writeUnsignedShort method, we do it manually
            dos.writeByte(126);
            byte b3 = (byte) (0xFF & (jsonMessage.length() >> 8));
            byte b4 = (byte) (0xFF & (jsonMessage.length()));
            dos.writeByte(b3);
            dos.writeByte(b4);
        }
        else if (jsonMessage.length() <= Long.MAX_VALUE) {
            dos.writeByte(127);
            dos.writeLong(jsonMessage.length());
        } else {
            throw new MyException("The message is either too long or invalid.");
        }

        // Write the message content
        dos.write(jsonMessage.getBytes(StandardCharsets.UTF_8));

        dos.flush();

    }

    // HANDLING WHEN THE OPCODE IS 8
    private void closeConnection() {
        System.out.println("Client about to close connection/refresh the page");
        curRunnable_.removeAllRooms();
    }

    // HANDLING REQUESTS FROM CLIENT
    public void handleWebSocketMessages () throws MyException, IOException {

        while (true) {
            String decodedMessage = decodeClientMessage();
            System.out.println(decodedMessage);

            // HANDLE SET USERNAME
            if (decodedMessage.startsWith("set-username")) {
                String username = decodedMessage.split(" ", 2)[1];
                curRunnable_.setUsername(username);
            }

            // HANDLE JOIN ROOM
            if (decodedMessage.startsWith("request-join")) {
                String[] infos = decodedMessage.split(" ", 3);
                String roomname = infos[1];
                Room theRoom = Room.getRoom(roomname);
                theRoom.addClient(curRunnable_);
            }

            // HANDLE EXIT ROOM
            if (decodedMessage.startsWith("request-exit")) {
                String[] infos = decodedMessage.split(" ", 3);
                String roomname = infos[1];
                Room theRoom = Room.getRoom(roomname);
                theRoom.removeClient(curRunnable_);
                theRoom.removeUsername(curRunnable_.getUsername());
            }

            // DISTRIBUTE NORMAL NON-SYSTEM MESSAGES FROM USERS
            if (decodedMessage.startsWith("user-message")) {
                String[] infos = decodedMessage.split(" ", 5);
                String roomname = infos[1];
                String sender = infos[2];
                long enochMiliSeconds = Long.valueOf(infos[3]);
                LocalTime sentTime = Instant.ofEpochMilli(enochMiliSeconds).atZone(ZoneId.systemDefault()).toLocalTime();
                LocalDate sentDate = Instant.ofEpochMilli(enochMiliSeconds).atZone(ZoneId.systemDefault()).toLocalDate();
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                String timestamp = dateFormatter.format(sentDate) + ", " + timeFormatter.format(sentTime);
                System.out.println(timestamp);
                String content = infos[4];

                Room theRoom = Room.getRoom(roomname);
                theRoom.handleUserMessage(sender, timestamp, content);
            }

            // HANDLE REQUEST TO LOAD ROOM DATA
            if (decodedMessage.startsWith("request-room-load")) {
                String roomname = decodedMessage.split(" ", 2)[1];
                Room theRoom = Room.getRoom(roomname);
                theRoom.handleLoadRoom(curRunnable_);
            }

            // HANDLE REQUEST FOR MEMBER LIST
            if (decodedMessage.startsWith("request-member-list")) {
                String roomname = decodedMessage.split(" ", 2)[1];
                Room theRoom = Room.getRoom(roomname);
                theRoom.handleMemberListOnly(curRunnable_);
            }
        }
    }
}
