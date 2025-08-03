package me.seakeer.learning.javase.oop;

/**
 * PolymorphismExample;
 * 多态示例
 *
 * @author Seakeer;
 * @date 2024/8/31;
 */
public class PolymorphismExample {

    public static void main(String[] args) {

        // 父类引用指向子类实例
        MediaPlayer mediaPlayer = new VideoPlayer();
        // 方法重写：子类重写父类canPlay()方法
        if (mediaPlayer.canPlay("VIDEO")) {
            // 方法重载：多个play方法
            mediaPlayer.play();
            mediaPlayer.play(10, 20);
        }
    }
}


/**
 * 媒体播放器
 */
class MediaPlayer {
    public void play() {
        this.play(0, 100);
    }

    public void play(int fromPercent, int endPercent) {
        System.out.printf("Playing from %d %% to %d %% \n", fromPercent, endPercent);
    }

    public boolean canPlay(String mediaType) {
        return false;
    }
}

/**
 * 视频播放器
 */
class VideoPlayer extends MediaPlayer {
    @Override
    public boolean canPlay(String mediaType) {
        return "VIDEO".equalsIgnoreCase(mediaType);
    }
}