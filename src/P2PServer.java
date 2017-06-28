import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngineResult.HandshakeStatus;

public class P2PServer {
	private ServerSocket ss;
	private Socket socket;
	public static final int SERVER_PORT = 10001;
	public static final String IP = "IP";
	public static final String PORT = "PORT";
	public static final String NICKNAME = "NAME";
	public static final String WelCome_Word = "Welcome, please input your nickname#port";
	
	private ArrayList<HashMap<String, String>> User_List = new ArrayList<>();
	private ArrayList<CreateServerThread> User_Thread = new ArrayList<>();
	
	public static void main(String[] args){
		new P2PServer();
	}
	
	public P2PServer(){
		try{
			ss = new ServerSocket(SERVER_PORT);
			System.out.println("server start");
			while (true) {
				socket = ss.accept();
				new CreateServerThread(socket);				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	// handle the multi-clients
	class CreateServerThread extends Thread{
		private Socket client;
		private BufferedReader in;
		private PrintWriter out;
		private HashMap<String, String> indentifer;
		private boolean connect_success = true;
		
		public CreateServerThread(Socket s){
			this.client = s;
			start();
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out = new PrintWriter(client.getOutputStream(), true);
				out.println(WelCome_Word);
				
				String line;
				line = in.readLine();
				String[] infor = line.split("#");
				if (infor.length == 3 && addToList(infor)) {
					
					// broadcast and add to Thread List
					broadcast(infor[0] + "  " + infor[1] + "  " + infor[2] + "  come to line");
					User_Thread.add(this);
					out.println("you has loged in successfully");
				} else {
					System.out.println("some one try to connect bu refused");
					out.println("your request is rejected");
					connect_success = false;
				}
				
				while (connect_success){
					line = in.readLine();
					if (line.equals("exit")){
						removeFromList();
						broadcast(infor[0] + "  " + infor[1] + "  " + infor[2] + "  off line");
						User_Thread.remove(this);
						break;
					}
					else if (line.equals("ls")) {
						out.println(listAllUsers());
					}
					else {
						out.println("input \"ls\" to list online or \"exit\" to exit or \"send file\" to send file");
					}
				}
				out.close();
				in.close();
				client.close();
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}				
		}
		
		public boolean addToList(String[] infor){
			indentifer = new HashMap<>();
			indentifer.put(NICKNAME, infor[0]);
			indentifer.put(IP, infor[1]);
			indentifer.put(PORT, infor[2]);
			if (User_List.contains(indentifer)) {
				return false;
			} else {
				User_List.add(indentifer);
				System.out.println(infor[0] + "  " + infor[1] + "  " + infor[2] + "  connect successfully");
				return true;
			}
		}
		
		public void removeFromList(){
			if (indentifer != null) {
				System.out.println(indentifer.get(NICKNAME) + "  " + indentifer.get(IP) + "  " + indentifer.get(PORT) + "  disconnect successfully");
				User_List.remove(indentifer);
			}
		}
		
		public void sendMessage(String msg){
			out.println(msg);
		}
		public String listAllUsers(){
			String s = "-- Online list --\n";
			HashMap<String, String> infor_map;
			for (int i = 0; i < User_List.size(); i++) {
				infor_map = User_List.get(i);
				s += infor_map.get(NICKNAME) + "  ";
				s += infor_map.get(IP) + "  ";
				s += infor_map.get(PORT) + "\n";
			}
			s += "-----------------";
			return s;
		}
		public void broadcast(String msg){
			for (int i = 0; i < User_Thread.size(); i++) {
				if (User_Thread.get(i)!=this) {
					User_Thread.get(i).sendMessage(msg);
				}
			}
		}
		
		
	}
}