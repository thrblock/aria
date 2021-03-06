package com.thrblock.aria.sound;

import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * The Sound Effect class,all the raw data was loaded into memory
 * <p>
 * ALL the methods there are thread-safe.
 * <p>
 * Sound类代表音效实例；一个音效内容被完全的加载入内存中
 * <p>
 * 此外 Sound中的播放相关方法均并行可行并且线程安全
 * 
 * <a href="mailto:thrblock@gmail.com">thrblock</a>
 *
 */
public class Sound {
    /**
     * raw data cache<p>
     * 播放缓冲区设置
     */
    private static final int PLAY_CACHE_LENGTH = 2 * 1024;
    /**
     * the source data line cache<p>
     * LINE缓冲区设置
     */
    private static final int LINE_CACHE_LENGTH = 8 * 1024; // 8 KB Line Cache
    private AudioFormat format;
    private ExecutorService pool;
    private byte[] decodedSrc;

    protected Sound(AudioFormat format, ExecutorService commonsPool, byte[] decodedSrc) {
        this.format = format;
        this.pool = commonsPool;
        this.decodedSrc = decodedSrc;
    }

    /**
     * play once<p>
     * 播放一次音效
     */
    public void play() {
        pool.execute(() -> {
            SourceDataLine refLine = readyLineByFormat();
            int offset = 0;
            while (offset < decodedSrc.length) {
                offset += refLine.write(decodedSrc, offset, offset + PLAY_CACHE_LENGTH > decodedSrc.length
                        ? decodedSrc.length - offset : PLAY_CACHE_LENGTH);
            }
            refLine.drain();
            refLine.stop();
            refLine.close();
        });
    }

    /**
     * play and loop<p>
     * 循环播放音效
     * 
     * @param times
     *            loop time 播放次数
     */
    public void loop(int times) {
        pool.execute(() -> {
            SourceDataLine refLine = readyLineByFormat();
            for (int i = 0; i < times; i++) {
                int offset = 0;
                while (offset < decodedSrc.length) {
                    offset += refLine.write(decodedSrc, offset, offset + PLAY_CACHE_LENGTH > decodedSrc.length
                            ? decodedSrc.length - offset : PLAY_CACHE_LENGTH);
                }
            }
            refLine.drain();
            refLine.stop();
            refLine.close();
        });
    }

    /**
     * when finished once,get boolean from supplier and loop if true<p>
     * 指定一个布尔提供者，当返回true时重置至起始位置循环音效
     * 
     * @param booleanSupplier
     *            布尔值提供者
     */
    public void loopUntil(BooleanSupplier booleanSupplier) {
        loopUntil(booleanSupplier, 0);
    }

    /**
     * when finished once,get boolean from supplier and loop back the given offset if true<p>
     * 指定一个布尔提供者，当返回true时重置至reOffset位置循环音效
     * 
     * @param booleanSupplier
     *            布尔值提供者
     * @param reOffset
     *            循环偏移量
     */
    public void loopUntil(BooleanSupplier booleanSupplier, int reOffset) {
        pool.execute(() -> {
            int settedOffset = 0;
            SourceDataLine refLine = readyLineByFormat();
            while (booleanSupplier.getAsBoolean()) {
                int offset = settedOffset;
                while (offset < decodedSrc.length) {
                    offset += refLine.write(decodedSrc, offset, offset + PLAY_CACHE_LENGTH > decodedSrc.length
                            ? decodedSrc.length - offset : PLAY_CACHE_LENGTH);
                }
                settedOffset = reOffset;
            }
            refLine.drain();
            refLine.stop();
            refLine.close();
        });
    }

    private SourceDataLine readyLineByFormat() {
        try {
            SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, format, LINE_CACHE_LENGTH);
            SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open();
            sourceDataLine.start();
            return sourceDataLine;
        } catch (LineUnavailableException e) {
            throw new AriaSoundRtException(e);
        }
    }
}
