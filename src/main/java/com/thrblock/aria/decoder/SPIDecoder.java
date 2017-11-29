package com.thrblock.aria.decoder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.springframework.stereotype.Component;

/**
 * 解码器，使用SPI交由第三方解码
 * @author thrblock
 */
@Component
public class SPIDecoder implements IDecoder {
    @Override
    public AudioInputStream getDecodedAudioInputStream(AudioInputStream audioInputStream) {
        AudioFormat baseFormat = audioInputStream.getFormat();
        AudioFormat decodedFormat = getDecodedAudioFormat(baseFormat);
        return AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);
    }
    
    @Override
    public AudioFormat getDecodedAudioFormat(AudioFormat baseFormat) {
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,//Encoding
                baseFormat.getSampleRate(),     //SampleRate
                16,                             //SampleSize
                baseFormat.getChannels(),       //Channels
                baseFormat.getChannels() * 2,   //FrameSize
                baseFormat.getSampleRate(),     //FrameRate
                false);
    }
}
