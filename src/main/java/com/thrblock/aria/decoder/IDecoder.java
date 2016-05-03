package com.thrblock.aria.decoder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * 解码器接口
 * @author Administrator
 */
public interface IDecoder {
	public AudioInputStream getDecodedAudioInputStream(AudioInputStream audioInputStream);
	public AudioFormat getDecodedAudioFormat(AudioFormat baseFormat);
}
