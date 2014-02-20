package com.duam.talktohome;

import static com.duam.talktohome.ConstantesTalkToHomeServer.AUX_FILE_NAME;
import static com.duam.talktohome.ConstantesTalkToHomeServer.OK_MESSAGE;
import static com.duam.talktohome.ConstantesTalkToHomeServer.PORT;
import static com.duam.talktohome.ConstantesTalkToHomeServer.PACKET_SIZE;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TalkToServer
{
	private boolean acceptingNew = true;
	
	public void start() throws IOException
	{
		DatagramSocket serverSocket = new DatagramSocket(PORT);

		byte[] bytes = new byte[PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(bytes, bytes.length);
		System.out.println("Listening on port "+ PORT);

		for (long id = 0; ; id++)
		{
			if (acceptingNew)
			{
				serverSocket.receive(receivePacket);
				
				long size = Long.parseLong(new String(receivePacket.getData()).trim());
				
				receiveFile(serverSocket, size, id);		
				play(id);
				delete(id);
				sendResponse(serverSocket, receivePacket.getAddress(), receivePacket.getPort());				
			}
		}
	}
	
	private void sendResponse(final DatagramSocket socket, final InetAddress IPAddress, final int port)
	{
		acceptingNew = false;
		
		Thread th = new Thread()
		{
			@Override
			public void run() 
			{
				RecDialog dialog = new RecDialog() 
				{					
					@Override
					protected void onResponseRecorded(File outputFile) 
					{
						uploadFile(outputFile, socket, IPAddress, port);
					}
				};
				dialog.show();
			}			
		};
		th.start();
	}
	
	private void uploadFile(File file, DatagramSocket socket, InetAddress address, int port)
	{
		String fileLength = String.valueOf(file.length());
		
		byte[] bytes = new byte[PACKET_SIZE];
		
		FileInputStream fis;
		
		try 
		{			
			fis = new FileInputStream(file);
			
			// inform file size
			socket.send(new DatagramPacket(fileLength.getBytes(), fileLength.getBytes().length, address, port));
			System.out.println("File length sent to: "+ address.toString() +":"+ port);
			
			byte[] response = new byte[256];
			
			while (fis.read(bytes) >= 0)
			{
				DatagramPacket sendPacket = new DatagramPacket(bytes, bytes.length, address, port);
				socket.send(sendPacket);

				DatagramPacket responsePacket = new DatagramPacket(response, response.length, address, port);
				socket.receive(responsePacket);
			}
			System.out.println("File content sent");
			
			fis.close();
			
			acceptingNew = true;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void delete(long id)
	{
		File file = new File(String.valueOf(id) +"_"+ AUX_FILE_NAME);
		System.out.println("Borrando... "+ file.getAbsolutePath());
		file.delete();
	}
	
	private void play(long id)
	{
		System.out.println("Playing file ["+"ffplay -autoexit -loglevel quiet -nodisp "+ String.valueOf(id) +"_"+ AUX_FILE_NAME+"]");
		try
		{
			Runtime rt = Runtime.getRuntime();
			Process p = rt.exec("ffplay -autoexit -loglevel quiet -nodisp "+ String.valueOf(id) +"_"+ AUX_FILE_NAME);
			p.waitFor();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void receiveFile(DatagramSocket serverSocket, long fileSize, long id)
	{
		System.out.println("Receiving file");
		
		try
		{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(String.valueOf(id) +"_"+ AUX_FILE_NAME));
			
			byte[] response = OK_MESSAGE.getBytes(); 
			
			int count = 0;
			
			while (count < fileSize)
			{				
				byte[] bytes = new byte[(fileSize > PACKET_SIZE) ? PACKET_SIZE : (int) fileSize];
				DatagramPacket packet = new DatagramPacket(bytes, bytes.length);

				serverSocket.receive(packet);
				bos.write(bytes);
		
				InetAddress IPAddress = packet.getAddress();
				int port = packet.getPort();
				
				DatagramPacket responsePacket = new DatagramPacket(response, response.length, IPAddress, port);
				serverSocket.send(responsePacket);
				
				count += bytes.length;
				long remaining = fileSize - count;

				if (remaining > 0)
				{
					bytes = new byte[(remaining < PACKET_SIZE) ? (int) remaining : PACKET_SIZE];
				}
			}
			System.out.println("File received");
			bos.flush();
			bos.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
	}
}
