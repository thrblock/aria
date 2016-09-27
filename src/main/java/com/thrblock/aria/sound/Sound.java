package com.thrblock.aria.sound;

import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;

import javax.sound.sampled.SourceDataLine;

public class Sound {
    private static final int CACHE_LENGTH = 2 * 1024;
    private SourceDataLine refLine;
    private ExecutorService pool;
    private byte[] decodedSrc;
    protected Sound(SourceDataLine refLine,ExecutorService commonsPool,byte[] decodedSrc) {
        this.refLine = refLine;
        this.pool = commonsPool;
        this.decodedSrc = decodedSrc;
    }
    
    public void play() {
        pool.execute(() -> {
            int offset = 0;
            while(offset < decodedSrc.length) {
                offset += refLine.write(decodedSrc, offset, offset + CACHE_LENGTH > decodedSrc.length?decodedSrc.length - offset:CACHE_LENGTH);
            }
        });
    }
    
    public void loop(int times) {
        pool.execute(() -> {
            for(int i = 0;i < times;i++) {
                int offset = 0;
                while(offset < decodedSrc.length) {
                    offset += refLine.write(decodedSrc, offset, offset + CACHE_LENGTH > decodedSrc.length?decodedSrc.length - offset:CACHE_LENGTH);
                }
            }
        });
    }
    
    public void loopUntil(BooleanSupplier booleanSupplier) {
        pool.execute(() -> {
            while(booleanSupplier.getAsBoolean()) {
                int offset = 0;
                while(offset < decodedSrc.length) {
                    offset += refLine.write(decodedSrc, offset, offset + CACHE_LENGTH > decodedSrc.length?decodedSrc.length - offset:CACHE_LENGTH);
                }
            }
        });
    }
    
    protected void destory() {
        refLine.drain();
        refLine.close();
    }
}
