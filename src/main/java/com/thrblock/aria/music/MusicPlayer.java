package com.thrblock.aria.music;

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.PreDestroy;
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

import com.thrblock.aria.decoder.IDecoder;

/**
 * 音乐播放器类<br />
 * 音乐数据不会全部装入内存，每次读入一部分并播放。<br />
 * 
 * @author thrblock
 */
@Component
@Lazy(true) // Lazy 当系统不需要音频需求时，不会创建线程实例
public class MusicPlayer implements Runnable {
    /**
     * SPI 解码器
     */
    @Autowired
    private IDecoder decoder;

    /**
     * 敏感性参数，防止CPU被循环占满
     */
    private static final int SENCITIVE = 10;
    /**
     * 底层播放缓冲区设置
     */
    private static final int LINE_CACHE_LENGTH = 512 * 1024; // 512 KB Line
                                                             // Cache
    /**
     * 音频流缓冲区设置
     */
    private static final int DATA_CACHE_LENGTH = 16 * 1024; // 16 KB Data Cache
    /**
     * 日志
     */
    private static final Logger LOG = LoggerFactory.getLogger(MusicPlayer.class);

    /**
     * 当前基础格式 初始化前为空
     */
    private AudioFormat baseFormat;
    /**
     * 当前解码格式 初始化前可能为空
     */
    private AudioFormat decodedFormat;

    /**
     * 当前播放的解码字节数，默认为0
     */
    private long currentPlayed;
    /**
     * 当前音乐的解码字节总数，初始化前为0
     */
    private long totalLength;

    /**
     * 当前的音频文件类
     */
    private File srcFile;
    /**
     * 当前的音量控制面板，初始化前可能为空
     */
    private FloatControl currentContorl;
    /**
     * 当前的音频输入流 初始化前可能为空
     */
    private AudioInputStream audioInput;

    /**
     * 播放标记，标志着音频是否处于播放状态（包括暂停态）
     */
    private boolean playFlag = false;
    /**
     * 暂停标记，标志着音频是否处于暂停状态
     */
    private boolean pauseFlag = false;
    /**
     * 运行状态，标志着模块该模块是否处于运行
     */
    private boolean runFlag = true;

    /**
     * 剩余循环次数，默认为0
     */
    private int loopTime = 0;

    /**
     * 音频流缓冲区
     */
    private byte[] cache = new byte[DATA_CACHE_LENGTH];
    /**
     * 解码字节预估缓冲区
     */
    private byte[] skipUse = new byte[DATA_CACHE_LENGTH];

    /**
     * 命令队列
     */
    private Queue<IMusicCmd> cmdQueue = new ConcurrentLinkedQueue<>();

    /**
     * 进度监听器
     */
    private MusicProgressListener progressListener;

    /**
     * 仅供Spring IOC容器 使用
     */
    private MusicPlayer() {
        new Thread(this).start();
    }

    /**
     * 构造一个播放器 使用指定的解码器实例
     * 
     * @param decoder
     *            解码器实例
     */
    public MusicPlayer(IDecoder decoder) {
        this();
        this.decoder = decoder;
    }

