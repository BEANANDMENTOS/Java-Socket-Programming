import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;

public class DataServer {
    private	ServerSocket serverSocket;
    private DatagramSocket datagramSocket;
    private Socket dataSocket;
    private PrintWriter output;
	private BufferedReader input;
    private InetAddress address;
    private DatagramPacket packet;
    private byte[] bufRec = new byte[256];
    private byte[] bufSend = new byte[256];

    private String authenticationServerPort;
    private String authorizeServerPort;
    private String dataServerPort;
    private String secretKey;
    private ArrayList<String> name = new ArrayList<String>();
    private ArrayList<String> ip = new ArrayList<String>();

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
        authenticationServerPort = prop.getProperty("authentication_server_port");
        authorizeServerPort = prop.getProperty("authorize_server_port");
        dataServerPort = prop.getProperty("data_server_port");
        secretKey = prop.getProperty("secret_key");

        //เปิดไฟล์ user_pass_action.csv
        try {
            BufferedReader csvReader = new BufferedReader(new FileReader("data_list.csv"));
            String row = "";
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(",");
                name.add(data[0]);
                ip.add(data[1]);
            }
            name.remove(0);
            ip.remove(0);
        } catch (FileNotFoundException ex) {
            System.out.println(ex);
        }
    }

    public void runServer() throws IOException {
        readFile();
        serverSocket = new ServerSocket(Integer.valueOf(dataServerPort), 1);
        datagramSocket = new DatagramSocket();
        address = InetAddress.getByName("127.0.0.1");

        String token;
        String action;
        String mapping;

        while(true) {
            Socket connectionSocket = serverSocket.accept();
            output = new PrintWriter(connectionSocket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

            while(true) {
                String messFromClient = input.readLine();
                String[] data = messFromClient.split(":");
                if(data.length != 3) {   // ออกเมื่อไม่เป็นไปตามฟอร์แมต
                    connectionSocket.close();
                    break;
                }
                token = data[0];
                action = data[1];
                mapping = data[2];
                if(action.equals("quit")) {  //เมื่อ action เป็น quit
                    System.out.println("Disconnect");
                    connectionSocket.close();
                    break;
                }
                else if(action.equals("nametoip") || action.equals("iptoname")) ;

                else { //กรณีอื่นๆ ตัดการเชื่อมต่อกับ ไคลแอนท์
                    System.out.println("Disconnect");
                    connectionSocket.close();
                    break;
                }
                //ส่ง token : action ไปที่ Authorize Serve
                String messageToAuthorizeServe = token + ":" + action;
                bufSend = messageToAuthorizeServe.getBytes();
                packet = new DatagramPacket(bufSend, bufSend.length, address, Integer.valueOf(authorizeServerPort));
                datagramSocket.send(packet);
                
                //เช็คว่า true หรือ false
                packet = new DatagramPacket(bufRec, bufRec.length);
                datagramSocket.receive(packet);
                String statusFromAuthorizeServe = new String(packet.getData(), 0, packet.getLength());

                String checkTF[] = statusFromAuthorizeServe.split(":");
                if(checkTF[1].equals("false")) {
                    serverSocket.close();
                    break;
                }
                else {
                    String result = "";
                    String NFH = "n";  //Not Found huh?  default --> "n" 
                    if(action.equals("nametoip")) {
                        for(int i=0;i<name.size();i++) {
                            if(mapping.equals(name.get(i))) {
                                NFH = "y";
                                result = ip.get(i);
                                output.println(result);
                            }
                        }
                        if(NFH == "n")
                            output.println("not found");
                    }
                    else if(action.equals("iptoname")) {
                        for(int i=0;i<ip.size();i++) {
                            if(mapping.equals(ip.get(i))) {
                                NFH = "y";
                                result = name.get(i);
                                output.println(result);
                            }
                        }
                        if(NFH == "n")
                            output.println("not found");
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        DataServer data_Server = new DataServer();
        data_Server.runServer();
    }
}