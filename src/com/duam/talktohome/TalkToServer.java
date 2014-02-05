package com.duam.talktohome;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TalkToServer
{
	private static final int PORT = 1616;
	private static final int PACKET_SIZE = 1024;
	private static final String OK_MESSAGE = "OK";
	private static final String AUX_FILE_NAME = "aux_audio_file.3gp";
	
	public void start() throws IOException
	{
		DatagramSocket serverSocket = new DatagramSocket(PORT);

		byte[] bytes = new byte[PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(bytes, bytes.length);
		System.out.println("Listening on port "+ PORT);

		for (long id = 0; ; id++)
		{
			serverSocket.receive(receivePacket);
			
			long size = Long.parseLong(new String(receivePacket.getData()).trim());
			
			receiveFile(serverSocket, size, id);			
			play(id);
			delete(id);
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
