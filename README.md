# aria   
aria 是一个轻量级的Java音频处理组件，设计用于游戏开发中的音乐音效处理，可接受各类格式的音频文件，并提供诸多播放策略；此外，aria提供了对Spring的支持，你可以在一个SpringContext下更加容易的使用它。 

### aria 的目标是为游戏开发提供轻量可靠的音频处理方案
 * 抽象的音乐(Music)作为游戏背景音乐的解决方案   
 * 抽象的音效(Sound)作为游戏内各类音效的解决方案   
 * 整合基于SPI的解码策略，目前已支持wav\mp3\ogg等常见格式   
 * 未来将依然围绕游戏开发，对音频采样、节律分析等提供支持   
 
### 基本依赖   
 * Java 1.8 及以上
 * SpringFramework 4.2.5.RELEASE
 * slf4j-api 1.7.2
 * 各类SPI解码组件 不再赘述 详见pom.xml
 
### 设计理念   
这里简要介绍两种主要的抽象，音乐(Music)与音效(Sound)   
 * 音乐(Music)   
 音乐被定义为某个环境下仅有一个的主旋律，其解码长度较大以至于我们需要分批次载入内存并写入播放设备。   
 音乐的主要API被设计为加载、播放（循环）、暂停、停止等   
 与音乐相关的类是MusicPlayer   
    
 * 音效(Sound)   
 音效被定义为环境中按一定条件顺发的音频，同一时间内可有多个相同或不同的音效同时存在。   
 相对于音乐，音效的解码长度足够短以至于可以将其全部放入内存进行处理。   
 音效的主要API被设计为播放、(各类状况下的附加条件)循环，抽象过程中关心的核心问题是并发环境下的优化。   
 与音效相关的类是SoundFactory及Sound，Sound中的相关API均并行可行且并发安全   
 
### Install   
克隆代码到本地后，使用Maven构造依赖即可   
```
<dependency>
    <groupId>com.thrblock.aria</groupId>
    <artifactId>aria-core</artifactId>
    <version>0.0.1</version>
</dependency>
```   

### 项目引用
 在自己的项目中使用aria有两类方案   
 * Plan A - 使用SpringContext   
 无论出于何种目的，当您尝试使用Java进行游戏开发时，我建议您使用成熟的组件容器来控制模块的加载顺序、依赖注入、声明周期等问题。比如说使用Spring   
 
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
 
 
 
 
 