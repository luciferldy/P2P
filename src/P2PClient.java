import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.text.BreakIterator;
import java.util.Arrays;

public class P2PClient {
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private BufferedReader line;
	private boolean connect_success = true;
	private boolean open_server = true;
	
	private FileReceive file_receive;
	private String nickName;
	
	public static void main(String[] args){
		new P2PClient();
	}
	
	public P2PClient(){
		try {
			socket = new Socket("127.0.0.1", P2PServer.SERVER_PORT);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			line = new BufferedReader(new InputStreamReader(System.in));
			String tell;
			String msg;
			
			msg = in.readLine(); // first word received must be welcome word
			if (msg.equals(P2PServer.WelCome_Word)) {
				System.out.println(msg);
				InetAddress addr = InetAddress.getLocalHost();
				String ip = addr.getHostAddress().toString();
				while (true) {
					tell = line.readLine();
					String[] infor = tell.split("#");
					if (infor.length == 2 && Integer.parseInt(infor[1]) > 0 && Integer.parseInt(infor[1]) < 65535) {
						out.println(infor[0]+"#"+ip+"#"+infor[1]);
						nickName = infor[0];
						System.out.println(in.readLine());
						file_receive = new FileReceive(Integer.parseInt(infor[1])); // open the file receiver
						break;
					} else {
						System.out.println("Illegal input, input your nickname#port again");
					}
				}
			} else {
				// first receive is not welcome, disconnect
				connect_success = false;
				out.println("exit");
				return;
			}
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					while (open_server) {
						char[] temp = new char[1024];
						int count = 0;
						try {
							count = in.read(temp);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
						if (count == -1) {
							break;
						}
						String response = String.valueOf(temp, 0, count);
						System.out.print(response); // read the byde can read the /n, so no need println any more
					}
					
				}
			}).start();
		
			while (connect_success){
				
				tell = line.readLine();
				if (tell.equals("exit")){
					out.println(tell);
					file_receive.releaseServer();
					break;
				}
				else if (tell.equals("send file")) {
					sendFile();
					continue;
				}
				else {
					out.println(tell);
					
				}
			}
			line.close();
			out.close();
			in.close();
			socket.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public void sendFile(){
		try {
			File file;
			// get the file
			while (true) {
				System.out.println("input the file path");
				String path = line.readLine();
				file = new File(path);
				if (file.exists()) {
					break;
				}else {
					System.out.println("Illegal Input");
				}
			}
			// get the ip and port
			String ip;
			String port;
			while (true) {
				System.out.println("input ip address and port");
				String[] infor = line.readLine().split("#");
				if ( infor.length == 2 && Integer.parseInt(infor[1]) > 0 && Integer.parseInt(infor[1]) < 65535) {
					ip = infor[0];
					port = infor[1];
					break;
				} else {
					System.out.println("Illegal input");
				}
			}
			
			
			Socket sendFileSocket = new Socket(InetAddress.getByName(ip), Integer.parseInt(port));
			ObjectOutputStream oos = new ObjectOutputStream(sendFileSocket.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(sendFileSocket.getInputStream()));
			FileInputStream fis = new FileInputStream(file);
			
			// send file name
			byte[] b = file.getName().getBytes();
			oos.write(b);
			oos.flush();			
			System.out.println("sending...");
			byte[] buf = new byte[1024];
			int len = fis.read(buf);
			while (len > 0) {
				oos.write(buf, 0, len);
				oos.flush();
				Thread.sleep(50);
				len = fis.read(buf);
			}
			System.out.println("sended");
			
			
			br.close();
			fis.close();
			oos.close();
			sendFileSocket.close();
		} catch (UnknownHostException e){
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	

	class FileReceive extends Thread{
		private ServerSocket ss;
		private Socket s;
		private int port;
		
		public FileReceive(int port){
			this.port = port;
			start();
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				String fileName = "";
				ss = new ServerSocket(port);
				while (open_server) {
					s = ss.accept();
					System.out.println("someone attemp to send file");
					ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
					byte[] buf = new byte[1024];
					int len;
			
					// get the file name
					if ((len = ois.read(buf)) != -1) {
						fileName = new String(buf, 0, len);
						String directoryName = nickName+"-receive";
						File directoryFile = new File(directoryName);
						if (!directoryFile.exists()) {
							directoryFile.mkdir();
						}						
						FileOutputStream fos = new FileOutputStream(directoryFile+"\\"+fileName);
						System.out.println("receiving...");
						while ( (len = ois.read(buf)) != -1) {
							fos.write(buf, 0, len);
							fos.flush();
						}
						System.out.println("received");
						fos.close();
						
					}else {
						System.out.println("can not get the file name");
					}
					ois.close();
					s.close();
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("something wrong with the server thread");
				e.printStackTrace();
			}
		}
		
		public void releaseServer(){
			try {
				ss.close();
				open_server = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
