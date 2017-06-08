package com.thrblock.aria.music;

/**
 * 
 * @author user
 *
 */
@FunctionalInterface
public interface MusicProgressListener {
    /**
     * 
     * @param current
     * @param all
     */
    public void progress(long current, long all);
}
