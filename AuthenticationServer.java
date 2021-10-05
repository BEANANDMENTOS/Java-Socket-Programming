import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Base64;


public class AuthenticationServer {
    private final int MAX_TIME_LOGIN = 3;
	private int loginTime = 0;
    private	ServerSocket serverSocket;
	// private Socket socket;
    private PrintWriter output;
	private BufferedReader input;

    private String authenticationServerPort;
    private String dataServerPort;
    private String secretKey;
    private ArrayList<String> username = new ArrayList<String>();
    private ArrayList<String> password = new ArrayList<String>();
    
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
            }
        } catch (FileNotFoundException ex) {
            System.out.println(ex);
        }
    }

    private String checkUserPass(String userName, String aPassword) {
        for(int i=0;i<username.size();i++) {
            if(userName.equals(username.get(i))) {
                if(aPassword.equals(password.get(i))) {
                    String info = userName + "." + aPassword + "." + secretKey;
                    String token = Base64.getEncoder().encodeToString(info.getBytes());
                    return token;
                }
            }
        }
        return null;
    }

    public void runServer() throws IOException {
        readFile(); //Read file server.config and user_pass_action.csv
        serverSocket = new ServerSocket(Integer.valueOf(authenticationServerPort));

        while(true) {
            Socket connectionSocket = serverSocket.accept(); //เชื่อมต่อ Client
            output = new PrintWriter(connectionSocket.getOutputStream(), true);
		    input  = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            String username = input.readLine();
            String password = input.readLine();
            String Token = checkUserPass(username.substring(5),password.substring(5)); //return เป็น token Base64
            if(Token == null) {
                loginTime++;
                output.println(Token = "");
                if(loginTime == MAX_TIME_LOGIN) {  //ถ้าครบ 3 รอบ socket ปิด
                    System.out.println("Please try again...");
                    connectionSocket.close();
                }
                continue;
            }
            else {
                System.out.println(username);
                System.out.println(password);
                System.out.println("TOKEN" + " : " + Token);
                System.out.println();
                output.println(Token);
                connectionSocket.close();
            }
        }
	}

    public static void main(String[] args) throws IOException {
        AuthenticationServer tcpServer = new AuthenticationServer();
		tcpServer.runServer();
    }
}