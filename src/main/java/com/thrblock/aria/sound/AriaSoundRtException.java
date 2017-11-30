package com.thrblock.aria.sound;

/**
 * AriaSoundRtException 将多类音效中的exception包装为applevel－runtime异常
 * 
 * <a href="mailto:thrblock@gmail.com">thrblock</a>
 *
 */
public class AriaSoundRtException extends RuntimeException {
    private static final long serialVersionUID = -7437605235587292712L;

    /**
     * warp a exception
     * 包装指定异常
     * 
     * @param e
     *            异常
     */
    public AriaSoundRtException(Exception e) {
        super(e);
    }
}
