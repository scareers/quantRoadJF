package com.scareers.utils.ai.tts;


import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * description: 搜狗和有道 免费tts API接口
 * tts: 即文字转语音, 以播放声音
 * 搜狗翻译api:
 * https://fanyi.sogou.com/reventondc/synthesis?text=%E4%BD%A0%E5%A5%BD%E5%95%8A&speed=1&lang=zh-CHS&from=translateweb&speaker=6
 * // 100不会读作一百
 * text 要转换的文本
 * speed 语速 1~？（我测试到15都还可以） 越大，语速越慢
 * lan=en 英文
 * lan=zh-CHS 中文
 * from 来自
 * speaker 语音类型 1-6的数字
 * 有道api: // 好听一些, 且可读出数字
 * http://tts.youdao.com/fanyivoice?word=guizhou&le=zh&keyfrom=speaker-target
 * <p>
 * 因语音可能太多, 因此不写入到本地
 *
 * @author: admin
 * @date: 2022/2/15/015-19:53:28
 */
public class Tts {
    private static final Log log = LogUtil.getLogger();
    private static final Object soundLock = new Object(); // 声音不重复, 锁

    public static void playSound(String content, boolean async) {
        if (async) {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    playSoundCore(content);
                }
            }, true);
        } else {
            playSoundCore(content);
        }
    }

    public static void playSound(File file, boolean async) {
        if (async) {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    playSoundCore(file);
                }
            }, true);
        } else {
            playSoundCore(file);
        }
    }

    /**
     * 有道tts api, 播放文字
     *
     * @param content
     */
    private static void playSoundCore(String content) {
        synchronized (soundLock) {
            try {
                File file = new File(StrUtil.format("resources/tts/audios/{}.mp3", content));
                HttpUtil.downloadFile(
                        StrUtil.format("http://tts.youdao.com/fanyivoice?word={}&le=zh&keyfrom=speaker-target",
                                content), file, 100000);
                playSoundCore(file);
            } catch (Exception e) {
                log.error("audio error: 语音播放失败,内容: {}", content);
                e.printStackTrace();
            }
        }
    }

    /**
     * 播放本地声音文件
     *
     * @param file
     */
    private static void playSoundCore(File file) {
        synchronized (soundLock) {
            try {
                FileInputStream stream = new FileInputStream(file);
                Player player = new Player(stream);
                player.play();
            } catch (FileNotFoundException | JavaLayerException e) {
                log.error("audio error: 语音播放失败");
                e.printStackTrace();
            }
        }
    }


    //
    public static void main(String[] args)
            throws Exception {
        while (true) {
            playSound("买入贵州茅台, 100股, 资金增加1000.2元", true); // 完美互斥
            playSound("买入贵州茅台,100股", true);
            Thread.sleep(20);
        }
    }
}



/*
使用MagicAudioPlayer
MagicAudioPlayer的使用方法十分容易，只要将其实体化后再使用play方法即可。读取音频文件的方式如下：

File audioFile = new File("/home/magiclen/test.wav");
AudioPlayer player = AudioPlayer.createPlayer(audioFile);
player.play();
也可以用URL当作音频来源：

AudioPlayer player = AudioPlayer.createPlayer(url);
player.play();
如果要暂停、停止播放的话，可以使用pause与stop方法。

player.pause();
player.stop();
如果要设置播放次数以及播放完成后自动关闭并释放资源的话，可以使用setPlayCount和setAutoClose方法，程序如下：

player.setPlayCount(2);
player.setAutoClose(true);
另外还可以使用setVolume和setBalance方法设置播放的音量以及左右声道的平衡，程序如下：

player.setVolume(100);
player.setBalance(100);
如果要取得AudioPlayer目前的状态，可以使用getStatus方法，如下：

AudioPlayer.Status status = player.getStatus();
如果要监听AudioPlayer的状态改变事件，可以使用setStatusChangedListener方法，如下：

player.setStatusChangedListener((previousStatus, currentStatus) -> {
    switch (currentStatus) {
        case OPEN:
            break;
        case START:
            break;
        case STOP:
            break;
        case CLOSE:
            break;
    }
});
当然也可以取得与设置目前播放的位置，如下：

long position = player.getAudioPosition();
//往前一秒
player.setAudioPosition(position + 1000000);

*
* */
