package com.thrblock.aria.music;

@FunctionalInterface
public interface MusicProgressListener {
	public void progress(long current,long all);
}
