public class MyException extends Exception {
    // Thrown when messages from client to server is unmasked
    public MyException (String s) {
        super(s);
    }

}
