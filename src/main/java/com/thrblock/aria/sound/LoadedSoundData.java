package com.thrblock.aria.sound;

import javax.sound.sampled.SourceDataLine;

public class LoadedSoundData {
	private byte[] decodedSrc;
	private SourceDataLine line;
	public byte[] getDecodedSrc() {
		return decodedSrc;
	}
	public void setDecodedSrc(byte[] decodedSrc) {
		this.decodedSrc = decodedSrc;
	}
	public SourceDataLine getLine() {
		return line;
	}
	public void setLine(SourceDataLine line) {
		this.line = line;
	}
}
