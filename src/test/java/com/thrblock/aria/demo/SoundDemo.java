package com.thrblock.aria.demo;

import com.thrblock.aria.decoder.SPIDecoder;
import com.thrblock.aria.sound.AriaSoundException;
import com.thrblock.aria.sound.Sound;
import com.thrblock.aria.sound.SoundFactory;

public class SoundDemo {
    public static void main(String[] args) throws InterruptedException, AriaSoundException {
        SoundFactory factory = new SoundFactory(new SPIDecoder());
        factory.init();
        Sound s1 = factory.buildSound(SoundDemo.class.getResourceAsStream("NT.mp3"));
        for (int i = 0; i < 50; i++) {
            s1.play();
            Thread.sleep(2000);
        }
        factory.destory();
    }
}
