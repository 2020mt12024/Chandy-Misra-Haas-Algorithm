import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;
import java.lang.*;
 
public class StartProcess {              
    public static void main(String[] args) throws InterruptedException {
		Process proc = new Process(args[0], Integer.parseInt(args[1]));
        proc.readyToReceivePacket();
	}
}
 
class Process extends JFrame{
	public static final int[][] wfg = {
		{0,1,0,0,0,0},
		{0,0,1,0,0,1},
		{0,0,0,1,1,0},
		{0,0,0,0,1,0},
		{0,0,0,0,0,1},
		{1,0,0,0,0,0}
	};         

	private final JLabel cmhOR = new JLabel("Deadlock detection Chandy-Misra-Haas Algorithm for the OR model");
	private final JLabel logLabel = new JLabel("Log");
	private final JLabel statusBar = new JLabel("By Shruti Sagar Mohanta (2020MT12024)");
	private final JButton initDLD = new JButton("<html>Initiate <br/>Deadlock <br/>Detection</html>");
	private final JTextArea logArea = new JTextArea();
	private DatagramSocket dgSocket;
	private Boolean[] waitFlag = new Boolean[wfg[0].length]; 
	private int[] num = new int[wfg[0].length]	;
	private final String processName;
	private ArrayList<String> waitProcessName = new ArrayList<String>();
	private ArrayList<String> engagingQuerySender = new ArrayList<String>();
	private int querySend;
	private int replyReceive;
	Random rnd = new Random();
           
