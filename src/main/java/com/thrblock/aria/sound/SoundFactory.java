package com.thrblock.aria.sound;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 音效工厂
 * @author Administrator
 *
 */
@Component
@Lazy(true)//Lazy 当系统不需要音频需求时，不会创建线程实例
public class SoundFactory {
	
}