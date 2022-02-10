import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HttpRequest {

    ClientRunnable curRunnable_;
    String fileName_, command_, protocol_;
    Map<String, String> reqHeaders = new HashMap<>();
    boolean doneReading = false;

    public HttpRequest(ClientRunnable curRunnable) {
        curRunnable_ = curRunnable;
    }

    // READING THE HTTP REQUEST
    public void readRequest() throws IOException {

        Socket client = curRunnable_.getClientSocket();
        InputStream in  = client.getInputStream();
        Scanner scanner = new Scanner(in);

        // Parsing HTTP requests
        command_  = scanner.next();
        fileName_ = scanner.next();
        if (fileName_.equals("/")) {fileName_ = "resources/index.html";} else {fileName_ = "resources" + fileName_;}
        protocol_ = scanner.next();
        scanner.nextLine();

        // Store other headers in a map
        while (scanner.hasNextLine() ) {
            String line = scanner.nextLine();
            if (line.strip().equals("")) { break; }
            String[] valuePair = line.split(":", 2);
            if (valuePair.length == 2) {
                reqHeaders.put(valuePair[0].strip(), valuePair[1].strip());
            }
        }

        doneReading = true;
    }

    // CHECK IF IT'S A WEBSOCKET HANDSHAKE
    public boolean isWebSocketHandshake () throws IOException {
        if (!doneReading) { readRequest(); }

        return (reqHeaders.get("Connection") != null &&
                reqHeaders.get("Sec-WebSocket-Key") != null &&
                reqHeaders.get("Connection").equals("Upgrade"));
    }

    // FILE NAME GETTER
    public String getFileName() throws IOException {
        if (!doneReading) { readRequest(); }
        return fileName_;
    }

    // HTTP HEADERS GETTER - FOR WEBSOCKET HANDSHAKE ACCEPTANCE
    public Map<String, String> getHeaders() throws IOException {
        if (!doneReading) { readRequest(); }
        return reqHeaders;
    }
}
