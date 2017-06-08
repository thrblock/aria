# aria   
aria 是一个轻量级的Java音频处理组件，设计用于游戏开发中的音乐音效处理，可接受各类格式的音频文件，并提供诸多播放策略；此外，aria提供了对Spring的支持，你可以在一个SpringContext下更加容易的使用它。 

### aria 的目标是为游戏开发提供轻量可靠的音频处理方案
 * 抽象的音乐(Music)作为游戏背景音乐的解决方案   
 * 抽象的音效(Sound)作为游戏内各类音效的解决方案   
 * 整合基于SPI的解码策略，目前已支持wav\mp3\ogg等常见格式   
 
### 基本依赖   
 * Java 1.8 及以上
 * SpringFramework 4.2.5.RELEASE
 * slf4j-api 1.7.2
 * 各类SPI解码组件 不再赘述 详见pom.xml
 
### API使用说明   
