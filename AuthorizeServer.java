import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Properties;

public class AuthorizeServer {
    private DatagramSocket socketServer;
    DatagramPacket packet;
    private byte[] bufRec = new byte[256];
    private byte[] bufSend = new byte[256];
    private InetAddress address;

    private String authorizeServerPort;
    private String dataServerPort;
    private String secretKey;
    private ArrayList<String> username = new ArrayList<String>();
    private ArrayList<String> password = new ArrayList<String>();
    private ArrayList<String> action = new ArrayList<String>();

    private void readFile() throws IOException {
        //เปิดไฟล์ server.config
		Properties prop = new Properties();
        String fileName = "server.config";
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
        } catch (FileNotFoundException ex) {
            System.out.println(ex);
        }
        prop.load(is);
        authorizeServerPort = prop.getProperty("authorize_server_port");
        dataServerPort = prop.getProperty("data_server_port");
        secretKey = prop.getProperty("secret_key");

        //เปิดไฟล์ user_pass_action.csv
        try {
            BufferedReader csvReader = new BufferedReader(new FileReader("user_pass_action.csv"));
            String row = "";
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(",");
                username.add(data[0]);
                password.add(data[1]);
                action.add(data[2]);
            }
            username.remove(0);
            password.remove(0);
            action.remove(0);
        } catch (FileNotFoundException ex) {
            System.out.println(ex);
        }
    }

    public void runServer() throws IOException {
        readFile();
        socketServer = new DatagramSocket(Integer.valueOf(authorizeServerPort));
        while(true) {
            packet = new DatagramPacket(bufRec, bufRec.length);
            socketServer.receive(packet);
            String messageFromDataServer = new String(packet.getData(), 0, packet.getLength());
            System.out.println("message : " + messageFromDataServer);
            address = packet.getAddress();
            int port = packet.getPort();
                
            String []CHECK = messageFromDataServer.split(":");
            String token = CHECK[0];
            String act = CHECK[1];
            byte[] decodedBytes =  Base64.getDecoder().decode(token);
            String TOKEN = new String(decodedBytes);
            String data[] = TOKEN.split("\\.");
            String user = data[0];
            String pass = data[1];

            // check user,pass,action
            String Ability = "";
            for(int i=0;i<username.size();i++) {
                if(user.equals(username.get(i))&&pass.equals(password.get(i))) {
                    String ACT[] = action.get(i).split(":"); // have action
                    for(int j=0;j<ACT.length;j++) {
                        if(act.equals(ACT[j])) {  //true
                            System.out.println("USER : " + user);
                            System.out.println("Status : true");
                            System.out.println();
                            Ability = act+":"+"true";
                            break;
                        }
                        else {
                            System.out.println("USER : " + user);
                            System.out.println("Status : false");
                            System.out.println();
                            Ability = act+":"+"false";
                        }
                    }
                    break;
                } 
            }
            bufSend = Ability.getBytes();
            packet = new DatagramPacket(bufSend, bufSend.length, address, port);
            socketServer.send(packet);
        }
    }

    public static void main(String[] args) throws IOException {
        new AuthorizeServer().runServer();
    }
}
