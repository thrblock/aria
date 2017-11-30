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
 * The music player,which load a bit music raw data into cache and play it.
 * <p>
 * 音乐播放器类
 * <p>
 * 音乐数据不会全部装入内存，每次读入一部分并播放。
 * <p>
 * 
 * <a href="mailto:thrblock@gmail.com">thrblock</a>
 */
@Component
@Lazy(true) // Lazy Load only if needed. 当系统不需要音频需求时，不会创建线程实例
public class MusicPlayer implements Runnable {
    /**
     * SPI Decoder
     * <p>
     * SPI 解码器
     */
    @Autowired
    private IDecoder decoder;

    /**
     * This prievent a empty-full loop in cpu
     * <p>
     * 敏感性参数，防止CPU被循环占满
     */
    private static final int SENCITIVE = 10;
    /**
     * This is the cache size for data line
     * <p>
     * 底层播放缓冲区设置
     */
    private static final int LINE_CACHE_LENGTH = 512 * 1024; // 512 KB Line
                                                             // Cache
    /**
     * This is the cache size for raw data
     * <p>
     * 音频流缓冲区设置
     */
    private static final int DATA_CACHE_LENGTH = 16 * 1024; // 16 KB Data Cache
    /**
     * This is the logger
     * <p>
     * 日志
     */
    private static final Logger LOG = LoggerFactory.getLogger(MusicPlayer.class);

    /**
     * The current audio format
     * <p>
     * 当前解码格式 初始化前可能为空
     */
    private AudioFormat decodedFormat;

    /**
     * The current played(number in byte of raw data)
     * <p>
     * 当前播放的解码字节数，默认为0
     */
    private long currentPlayed;
    /**
     * The total length of raw data
     * <p>
     * 当前音乐的解码字节总数，初始化前为0
     */
    private long totalLength;

    /**
     * Current file
     * <p>
     * 当前的音频文件类
     */
    private File srcFile;
    /**
     * The control pannel,may be null
     * <p>
     * 当前的音量控制面板，初始化前可能为空
     */
    private FloatControl currentContorl;
    /**
     * The audio inputstream
     * <p>
     * 当前的音频输入流 初始化前可能为空
     */
    private AudioInputStream audioInput;

    /**
     * The flag to mark status as playing
     * <p>
     * 播放标记，标志着音频是否处于播放状态（包括暂停态）
     */
    private boolean playFlag = false;
    /**
     * The flag to mark status as pause
     * <p>
     * 暂停标记，标志着音频是否处于暂停状态
     */
    private boolean pauseFlag = false;
    /**
     * The flag to mark status running.
     * <p>
     * 运行状态，标志着模块该模块是否处于运行
     */
    private boolean runFlag = true;

    /**
     * loop time
     * <p>
     * 剩余循环次数，默认为0
     */
    private int loopTime = 0;

    /**
     * Line cache
     * <p>
     * 音频流缓冲区
     */
    private byte[] cache = new byte[DATA_CACHE_LENGTH];
    /**
     * raw data cache
     * <p>
     * 解码字节预估缓冲区
     */
    private byte[] skipUse = new byte[DATA_CACHE_LENGTH];

    /**
     * The command queue
     * <p>
     * 命令队列
     */
    private Queue<IMusicCmd> cmdQueue = new ConcurrentLinkedQueue<>();

    /**
     * The progress listener
     * <p>
     * 进度监听器
     */
    private MusicProgressListener progressListener;

    /**
     * For Spring IOC use only.
     * <p>
     * 仅供Spring IOC容器 使用
     */
    private MusicPlayer() {
        new Thread(this).start();
    }

    /**
     * Build a music player with the given decoder
     * <p>
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
     * set progress listener
     * <p>
     * 设定进度监听器
     * 
     * @param progressListener
     *            进度监听器，可由lambda构造
     */
    public void setProgressListener(MusicProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * init a music file
     * <p>
     * 初始化一个音频文件
     * 
     * @param srcFile
     *            src audio file 音频文件
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
     * init a music file,and check it's raw data length or not,the checking will
     * take some time and make progress listener available
     * <p>
     * 初始化一个音频文件,并根据需要检测解码数据长度,检测需要一定时间并将使进度监听器可用
     * 
     * @param srcFile
     *            audio file 音频文件
     * @param recalcLength
     *            check or not 重计算 长度
     * @throws UnsupportedAudioFileException
     *             when decode not support 解码器不支持此格式时抛出
     * @throws IOException
     *             when a io error IO错误时抛出
     */
    private void initMusic(File srcFile, boolean recalcLength) throws UnsupportedAudioFileException, IOException {
        this.srcFile = srcFile;
        AudioInputStream srcInput = AudioSystem.getAudioInputStream(srcFile);
        AudioFormat baseFormat = srcInput.getFormat();
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
     * play and loop
     * <p>
     * 播放并设置循环次数，循环次数为实际播放次数 + 1
     * 
     * @param loopTime
     *            loop times,-1 means forever 循环次数，-1为永远循环
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
     * play once<p>
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
     * pause<p>
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
     * remuse<p>
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
     * stop<p>
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
     * Auto destroy when use spring<p>
     * 销毁 Spirng IOC控制时自动进行
     */
    @PreDestroy
    public void destroy() {
        cmdQueue.offer(() -> {
            runFlag = false;
            playFlag = false;
            pauseFlag = false;
            loopTime = 0;
        });
    }

    /**
     * set audio volume<p>
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
     * get current volume<p>
     * 获得当前音量，范围参阅相关API
     * 
     * @see #getMinVolume() 获得音量最小值
     * @see #getMaxVolume() 获得音量最大值
     * @return 当前音量
     */
    public float getVolume() {
        if (currentContorl != null) {
            return currentContorl.getValue();
        } else {
            return 0;
        }
    }

    /**
     * get the max volume<p>
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
     * get the min volume<p>
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
     * get current played in bytes of raw data<p>
     * 获得当前解码字节播放数
     * 
     * @return 解码字节播放数
     */
    public long getCurrentPlayed() {
        return currentPlayed;
    }

    /**
     * get current total in bytes of raw data<p>
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