package com.thrblock.aria.sound;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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
 * @author Administrator
 *
 */
@Component
@Lazy(true)//Lazy 当系统不需要音频需求时，不会创建线程实例
public class SoundFactory {
    /**
     * 底层播放缓冲区设置
     */
    private static final int LINE_CACHE_LENGTH = 8 * 1024; //8 KB Line Cache
    
    private ExecutorService commonsPool;
    private Map<AudioFormatInfo,LoadedSoundData> lineMap;
    
    @Autowired
    private IDecoder decoder;
    
    @PostConstruct
    public void init() {
        commonsPool = Executors.newCachedThreadPool();
        lineMap = new HashMap<>();
    }
    
    public Sound buildSound(InputStream src) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        try(AudioInputStream ais = AudioSystem.getAudioInputStream(src)){
            AudioInputStream decodedStream = decoder.getDecodedAudioInputStream(ais);
            AudioFormatInfo formatInfo = new AudioFormatInfo(decodedStream.getFormat());
            if(lineMap.containsKey(formatInfo)) {
                LoadedSoundData loadedData = lineMap.get(formatInfo);
                return new Sound(loadedData.getLine(), commonsPool, loadedData.getDecodedSrc());
            } else {
                byte[] loadCache = new byte[1024];
                ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
                for(int realRead = 0;realRead != -1;realRead = ais.read(loadCache, 0, loadCache.length)) {
                    byteOS.write(loadCache, 0, realRead);
                }
                LoadedSoundData loadedData = new LoadedSoundData();
                loadedData.setDecodedSrc(byteOS.toByteArray());
                loadedData.setLine(readyLineByFormat(decodedStream.getFormat()));
                lineMap.put(formatInfo, loadedData);
                return new Sound(loadedData.getLine(), commonsPool, loadedData.getDecodedSrc());
            }
        }
    }
    
    public Sound buildSound(File f) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        return buildSound(new FileInputStream(f));
    }
    
    private SourceDataLine readyLineByFormat(AudioFormat format) throws LineUnavailableException {
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class,format,LINE_CACHE_LENGTH);
        SourceDataLine sourceDataLine = (SourceDataLine)AudioSystem.getLine(info);
        sourceDataLine.open();
        sourceDataLine.start();
        return sourceDataLine;
    }
    
    @PreDestroy
    public void destory() throws InterruptedException {
        commonsPool.shutdown();
        commonsPool.awaitTermination(3,TimeUnit.SECONDS);
        for(LoadedSoundData data:lineMap.values()) {
            data.getLine().close();
        }
    }
}