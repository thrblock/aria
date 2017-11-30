package com.thrblock.aria.music;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * A music tool used by console
 * <p>
 * 一个控制台下的音乐工具
 * <a href="mailto:thrblock@gmail.com">thrblock</a>
 */
public class ConsoleMusic {
    private static AbstractApplicationContext context;
    static {
        context = new ClassPathXmlApplicationContext("aria-context.xml");
        context.registerShutdownHook();
    }

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, InterruptedException {
        MusicPlayer player = context.getBean(MusicPlayer.class);
        player.initMusic(new File(args[0]));
        player.play(-1);
    }
}
