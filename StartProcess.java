import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;
import java.lang.*;
import javax.swing.text.*;
 
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
	//private final JTextArea logArea = new JTextArea();
	private final JTextPane logArea = new JTextPane();
	
	private final SimpleAttributeSet formatInit = new SimpleAttributeSet();
	
	private final SimpleAttributeSet formatQuerySend = new SimpleAttributeSet();
	private final SimpleAttributeSet formatReplySend = new SimpleAttributeSet();
	private final SimpleAttributeSet formatDeadlock = new SimpleAttributeSet();
	private final SimpleAttributeSet formatQueryReceive = new SimpleAttributeSet();
	private final SimpleAttributeSet formatReplyReceive = new SimpleAttributeSet();
	
	
	private final StyledDocument styleDoc = logArea.getStyledDocument();
	private DatagramSocket dgSocket;
	private Boolean[] waitFlag = new Boolean[wfg[0].length]; 
	private int[] num = new int[wfg[0].length]	;
	private final String processName;
	private ArrayList<String> dependentSet = new ArrayList<String>();
	private ArrayList<String> engagingQuerySender = new ArrayList<String>();
	Random rnd = new Random();
	
	public Process (String pName, int port){
		super("Process: "+pName+" Port: "+port);
		super.add(cmhOR, BorderLayout.NORTH);
		super.add(logLabel, BorderLayout.WEST);
		super.add(initDLD, BorderLayout.EAST);
		super.add(new JScrollPane(logArea), BorderLayout.CENTER);
		super.add(statusBar, BorderLayout.SOUTH);
		super.setSize(new Dimension(500,280));
		super.setVisible(true);
		super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		super.setResizable(false);
		super.setIconImage(new ImageIcon("icon.png").getImage());
		logArea.setEditable(false);
		StyleConstants.setForeground(formatInit, Color.BLACK);
		StyleConstants.setBold(formatInit, true);
		StyleConstants.setUnderline(formatInit, true);
		StyleConstants.setForeground(formatQuerySend, Color.BLUE);
		StyleConstants.setForeground(formatReplySend, Color.MAGENTA);	
		StyleConstants.setForeground(formatDeadlock, Color.RED);
		StyleConstants.setForeground(formatQueryReceive, new Color(0, 128, 0));
		StyleConstants.setForeground(formatReplyReceive, new Color(255, 128, 64));			
		StyleConstants.setBold(formatDeadlock, true);
		Arrays.fill(waitFlag,false);
	    processName = pName;
		
				
		try{
			dgSocket = new DatagramSocket((getIndex(pName)+1)*1000);
			
		}catch(SocketException ex){
			System.exit(1);
		}
	   
		for(int i=0;i < wfg[0].length;++i){
			if(wfg[getIndex(pName)][i] == 1){
				dependentSet.add(getProcessName(i));
			}
		}
		initDLD.addActionListener((ActionEvent evt) -> {			                            
			String msg;		   			
			try{ 
				logMsg("Deadlock detection initiated",formatInit);
				for(int j=0; j < dependentSet.size();++j){
					msg="QUERY-"+pName+","+pName+","+dependentSet.get(j);
					byte buff[]=msg.getBytes();
					DatagramPacket dgPacketSend = new DatagramPacket(buff,buff.length,InetAddress.getLocalHost(),(getIndex(dependentSet.get(j))+1)*1000);
					dgSocket.send(dgPacketSend);
					logMsg("QUERY("+pName+","+pName+","+dependentSet.get(j)+") sent to Dependent Process "+dependentSet.get(j),formatQuerySend);
				}   
				waitFlag[getIndex(processName)] = true;  
				num[getIndex(processName)] = dependentSet.size();  				
			}catch(IOException ex){
				logMsg(ex.getMessage(),formatDeadlock);
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
				logMsg(msgType+"("+initProcessName+","+senderProcessName+","+receiverProcessName+") received from Process " + senderProcessName,msgType.equals("QUERY") ? formatQueryReceive : formatReplyReceive);
				String msg;	
				if(dependentSet.size() > 0){
					if(msgType.equals("QUERY")){
						if(waitFlag[getIndex(initProcessName)]){
							Thread.sleep(500+rnd.nextInt(501));
							msg="REPLY-"+initProcessName+","+processName+","+senderProcessName;
							byte buffreply[]=msg.getBytes();
							DatagramPacket dgPacketSend = new DatagramPacket(buffreply,buffreply.length,InetAddress.getLocalHost(),(getIndex(senderProcessName)+1)*1000);
							dgSocket.send(dgPacketSend);
							logMsg("REPLY("+initProcessName+","+processName+","+senderProcessName+") sent to Process "+senderProcessName,formatReplySend);							
						}else{
							engagingQuerySender.add(senderProcessName);						
							waitFlag[getIndex(initProcessName)] = true;
							num[getIndex(initProcessName)] = dependentSet.size();
							for(int k=0; k < dependentSet.size();++k){
								Thread.sleep(500+rnd.nextInt(501));
								msg="QUERY-"+initProcessName+","+processName+","+dependentSet.get(k);
								byte buffsend[]=msg.getBytes();
								DatagramPacket dgPacketSend = new DatagramPacket(buffsend,buffsend.length,InetAddress.getLocalHost(),(getIndex(dependentSet.get(k))+1)*1000);
								dgSocket.send(dgPacketSend);
								logMsg("QUERY("+initProcessName+","+processName+","+dependentSet.get(k)+") sent to Dependent Process "+dependentSet.get(k),formatQuerySend);
							} 	
						}						
					}
					if(msgType.equals("REPLY")){						
						if(waitFlag[getIndex(initProcessName)]) {
							num[getIndex(initProcessName)] -= 1 ;
							if(num[getIndex(initProcessName)] == 0){
								if(initProcessName.equals(receiverProcessName)){
									Thread.sleep(500+rnd.nextInt(501));
									logMsg("\n!!!!!   Deadlock Detected   !!!!!",formatDeadlock);
								}else{
									for(int m=0; m < engagingQuerySender.size();++m){
										Thread.sleep(500+rnd.nextInt(501));
										msg="REPLY-"+initProcessName+","+processName+","+engagingQuerySender.get(m);
										byte buffreply[]=msg.getBytes();
										DatagramPacket dgPacketSend = new DatagramPacket(buffreply,buffreply.length,InetAddress.getLocalHost(),(getIndex(engagingQuerySender.get(m))+1)*1000);
										dgSocket.send(dgPacketSend);		
										logMsg("REPLY("+initProcessName+","+processName+","+engagingQuerySender.get(m)+") sent to Process "+engagingQuerySender.get(m)+" which sent the engaging query",formatReplySend);
									}								
								}
							}
						}
					}
				}
			}catch(IOException ex){
				logMsg(ex.getMessage(),formatDeadlock);
			}
		}
	}
	public void logMsg(final String msg, final SimpleAttributeSet attrib) {
		SwingUtilities.invokeLater(() -> {
			try
			{
				styleDoc.insertString(styleDoc.getLength(), msg+"\n", attrib);
			}catch(Exception e) { 
				logMsg(e.getMessage(),formatDeadlock);
			}
			
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