package com.duam.talktohome;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.imageio.ImageIO;



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
			
		    TrayIcon trayicon = new TrayIcon(ImageIO.read(Main.class.getResourceAsStream("icono.png")),"I am a description");
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

}
