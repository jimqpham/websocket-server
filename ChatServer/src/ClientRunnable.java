import java.io.FileNotFoundException;
import java.net.Socket;
import java.util.ArrayList;

public class ClientRunnable implements Runnable {

    private Socket client_;
    private ArrayList<Room> rooms_ = new ArrayList<>();
    private String username_;

    public ClientRunnable(Socket client) {
        client_ = client;
    }

    @Override
    public void run() {

        HttpRequest         request = new HttpRequest(this);
        HttpResponse        response = new HttpResponse(request, this);
        WebSocketResponse   wsObj = new WebSocketResponse(this, request);

        try {
            // If the HTTP request is a WebSocket handshake, accept and switch to WebSocket protocol
            if (request.isWebSocketHandshake()) {
                System.out.println("Web Socket handshake received.");
                wsObj.respondHandshake();
                wsObj.handleWebSocketMessages();
            }
            // If not, respond as a normal HTTP request
            else {
                response.respondHttp();
                client_.close();
            }
        }
        catch (FileNotFoundException fnf) {
            response.respondHttpError("404 Not Found");
            fnf.printStackTrace();
        }
        catch (MyException me) {
            // Handling custom exceptions with custom error messages
            System.out.println( me.getMessage() );
            me.printStackTrace();
        }
        catch (Exception e) {
            response.respondHttpError("500 Internal Server Error");
            e.printStackTrace();
        }
    }

    // Add and remove room(s) that this client is in
    public void addRoom (Room room) { rooms_.add(room); }
    public void removeRoom (Room room) {
        if (rooms_.contains(room)) {rooms_.remove(room);}
    }
    public void removeAllRooms() {
        for (Room room: rooms_) {
            room.removeClient(this);
        }
        rooms_.clear();
    }

    // Some other getters and setters
    public Socket getClientSocket() { return client_; }
    public String getUsername() {return username_;}
    public void setUsername(String username) {username_ = username;}
}