    /**
     * 设定进度监听器
     * 
     * @param progressListener
     *            进度监听器，可由lambda构造
     */
    public void setProgressListener(MusicProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * 初始化一个音频文件
     * 
     * @param srcFile
     *            音频文件
     */
    public void initMusic(File srcFile) {
        cmdQueue.offer(() -> {
            try {
                if (!playFlag) {
                    initMusic(srcFile, true);
                }
            } catch (UnsupportedAudioFileException | IOException e) {
                LOG.info("Exception in init:" + e);
            }
        });
    }

    /**
     * 初始化一个音频文件
     * 
     * @param srcFile
     *            音频文件
     * @param recalcLength
     *            重计算 长度
     * @throws UnsupportedAudioFileException
     *             解码器不支持此格式时抛出
     * @throws IOException
     *             IO错误时抛出
     */
    private void initMusic(File srcFile, boolean recalcLength) throws UnsupportedAudioFileException, IOException {
        this.srcFile = srcFile;
        AudioInputStream srcInput = AudioSystem.getAudioInputStream(srcFile);
        this.baseFormat = srcInput.getFormat();
        this.decodedFormat = decoder.getDecodedAudioFormat(baseFormat);
        this.audioInput = decoder.getDecodedAudioInputStream(srcInput);
        if (recalcLength) {
            long ts = System.currentTimeMillis();
            totalLength = lengthDetect(srcFile);
            LOG.info("length detect:" + totalLength + ",time use:" + (System.currentTimeMillis() - ts));
            LOG.info("Format decoded:" + decodedFormat);
        }
    }

    /**
     * 播放并设置循环次数，循环次数为实际播放次数 + 1
     * 
     * @param loopTime
     *            循环次数，-1为永远循环
     */
    public void play(int loopTime) {
        cmdQueue.offer(() -> {
            if (!playFlag) {
                playFlag = true;
                pauseFlag = false;
                this.loopTime = loopTime;
            }
        });
    }

    /**
     * 播放一次
     */
    public void play() {
        cmdQueue.offer(() -> {
            if (!playFlag) {
                playFlag = true;
                pauseFlag = false;
                loopTime = 0;
            }
        });
    }

    /**
     * 暂停
     */
    public void pause() {
        cmdQueue.offer(() -> {
            if (playFlag && !pauseFlag) {
                pauseFlag = true;
            }
        });
    }

    /**
     * 恢复
     */
    public void remuse() {
        cmdQueue.offer(() -> {
            if (playFlag && pauseFlag) {
                pauseFlag = false;
            }
        });
    }

    /**
     * 停止
     */
    public void stop() {
        cmdQueue.offer(() -> {
            if (playFlag) {
                playFlag = false;
                pauseFlag = false;
                loopTime = 0;
            }
        });
    }

    /**
     * 销毁 Spirng IOC控制时自动进行
     */
    @PreDestroy
    public void destory() {
        cmdQueue.offer(() -> {
            runFlag = false;
            playFlag = false;
            pauseFlag = false;
            loopTime = 0;
        });
    }

    /**
     * 设置音量，音量范围可参阅相关API获得
     * 
     * @param volume
     *            音量
     * @see #getMinVolume() 获得音量最小值
     * @see #getMaxVolume() 获得音量最大值
     */
    public void setVolume(float volume) {
        if (currentContorl != null) {
            currentContorl.setValue(volume);
        }
    }

    /**
     * 获得当前音量，范围参阅相关API
     * 
     * @see #getMinVolume() 获得音量最小值
     * @see #getMaxVolume() 获得音量最大值
     * @return
     */
    public float getVolume() {
        if (currentContorl != null) {
            return currentContorl.getValue();
        } else {
            return 0;
        }
    }

    /**
     * 获得当前音量的最大值
     * 
     * @return 当前音量最大值
     * 
     * @see #setVolume(float) 设置音量
     */
    public float getMaxVolume() {
        if (currentContorl != null) {
            return currentContorl.getMaximum();
        } else {
            return 0;
        }
    }

    /**
     * 获得当前音量的最小值
     * 
     * @return 当前音量最小值
     * 
     * @see #setVolume(float) 设置音量
     */
    public float getMinVolume() {
        if (currentContorl != null) {
            return currentContorl.getMinimum();
        } else {
            return 0;
        }
    }

    /**
     * 获得当前解码字节播放数
     * 
     * @return 解码字节播放数
     */
    public long getCurrentPlayed() {
        return currentPlayed;
    }

    /**
     * 获得当前解码字节总数
     * 
     * @return 解码字节总数
     */
    public long getTotalLength() {
        return totalLength;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Aria Music");
        while (runFlag) {
            while (!playFlag && runFlag) { // 等待播放信号
                processCmdStep();
                sleepQuietly(SENCITIVE);
            }
            try (SourceDataLine line = readyLineByFormat()) {// generate data
                                                             // line
                currentContorl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                playLine(line);
            } catch (LineUnavailableException | IOException e) {
                LOG.info("Exception in line operation:" + e);
            }
            streamCloseQuietly();
            if (loopTime > 0) {
                loopTime--;
                reinit();
            } else if (loopTime == -1) {
                reinit();
            } else {
                playFlag = false;
            }
        }
    }

    private SourceDataLine readyLineByFormat() throws LineUnavailableException {
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat, LINE_CACHE_LENGTH);
        SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        sourceDataLine.open();
        sourceDataLine.start();
        return sourceDataLine;
    }

    private void reinit() {
        try {
            initMusic(srcFile, false);
        } catch (UnsupportedAudioFileException | IOException e) {
            LOG.info("Exception in loop reinit:" + e);
        }
    }

    private void playLine(SourceDataLine line) throws IOException {
        currentPlayed = 0;
        for (int realRead = 0; realRead != -1 && playFlag; realRead = audioInput.read(cache, 0, cache.length)) {
            currentPlayed += realRead;
            while (pauseFlag) {
                processCmdStep();
                sleepQuietly(SENCITIVE);
            }
            processCmdStep();
            line.write(cache, 0, realRead);
            if (progressListener != null) {
                progressListener.progress(currentPlayed, totalLength);
            }
        }
        line.drain();// 一定程度上避免切换时的爆音产生
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

        AudioFormat dFormat = decoder.getDecodedAudioFormat(format);
        AudioInputStream decodedStream = decoder.getDecodedAudioInputStream(audioInputStream);

        long frames = decodedStream.getFrameLength();
        long result = frames * dFormat.getFrameSize();

        if (result <= 0) {
            result = 0;
            long realSkip = 0;
            do {
                result += realSkip;
                realSkip = decodedStream.read(skipUse);// 使用skip得不到结果也是醉了
            } while (realSkip != -1);
        }
        audioInputStream.close();

        return result;
    }

    private void processCmdStep() {
        if (!cmdQueue.isEmpty()) {
            IMusicCmd cmd = cmdQueue.poll();
            if (runFlag) {
                cmd.exec();
            }
        }
    }
}