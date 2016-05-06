package com.thrblock.aria.sound;

import javax.sound.sampled.AudioFormat;

public class AudioFormatInfo {
	final float sampleRate;
	final int channel;
	public AudioFormatInfo(AudioFormat format) {
		this.sampleRate = format.getSampleRate();
		this.channel = format.getChannels();
	}
	
	@Override
	public String toString() {
		return "[channel:"+channel+",sampleRate:"+sampleRate+"]";
	}
	
	
	@Override
	public int hashCode() {
		return (int)sampleRate;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof AudioFormatInfo) {
			AudioFormatInfo another = (AudioFormatInfo) obj;
			return another.channel == this.channel && another.sampleRate == this.sampleRate;
		} else {
			return false;
		}
	}
}
