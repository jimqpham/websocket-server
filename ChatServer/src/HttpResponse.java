import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HttpResponse {

    /*********************************************************
     *************** HANDLE NORMAL HTTP REQUESTS **************
     *********************************************************/

    private HttpRequest request_;
    private ClientRunnable curRunnable_;

    public HttpResponse(HttpRequest request, ClientRunnable curRunnable) {
        request_ = request;
        curRunnable_ = curRunnable;
    }

    // RETURN NON-HANDSHAKE NORMAL HTTP HEADERS AND FILES
    public void respondHttp() throws IOException {

        Socket client = curRunnable_.getClientSocket();
        OutputStream out = client.getOutputStream();
        PrintWriter pw = new PrintWriter(out);

        // Return regular HTTP headers
        pw.println("HTTP/1.1 200 OK");
        pw.println("Date: " + getDate());
        pw.println();
        pw.flush();

        // Pass the file
        String fileName = request_.getFileName();
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        fis.transferTo(out);

        out.flush();
        out.close();
    }

    // HANDLING EXCEPTIONS/ERRORS - RETURN ERROR STATUS AND MESSAGE
    public void respondHttpError(String errorCode) {
        // This is in the catch block in the main code
        // Exceptions should be handled right away and not bubbled up

        try {
            Socket client = curRunnable_.getClientSocket();
            OutputStream out = client.getOutputStream();
            PrintWriter pw = new PrintWriter(out, true);

            // Return the error status, headers and error message to client
            pw.println("HTTP/1.1 " + errorCode);
            pw.println("Date: " + getDate());
            pw.println();
            pw.println(errorCode);

            out.flush();
            out.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // GET THE SYSTEM DATE TO INCLUDE IN HTTP HEADER
    private String getDate() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

}
