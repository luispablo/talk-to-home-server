package com.duam.talktohome;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public abstract class RecDialog 
{
	private static final String MENSAJE_TIMEOUT = "La ventana se cerrará en XXX segundos";
	private static final String MENSAJE_GRABANDO = "Tiempo restante: XXX segundos";
	private static final String OUTPUT_FILENAME = "temp_response.3gp";
	
    private int timeout = 10;
    private int recTimeout = 30;
    private boolean grabando = false;    
    private File wavFile = new File("temp_response.wav");
    private TargetDataLine line;
    
    public void show() 
    {
    	final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setText("Home2Server response...");
        shell.setSize(300, 200);

        FillLayout layout = new FillLayout();
        layout.spacing = 10;
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        shell.setLayout(layout);
        
        final Label label = new Label(shell, SWT.SHADOW_NONE);
        label.setText(MENSAJE_TIMEOUT.replaceFirst("XXX", String.valueOf(timeout)));
        
        display.addFilter(SWT.KeyDown, new Listener() 
        {			
			@Override
			public void handleEvent(Event event) 
			{
				switch(event.keyCode)
				{
					case SWT.SPACE:
						if (!grabando)
						{
							Thread th = new Thread()
							{
								@Override
								public void run() 
								{
									startRecording();
								}								
							};
							th.start();

							grabando = true;
							label.setForeground(display.getSystemColor(SWT.COLOR_RED));
							label.setText(MENSAJE_GRABANDO.replaceFirst("XXX", String.valueOf(recTimeout)));
							
					        Runnable r = new Runnable() 
					        {			
								@Override
								public void run() 
								{
									if (!label.isDisposed())
									{
										label.setText(MENSAJE_GRABANDO.replaceFirst("XXX", String.valueOf(recTimeout--)));
										
										if (recTimeout > 0)
										{
											display.timerExec(1000, this);
										}
										else
										{
											shell.dispose();
										}
									}
								}
							};
							display.timerExec(1000, r);							
						}
						break;
				}
			}
		});
        
        display.addFilter(SWT.KeyUp, new Listener() 
        {			
			@Override
			public void handleEvent(Event event) 
			{
				switch(event.keyCode)
				{
					case SWT.SPACE:
						if (grabando)
						{
							Thread th = new Thread()
							{
								@Override
								public void run() 
								{
									stopRecording();
								}
							};
							th.start();
							shell.dispose();
							grabando = false;
						}
						break;
					case SWT.ESC:
						shell.dispose();
						break;
				}
			}
		});
        
        Runnable r = new Runnable() 
        {			
			@Override
			public void run() 
			{
				if (!label.isDisposed() && !grabando)
				{
					label.setText(MENSAJE_TIMEOUT.replaceFirst("XXX", String.valueOf(timeout--)));
					
					if (timeout > 0)
					{
						display.timerExec(1000, this);
					}
					else
					{
						shell.dispose();
					}
				}
			}
		};
		display.timerExec(1000, r);

        shell.pack();
        shell.open();

        while (!shell.isDisposed())
        {
        	if (!display.readAndDispatch())
        	{
                display.sleep();
        	}
        }
        display.dispose();
    }
	
    private void startRecording() 
    {
        try 
        {
            AudioFormat format = new AudioFormat(16000, 8, 2, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
 
            // checks if system supports the data line
            if (!AudioSystem.isLineSupported(info)) 
            {
                System.out.println("Line not supported");
                System.exit(0);
            }
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();   // start capturing
 
            System.out.println("Start capturing...");
 
            AudioInputStream ais = new AudioInputStream(line);
 
            System.out.println("Start recording...");
 
            // start recording
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
        } 
        catch (LineUnavailableException | IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    private void stopRecording() 
    {
        line.stop();
        line.close();
        System.out.println("Finished");
        
        try
		{
			Runtime rt = Runtime.getRuntime();
        	System.out.println("Convirtiendo archivo...");
			Process p = rt.exec("ffmpeg -loglevel quiet -i temp_response.wav -f 3gp -ac 1 -ar 8000 -ab 6.7k temp_response.3gp");
			p.waitFor();
			
			System.out.println("archivo convertido, borrando wav");
			wavFile.delete();
			
			File amrFile = new File(OUTPUT_FILENAME);
			System.out.println("Llamando al evento para enviar");
			onResponseRecorded(amrFile);
			System.out.println("Enviado. Borrando 3gp");
			amrFile.delete();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
    }
    
    protected abstract void onResponseRecorded(File outputFile);
    
}
