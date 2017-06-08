package com.thrblock.aria.music;

/**
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
