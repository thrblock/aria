package com.thrblock.aria.music;

/**
 * The music progress listener
 * <p>
 * 进度监听器
 * @author <a href="mailto:thrblock@gmail.com">thrblock</a>
 *
 */
@FunctionalInterface
public interface MusicProgressListener {
    /**
     * @param current 当前进度
     * @param all 总进度
     */
    public void progress(long current, long all);
}
