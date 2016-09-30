package com.thrblock.aria.decoder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * 解码器接口
 * @author Administrator
 */
public interface IDecoder {
	/**
	 * 由编码流解码为原始流
	 * @param audioInputStream 编码流
	 * @return 原始流
	 */
	public AudioInputStream getDecodedAudioInputStream(AudioInputStream audioInputStream);
	/**
	 * 由编码格式解码为原始格式
	 * @param baseFormat 编码格式
	 * @return 原始格式
	 */
	public AudioFormat getDecodedAudioFormat(AudioFormat baseFormat);
}
