import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

    private static ServerSocket server_;
    private static Socket client_;

    public static void main(String[] args) {

        // Create a new server socket waiting for clients
        try {
            server_ = new ServerSocket(8080);
            System.out.println("Server waiting for client...");
        } catch (IOException e) {
            System.out.println("Server socket error.");
            e.printStackTrace();
        }

        // Accepting a new client
        while (true) {
            try {
                client_ = server_.accept();
                System.out.println("A client connected.");
            } catch (IOException e) {
                System.out.println("Client socket error.");
                e.printStackTrace();
            }

        // Assign that client to a new thread
        Thread t = new Thread(new ClientRunnable(client_));
        t.start();

        }
    }
}
