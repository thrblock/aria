package com.thrblock.aria.music;

/**
 * Music command in queue
 * <p>
 * 音乐队列指令
 * 
 * @author zepu.li
 *
 */
@FunctionalInterface
interface IMusicCmd {
    /**
     * 运行指令
     */
    public void exec();
}
