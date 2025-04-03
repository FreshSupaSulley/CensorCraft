package com.supasulley.jscribe;

import java.awt.Color;
import java.awt.Graphics;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class AppTest {
	
	static JScribe scribe;
	
	public static void main(String[] args) throws Exception
	{
		scribe = new JScribe(Paths.get("ggml-tiny.en.bin"));
		scribe.start("", 2000, 1500, 30000);
		
		System.out.println(scribe.getActiveMicrophone().getName());
		
		JFrame frame = new JFrame();
		frame.setBounds(0, 0, 200, 400);
		
		frame.add(new FuckYou());
		
		frame.setVisible(true);
		
		while(true)
		{
			String buffer = scribe.getBuffer();
			if(!buffer.isBlank())
			{
				System.out.println(buffer);
			}
			
			frame.repaint();
			FuckYou.length = (int) (scribe.getAudioLevel() * 200);
		}
		// Translate for a while
//		while(true)
//		{
//			scribe.stop();
//			
//			if(scribe.isRunning())
//			{
//				System.out.println("problem");
//				System.exit(1);
//			}
//			long time = System.currentTimeMillis();
//			scribe.start("");
//			System.out.println(System.currentTimeMillis() - time);
//			if(!scribe.isRunning())
//			{
//				System.out.println("problem2");
//				System.exit(1);
//			}
//			Thread.sleep(1000);
//			System.out.println("This should be true: " + scribe.isRunning() + " " + scribe.isRunningAndNoAudio());
//		}
	}
	
	static class FuckYou extends JPanel {
		
		static int length = 0;
		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			g.fillRect(0, 0, length, 10);
			g.drawString("Level: " + scribe.getAudioLevel(), 0, 100);
		}
	}
}
