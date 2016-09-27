package com.thrblock.aria.demo;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.thrblock.aria.decoder.SPIDecoder;
import com.thrblock.aria.sound.Sound;
import com.thrblock.aria.sound.SoundFactory;

public class SoundDemo {
	public static void main(String[] args) throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException {
		SoundFactory factory = new SoundFactory(new SPIDecoder());
		factory.init();
		Sound s1 = factory.buildSound(SoundDemo.class.getResourceAsStream("A1.mp3"));
		Sound s2 = factory.buildSound(SoundDemo.class.getResourceAsStream("A1.mp3"));
		s1.loop(5);
		Thread.sleep(500);
		s2.loop(5);
		Thread.sleep(1000);
		factory.destory();
	}
}
