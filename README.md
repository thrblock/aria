# aria   
Read this in other languages: [简体中文](https://github.com/thrblock/aria/blob/master/README.zh-cn.md).   
Aria is a light-weight java sound system designed to be used in game develop.
It's also contain spring support so it can be easily used in a spring application.

### The project aim
The aim of aria is provide a light-weight and reliable solution for game dev.
 * Music for common background music.   
 * Sound for sound effect.   
 * Based on the SPI decoder,the system can support wav\mp3\ogg now,and you can include many other format like FLAV easily.   
 * Many other features for game dev in the future.   
 
### Design concept   
There are two main abstractions in the system.The Music and Sound.   
    
 * Music   
The Music is defined as the only one main theme in a scene.Usually a music is long enough so we need load a bit and play for each time.   
The api of Music is like init,load(loop),pause,stop etc.
The relevant of Music is MusicPlayer class.   
    
 * Sound   
The Sound is defined as sounds that trigged by some relevant events,which means there are lot's of same or different sounds can be played at one time.   
Compare with music,a sound is short enough so we load them all into memory.   
The api of Sound is like play,loop(for many conditions).The sound playing is also optimized for concurrent environment.   
The relevant of Sound is SondFactory class and Sound class,all the api in Sound are thread-safe.   
    
### Get the latest release   
You need at last java 1.8 to use this system.   
The project is hosted in maven central now.   
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.thrblock.aria/aria-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.thrblock.aria/aria-core/)   
```
<dependency>
    <groupId>com.thrblock.aria</groupId>
    <artifactId>aria-core</artifactId>
    <version>1.1.0</version>
</dependency>
```   

### How to use
 There are two main plans,using spring framework or not.   
 * Plan A - Using spring framework   
 When you want to use java to develop a game,I strong recommand you using a framework to control the lifecycle of you game component like spring   
 All we need to do is config a componet-scan,use xml for example.
```   
 <beans xmlns="...">
     <!-- aria sound components -->
     <context:component-scan base-package="com.thrblock.aria" />
 </beans>
```   
 And then reject in you class,use @Autowired for example.   
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

 * Plan B - without spring   
 Without spring context and we need control lifecycle manuly   
```   
 public class MusicDemo {
     public static void main(String[] args) throws InterruptedException {
         MusicPlayer player = new MusicPlayer(new SPIDecoder());
         player.initMusic(new File("./BackGroundMusic.mp3"));
         player.play(-1);
         Thread.sleep(5000);
         player.stop();
         player.destroy();//do not forget this.
     }
 }
```   
 
### Others
 * For more info see the examples in src/test/java   
 * Contact us: thrblock@gmail.com master@thrblock.com OR badteeth@qq.com   
 
 