import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 5001);
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        String idMessage = "8=Fix 4.2\n35=identification\n9=44\n49=nasdaq\n10=44444\n";
        outputStream.write(idMessage.getBytes());
        outputStream.flush();

        while (true)
        {
            byte[] buffer = new byte[1000];
            int re = inputStream.read(buffer);
            if (re > 0) {
                System.out.println("Bytes read: " + re);
                String str1 = new String(buffer, 0, re);
                System.out.println(str1);
                String response = "8=Fix 4.2\n35=Confirmed\n10=44444\n";
                outputStream.write(response.getBytes());
                outputStream.flush();
            }
        }
    }
}
