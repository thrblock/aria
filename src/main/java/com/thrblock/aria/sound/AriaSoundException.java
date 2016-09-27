package com.thrblock.aria.sound;

/**
 * AriaSoundException
 * 将多类音效中的exception包装为applevel－异常
 * @author user
 *
 */
public class AriaSoundException extends Exception {
	private static final long serialVersionUID = 9023388481695333104L;

	/**
	 * 包装指定异常
	 * @param e 异常
	 */
	public AriaSoundException(Exception e) {
		super(e);
	}
}
