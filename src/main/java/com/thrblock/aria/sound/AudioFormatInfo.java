package com.thrblock.aria.sound;

import javax.sound.sampled.AudioFormat;

/**
 * 这是一个音频信息标记 与转码有关 本类的存在，是由于AudoiFormat类没有重写equals与hashcode
 * 
 * @author zepu.li
 */
public class AudioFormatInfo {
    final float sampleRate;
    final int channel;

    /**
     * 使用一个AudoiFormat构造一个音频信息标记
     * 
     * @param format
     */
    public AudioFormatInfo(AudioFormat format) {
        this.sampleRate = format.getSampleRate();
        this.channel = format.getChannels();
    }

    @Override
    public String toString() {
        return "[channel:" + channel + ",sampleRate:" + sampleRate + "]";
    }

    @Override
    public int hashCode() {
        return (int) sampleRate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AudioFormatInfo) {
            AudioFormatInfo another = (AudioFormatInfo) obj;
            return another.channel == this.channel && another.sampleRate == this.sampleRate;
        } else {
            return false;
        }
    }
}
