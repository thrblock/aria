package com.thrblock.aria.sound;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.thrblock.aria.decoder.IDecoder;

/**
 * 音效工厂
 * 
 * @author Administrator
 *
 */
@Component
@Lazy(true) // Lazy 当系统不需要音频需求时，不会创建线程实例
public class SoundFactory {
    /**
     * 底层播放缓冲区设置
     */
    private static final int LINE_CACHE_LENGTH = 8 * 1024; // 8 KB Line Cache

    private ExecutorService commonsPool;
    private Set<Sound> loadedSound;

    @Autowired
    private IDecoder decoder;

    /**
     * for spring use only
     */
    SoundFactory() {
    }

    public SoundFactory(IDecoder decoder) {
        this.decoder = decoder;
    }

    @PostConstruct
    public void init() {
        commonsPool = Executors.newCachedThreadPool();
        loadedSound = new HashSet<>();
    }

    public Sound buildSound(InputStream src)
            throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(src)) {
            AudioInputStream decodedStream = decoder.getDecodedAudioInputStream(ais);
            AudioFormat decodedFormat = decoder.getDecodedAudioFormat(ais.getFormat());
            byte[] loadCache = new byte[1024];
            ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
            for (int realRead = 0; realRead != -1; realRead = decodedStream.read(loadCache, 0, loadCache.length)) {
                byteOS.write(loadCache, 0, realRead);
            }
            Sound s = new Sound(readyLineByFormat(decodedFormat), commonsPool, byteOS.toByteArray());
            loadedSound.add(s);
            return s;
        }
    }

    public Sound buildSound(File f) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        return buildSound(new FileInputStream(f));
    }

    private SourceDataLine readyLineByFormat(AudioFormat format) throws LineUnavailableException {
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, format, LINE_CACHE_LENGTH);
        SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        sourceDataLine.open();
        sourceDataLine.start();
        return sourceDataLine;
    }

    @PreDestroy
    public void destory() throws InterruptedException {
        commonsPool.shutdown();
        commonsPool.awaitTermination(3, TimeUnit.SECONDS);
        for (Sound s : loadedSound) {
            s.destory();
        }
    }
}