package com.duam.talktohome;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;



public class Main 
{

	public static void main(String args[]) throws Exception 
	{
		SystemTray systray = SystemTray.getSystemTray();

		try
		{
			PopupMenu menu = new PopupMenu();
			MenuItem itemExit = new MenuItem("Cerrar");
			itemExit.addActionListener(new ActionListener() 
			{				
				@Override
				public void actionPerformed(ActionEvent event) 
				{
					System.exit(0);
				}
			});
			menu.add(itemExit);
			
		    TrayIcon trayicon = new TrayIcon(ImageIO.read(Main.class.getResourceAsStream("icono.png")), "Talk 2 Home - SERVER");
		    trayicon.setImageAutoSize(true);
		    trayicon.setPopupMenu(menu);
		    
		    systray.add(trayicon);
		}
		catch(IOException e) 
		{
		    e.printStackTrace();
		}

		TalkToServer server = new TalkToServer();
		server.start();
	}
	
	private static void toto()
	{
		AudioFormat	audioFormat = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				44100.0F, 16, 2, 4, 44100.0F, false);
		
		TargetDataLine line;
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat); // format is an AudioFormat object
		if (!AudioSystem.isLineSupported(info)) {
		    // Handle the error ...
			System.out.println("erro");
		}
		// Obtain and open the line.
		try {
		    line = (TargetDataLine) AudioSystem.getLine(info);
		    line.open(audioFormat);
		} catch (LineUnavailableException ex) {
		    // Handle the error ...
			ex.printStackTrace();
		}

		// Assume that the TargetDataLine, line, has already
		// been obtained and opened.
		ByteArrayOutputStream out  = new ByteArrayOutputStream();
		int numBytesRead;
		byte[] data = new byte[line.getBufferSize() / 5];

		// Begin audio capture.
		line.start();

		// Here, stopped is a global boolean set by another thread.
		while (!stopped) {
		    // Read the next chunk of data from the TargetDataLine.
		    numBytesRead =  line.read(data, 0, data.length);
		    // Save this chunk of data.
		    out.write(data, 0, numBytesRead);
		}		
	}

}
