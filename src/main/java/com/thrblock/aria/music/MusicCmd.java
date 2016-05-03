package com.thrblock.aria.music;

public class MusicCmd {
	public static final int TP_DESTORY = 0xFF;
	
	public static final int TP_INIT = 0x01;
	public static final int TP_PLAY = 0x02;
	public static final int TP_PLAY_LOOP = 0x03;
	public static final int TP_PAUSE = 0x04;
	public static final int TP_REMUSE = 0x05;
	public static final int TP_STOP = 0x06;
	
	private final int type;
	private final Object data;
	public MusicCmd(int type) {
		this.type = type;
		this.data = null;
	}
	
	public MusicCmd(int type,Object data) {
		this.type = type;
		this.data = data;
	}

	public int getType() {
		return type;
	}

	public Object getData() {
		return data;
	}
}
