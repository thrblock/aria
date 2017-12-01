# aria   
aria 是一个轻量级的java音频处理组件，设计用于游戏开发中的音乐音效处理，可接受各类格式的音频文件，并提供诸多播放策略；此外，aria提供了对Spring的支持，你可以在一个SpringContext下更加容易的使用它。 

### aria 的目标是为游戏开发提供轻量可靠的音频处理方案
 * 抽象的音乐(Music)作为游戏背景音乐的解决方案   
 * 抽象的音效(Sound)作为游戏内各类音效的解决方案   
 * 整合基于SPI的解码策略，目前已支持wav\mp3\ogg等常见格式   
 * 未来将依然围绕游戏开发，对音频采样、节律分析等提供支持   
 
### 设计理念   
这里简要介绍两种主要的抽象，音乐(Music)与音效(Sound)   
    
 * 音乐(Music)   
 音乐被定义为某个环境下仅有一个的主旋律，其解码长度较大以至于需要分批次载入内存并写入播放设备。   
 音乐的主要API被设计为加载、播放（循环）、暂停、停止等   
 与音乐相关的类是MusicPlayer   
    
    
 * 音效(Sound)   
 音效被定义为环境中按一定条件下瞬发的音频，同一时间内可有多个相同或不同的音效同时处于播放状态。   
 相对于音乐，音效的解码长度足够短以至于可以将其全部放入内存进行处理。   
 音效的主要API被设计为播放、(各类状况下的附加条件)循环，抽象过程中关心的核心问题是并发环境下的优化。   
 与音效相关的类是SoundFactory及Sound，Sound中的相关API均并行可行且并发安全   
    
### Install   
 需要java1.8或以上版本支持，克隆代码到本地后，使用maven构造依赖即可   
```
<dependency>
    <groupId>com.thrblock.aria</groupId>
    <artifactId>aria-core</artifactId>
    <version>1.1.0</version>
</dependency>
```   

### 项目引用
 在自己的项目中使用aria有两类方案，即使用SpringFramework或使用原生java进行开发。   
 * Plan A - 使用SpringContext   
 无论出于何种目的，当尝试使用java进行游戏开发时，建议使用成熟的组件容器来控制模块的加载顺序、依赖注入、生命周期等问题。例如SpringFramework   
 为Spring配置一个scan包路径，以xml为例：
```   
 <?xml version="1.0" encoding="UTF-8"?>
 <beans xmlns="...">
     <!-- aria sound components -->
     <context:component-scan base-package="com.thrblock.aria" />
 </beans>
```   
 接下来在你的游戏组件中使用依赖注入即可   
```
 @Component
 public class YourGameComponent {
     @Autowired
     MusicPlayer player;
     	
     public void whenYourComponentInit() {
         player.initMusic(new File("./BackGroundMusic.mp3"));
     }
     
     public void whenYourComponentActivited() {
         player.play(-1);//loop forever until stop.
     }
     
     public void whenYourComponentStop() {
         player.stop();
     }
 }
```   

 * Plan B - 使用原生java   
 在不依赖组件容器而使用原生java时，需要手动控制音频组件的生命周期   
```   
 public class MusicDemo {
     public static void main(String[] args) throws InterruptedException {
         MusicPlayer player = new MusicPlayer(new SPIDecoder());
         player.initMusic(new File("./Blast.mp3"));
         player.play(-1);
         Thread.sleep(5000);
         player.stop();
         player.destory();//如果忘记了destory会导致程序无法退出
     }
 }
```   
 
### 其它事项
 * 更多使用实例可见src/test/java中的实例   
 * 请尊重并遵循开源协议规则
 * 意见及建议:thrblock@gmail.com master@thrblock.com OR badteeth@qq.com   
 
 