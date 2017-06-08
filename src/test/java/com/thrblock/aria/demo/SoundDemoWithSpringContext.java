package com.thrblock.aria.demo;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.thrblock.aria.sound.AriaSoundException;
import com.thrblock.aria.sound.Sound;
import com.thrblock.aria.sound.SoundFactory;

public class SoundDemoWithSpringContext {
    private static AbstractApplicationContext context;
    static {
        context = new ClassPathXmlApplicationContext("aria-context.xml");
        context.registerShutdownHook();
    }

    public static void main(String[] args) throws InterruptedException, AriaSoundException {
        SoundFactory factory = context.getBean(SoundFactory.class);
        Sound s1 = factory.buildSound(SoundDemoWithSpringContext.class.getResourceAsStream("NT.mp3"));
        for (int i = 0; i < 50; i++) {
            s1.play();
            Thread.sleep(2000);
        }
    }
}
