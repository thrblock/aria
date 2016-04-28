package com.thrblock.aria.music;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.thrblock.aria.decoder.SPIDecoder;
/**
 * 音乐类<br />
 * 音乐数据不会全部装入内存，每次读入一部分并播放。<br />
 * @author Administrator
 */
@Component
@Lazy(true)//Lazy 当系统不需要音频需求时，不会创建线程实例
public class MusicPlayer implements Runnable {
    @Autowired private SPIDecoder decoder;
    
    private static final int SENCITIVE = 10;
    private static final int LINE_CACHE_LENGTH = 16 * 1024; //16 KB Line Cache
    private static final int DATA_CACHE_LENGTH = 16 * 1024; //16 KB Data Cache
    private static final Logger LOG = LoggerFactory.getLogger(MusicPlayer.class);
    
    private AudioFormat baseFormat;
    private AudioFormat decodedFormat;
    
    private long currentPlayed;
    private long totalLength;
    
    private File srcFile;
    private FloatControl currentContorl;
    private AudioInputStream audioInput;
    
    private boolean playFlag = false;
    private boolean pauseFlag = false;
    private boolean runFlag = true;
    
    private int loopTime = 0;
    
    private byte[] cache = new byte[DATA_CACHE_LENGTH];
    private byte[] skipUse = new byte[DATA_CACHE_LENGTH];
    
    private MusicProgressListener progressListener;
    public MusicPlayer() {
        new Thread(this).start();
    }
    
    public void setProgressListener(MusicProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void initMusic(File srcFile) throws UnsupportedAudioFileException, IOException{
        initMusic(srcFile,true);
    }
    
    private void initMusic(File srcFile,boolean recalcLength) throws UnsupportedAudioFileException, IOException {
        this.srcFile = srcFile;
        AudioInputStream srcInput = AudioSystem.getAudioInputStream(srcFile);
        this.baseFormat = srcInput.getFormat();
        this.decodedFormat = decoder.getDecodedAudioFormat(baseFormat);
        this.audioInput = decoder.getDecodedAudioInputStream(srcInput);
        if(recalcLength) {
            long ts = System.currentTimeMillis();
            totalLength = lengthDetect(srcFile);
            LOG.info("length detect:" + totalLength + ",time use:" + (System.currentTimeMillis() - ts));
            LOG.info("Format decoded:" + decodedFormat);
        }
    }
    
    /**
     * 设置循环次数，循环次数为实际播放次数 + 1
     * @param loopTime 循环次数，-1为永远循环 
     */
    public void play(int loopTime) {
        if(runFlag) {
            this.loopTime = loopTime;
            playFlag = true;
            pauseFlag = false;
        }
    }
    
    
    public void play() {
        play(0);
    }

    public void pause() {
        pauseFlag = true;
    }
    
    public void remuse() {
        pauseFlag = false;
    }
    
    public void stop() {
        playFlag = false;
        pauseFlag = false;
        loopTime = 0;
    }
    
    public void destory() {
        runFlag = false;
        playFlag = false;
        pauseFlag = false;
        loopTime = 0;
    }
    
    public void setVolume(float volume) {
        if(currentContorl != null) {
            currentContorl.setValue(volume);
        }
    }
    
    public float getVolume() {
        if(currentContorl != null) {
            return currentContorl.getValue();
        } else {
            return 0;
        }
    }
    
    public float getMaxVolume() {
        if(currentContorl != null) {
            return currentContorl.getMaximum();
        } else {
            return 0;
        }
    }
    
    public float getMinVolume() {
        if(currentContorl != null) {
            return currentContorl.getMinimum();
        } else {
            return 0;
        }
    }
    
    public long getCurrentPlayed() {
    	return currentPlayed;
    }
    
    public long getTotalLength() {
    	return totalLength;
    }
    
    @Override
    public void run() {
        Thread.currentThread().setName("Aria Music");
        while(runFlag) {
            while(!playFlag) { //等待播放信号
                sleepQuietly(SENCITIVE);
            }
            try(SourceDataLine line = readyLineByFormat()) {//generate data line
                currentContorl = (FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
                playLine(line);
            } catch (LineUnavailableException | IOException e) {
                LOG.info("Exception in line operation:" + e);
            }
            streamCloseQuietly();
            if(loopTime > 0) {
                loopTime --;
                reinit();
            } else if(loopTime == -1) {
                reinit();
            } else {
            	playFlag = false;
            }
        }
    }
    
    private SourceDataLine readyLineByFormat() throws LineUnavailableException {
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class,decodedFormat,LINE_CACHE_LENGTH);
        SourceDataLine sourceDataLine = (SourceDataLine)AudioSystem.getLine(info);
        sourceDataLine.open();
        sourceDataLine.start();
        return sourceDataLine;
    }
    
    private void reinit() {
        try {
            initMusic(srcFile,false);
        } catch (UnsupportedAudioFileException | IOException e) {
            LOG.info("Exception in loop reinit:" + e);
        }
    }
    
    private void playLine(SourceDataLine line) throws IOException {
    	currentPlayed = 0;
        for(int realRead = 0;realRead != -1 && playFlag;realRead = audioInput.read(cache, 0, cache.length)) {
        	currentPlayed += realRead;
            while(pauseFlag) {
                sleepQuietly(SENCITIVE);
            }
            line.write(cache, 0, realRead);
            if(progressListener != null) {
                progressListener.progress(currentPlayed, totalLength);
            }
        }
        playFlag = false;
    }
    
    private void streamCloseQuietly() {
        try {
            audioInput.close();
        } catch (IOException e) {
            LOG.info("IOException in stream close:" + e);
        }
    }
    
    private void sleepQuietly(int milliSecond) {
        try {
            Thread.sleep(milliSecond);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.info("InterruptedException:" + e);
        }
    }
    
    private long lengthDetect(File srcFile) throws UnsupportedAudioFileException, IOException {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(srcFile);
        AudioFormat format = audioInputStream.getFormat();
        
        AudioFormat decodedFormat = decoder.getDecodedAudioFormat(format);
        AudioInputStream decodedStream = decoder.getDecodedAudioInputStream(audioInputStream);
        
        long frames = decodedStream.getFrameLength();
        long result = frames * decodedFormat.getFrameSize();
        
        if(result <= 0) {
            result = 0;
            long realSkip = 0;
            do{
                realSkip = decodedStream.read(skipUse);//使用skip得不到结果也是醉了
                result += realSkip;
            }while(realSkip != -1);
        }
        audioInputStream.close();
        
        return result;
    }
}