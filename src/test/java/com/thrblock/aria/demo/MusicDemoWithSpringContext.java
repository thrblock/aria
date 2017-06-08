package com.thrblock.aria.demo;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.thrblock.aria.music.MusicPlayer;

public class MusicDemoWithSpringContext {
    private static AbstractApplicationContext context;
    static {
        context = new ClassPathXmlApplicationContext("aria-context.xml");
        context.registerShutdownHook();
    }

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, InterruptedException {
        MusicPlayer player = context.getBean(MusicPlayer.class);
        player.initMusic(new File("./Blast.mp3"));
        player.play(-1);
    }
}
