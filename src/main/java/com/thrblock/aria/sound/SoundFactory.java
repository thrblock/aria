package com.thrblock.aria.sound;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.thrblock.aria.decoder.IDecoder;

/**
 * the sound factory
 * <p>
 * 音效工厂
 * 
 * <a href="mailto:thrblock@gmail.com">thrblock</a>
 *
 */
@Component
@Lazy(true) // Lazy 当系统不需要音频需求时，不会创建线程实例
public class SoundFactory {

    private ExecutorService commonsPool;

    @Autowired
    private IDecoder decoder;

    /**
     * for spring use only
     */
    SoundFactory() {
    }

    /**
     * build with the given decoder
     * <p>
     * 
     * @param decoder
     *            解码器
     */
    public SoundFactory(IDecoder decoder) {
        this.decoder = decoder;
    }

    /**
     * init the factory,auto done when in spring context
     * <p>
     * 初始化工厂 当使用spring控制时自动完成
     */
    @PostConstruct
    public void init() {
        commonsPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("AriaSE-" + t.getId());
            return t;
        });
    }

    /**
     * build sond by src audio stream
     * <p>
     * 构造一个音效实例
     * 
     * @param src
     *            src audio stream,eg.file input stream. 原始音频数据流（不是解码流）
     * @return 音效实例
     * @throws AriaSoundException
     *             when exception 当出现异常时抛出
     */
    public Sound buildSound(InputStream src) throws AriaSoundException {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(src)) {
            AudioInputStream decodedStream = decoder.getDecodedAudioInputStream(ais);
            AudioFormat decodedFormat = decoder.getDecodedAudioFormat(ais.getFormat());
            byte[] loadCache = new byte[1024];
            ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
            for (int realRead = 0; realRead != -1; realRead = decodedStream.read(loadCache, 0, loadCache.length)) {
                byteOS.write(loadCache, 0, realRead);
            }
            return new Sound(decodedFormat, commonsPool, byteOS.toByteArray());
        } catch (IOException | UnsupportedAudioFileException e) {
            throw new AriaSoundException(e);
        }
    }

    /**
     * build sond by src audio file
     * <p>
     * 构造一个音效实例
     * 
     * @param f
     *            src audio file 原始音频文件
     * @return sound instance 音效实例
     * @throws AriaSoundException
     *             when exception 当异常时抛出
     */
    public Sound buildSound(File f) throws AriaSoundException {
        try(InputStream is = new FileInputStream(f)) {
            return buildSound(new BufferedInputStream(is));
        } catch (IOException e) {
            throw new AriaSoundException(e);
        }
    }

    /**
     * destroy se thread pool<p>
     * 等待音效播放完全并销毁音效线程池
     * 
     * @throws InterruptedException
     *             when interrupted 当超时时抛出
     */
    @PreDestroy
    public void destroy() throws InterruptedException {
        commonsPool.shutdown();
        commonsPool.awaitTermination(3, TimeUnit.SECONDS);
    }
}