	public Process (String pName, int port){
		super("Process: "+pName+" Port: "+port);
		super.add(cmhOR, BorderLayout.NORTH);
		super.add(logLabel, BorderLayout.WEST);
		super.add(initDLD, BorderLayout.EAST);
		super.add(new JScrollPane(logArea), BorderLayout.CENTER);
		super.add(statusBar, BorderLayout.SOUTH);
		super.setSize(new Dimension(450,350));
		super.setVisible(true);
		super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		super.setResizable(false);
		super.setAlwaysOnTop(true);
		super.setIconImage(new ImageIcon("icon.png").getImage());
		logArea.setEditable(false);
		Arrays.fill(waitFlag,false);
	    processName = pName;
		replyReceive = 0;
		querySend = 0;
		try{
			dgSocket = new DatagramSocket((getIndex(pName)+1)*1000);
		}catch(SocketException ex){
			System.exit(1);
		}
	   
		for(int i=0;i < wfg[0].length;++i){
			if(wfg[getIndex(pName)][i] == 1){
				waitProcessName.add(getProcessName(i));
			}
		}
		initDLD.addActionListener((ActionEvent evt) -> {			                            
			String msg;		   			
			try{                                         
				logMsg("Deadlock detection initiated");
				for(int j=0; j < waitProcessName.size();++j){
					msg="QUERY-"+pName+","+pName+","+waitProcessName.get(j);
					byte buff[]=msg.getBytes();
					DatagramPacket dgPacketSend = new DatagramPacket(buff,buff.length,InetAddress.getLocalHost(),(getIndex(waitProcessName.get(j))+1)*1000);
					dgSocket.send(dgPacketSend);
					++querySend;
					logMsg("\nQUERY("+pName+","+pName+","+waitProcessName.get(j)+") sent to Dependent Process "+waitProcessName.get(j));
				}   
				waitFlag[getIndex(processName)] = true;  
				num[getIndex(processName)] = waitProcessName.size();  				
			}catch(IOException ex){
				logMsg(ex.getMessage());
			}                                  
		});    

	}
	public synchronized void readyToReceivePacket() throws InterruptedException{
		while(true){
			try{
				byte buff[]=new byte[128];
				DatagramPacket dgPacketReceive = new DatagramPacket(buff,buff.length);
				dgSocket.receive(dgPacketReceive);
				String strMsg = new String(dgPacketReceive.getData());
				strMsg = strMsg.trim();
				String msgType = strMsg.split("-")[0];
				String triplet = strMsg.split("-")[1];
				String initProcessName = triplet.split(",")[0];
				String senderProcessName = triplet.split(",")[1];
				String receiverProcessName = triplet.split(",")[2];				
				logMsg("\n"+msgType+"("+initProcessName+","+senderProcessName+","+receiverProcessName+") received from Process " + senderProcessName);
				String msg;	
				if(waitProcessName.size() > 0){
					if(msgType.equals("QUERY")){
						engagingQuerySender.add(senderProcessName);
						if(waitFlag[getIndex(initProcessName)]){
							Thread.sleep(100+rnd.nextInt(401));
							//Send REPLY message
							if(initProcessName.equals(processName)){//If Initiator is Receiver
								msg="REPLY-"+initProcessName+","+processName+","+senderProcessName;
								byte buffreply[]=msg.getBytes();
								DatagramPacket dgPacketSend = new DatagramPacket(buffreply,buffreply.length,InetAddress.getLocalHost(),(getIndex(senderProcessName)+1)*1000);
								dgSocket.send(dgPacketSend);
								logMsg("\nREPLY("+initProcessName+","+processName+","+senderProcessName+") sent to Process "+senderProcessName);
							}else{//REPLY RECEIVE COUNT == QUERY SEND COUNT for Other Processes
								if(replyReceive == querySend){
									msg="REPLY-"+initProcessName+","+processName+","+senderProcessName;
									byte buffreply[]=msg.getBytes();
									DatagramPacket dgPacketSend = new DatagramPacket(buffreply,buffreply.length,InetAddress.getLocalHost(),(getIndex(senderProcessName)+1)*1000);
									dgSocket.send(dgPacketSend);
									logMsg("\nREPLY("+initProcessName+","+processName+","+senderProcessName+") sent to Process "+senderProcessName);
								}
							}
						}else{
							waitFlag[getIndex(initProcessName)] = true;
							num[getIndex(processName)] = waitProcessName.size();
							for(int k=0; k < waitProcessName.size();++k){
								Thread.sleep(100+rnd.nextInt(401));
								msg="QUERY-"+initProcessName+","+processName+","+waitProcessName.get(k);
								byte buffsend[]=msg.getBytes();
								DatagramPacket dgPacketSend = new DatagramPacket(buffsend,buffsend.length,InetAddress.getLocalHost(),(getIndex(waitProcessName.get(k))+1)*1000);
								dgSocket.send(dgPacketSend);
								++querySend;
								logMsg("\nQUERY("+initProcessName+","+processName+","+waitProcessName.get(k)+") sent to Dependent Process "+waitProcessName.get(k));
							} 	
						}						
					}
					if(msgType.equals("REPLY")){
						++replyReceive;
						if(waitFlag[getIndex(initProcessName)]) num[getIndex(processName)] -= 1 ;
						if(num[getIndex(processName)] == 0){
							if(initProcessName.equals(receiverProcessName)){
								Thread.sleep(100+rnd.nextInt(401));
								logMsg("\n\n!!!!!   Deadlock Detected   !!!!!\n");
							}else{
								//Send REPLY message to Engaging Query Sender
								for(int m=0; m < engagingQuerySender.size();++m){
									Thread.sleep(100+rnd.nextInt(401));
									msg="REPLY-"+initProcessName+","+processName+","+engagingQuerySender.get(m);
									byte buffreply[]=msg.getBytes();
									DatagramPacket dgPacketSend = new DatagramPacket(buffreply,buffreply.length,InetAddress.getLocalHost(),(getIndex(engagingQuerySender.get(m))+1)*1000);
									dgSocket.send(dgPacketSend);
									logMsg("\nREPLY("+initProcessName+","+processName+","+engagingQuerySender.get(m)+") sent to Process "+engagingQuerySender.get(m));
								}								
							}
						}
					}
				}
			}catch(IOException ex){
				logMsg(ex.getMessage());
			}
		}
	}
	public void logMsg(final String msg) {
		SwingUtilities.invokeLater(() -> {
			logArea.append(msg);
		});
	}
	public int getIndex(String proc) {
		int idx = 0;
		switch(proc){
			case "A":
		idx = 0;break;
			case "B" :
				idx = 1;break;
			case "C" :
				idx = 2;break;
			case "D" :
				idx = 3;break;
			case "E" :
				idx = 4;break;
			case "F" :
				idx = 5;break;
		}
		return idx;
	}
	public String getProcessName(int idx) {
		String str = "";
		switch(idx){
			case 0:
				str = "A";break;
			case 1:
				str = "B";break;
			case 2:
				str = "C";break;
			case 3:
				str = "D";break;
			case 4:
				str = "E";break;
			case 5:
				str = "F";break;          
		}
		return str;
	}
}