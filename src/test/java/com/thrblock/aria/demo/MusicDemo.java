package com.thrblock.aria.demo;

import java.io.File;

import com.thrblock.aria.decoder.SPIDecoder;
import com.thrblock.aria.music.MusicPlayer;

public class MusicDemo {
    public static void main(String[] args) throws InterruptedException {
        MusicPlayer player = new MusicPlayer(new SPIDecoder());
        player.initMusic(new File("./Blast.mp3"));
        player.play(-1);
        Thread.sleep(5000);
        player.stop();
        player.destory();
    }
}
