package com.thrblock.aria.decoder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * Decoder interface
 * <p>
 * 解码器接口
 * 
 * <a href="mailto:thrblock@gmail.com">thrblock</a>
 */
public interface IDecoder {
    /**
     * Get decoded stream from encoded audio stream
     * <p>
     * 由编码流解码为原始流
     * 
     * @param audioInputStream
     *            encoded audio stream 编码流
     * @return decoded stream 原始流
     */
    public AudioInputStream getDecodedAudioInputStream(AudioInputStream audioInputStream);

    /**
     * Get decoded audio format from base audio format
     * <p>
     * 由编码格式解码为原始格式
     * 
     * @param baseFormat
     *            baseFormat 编码格式
     * @return decoded format 原始格式
     */
    public AudioFormat getDecodedAudioFormat(AudioFormat baseFormat);
}
