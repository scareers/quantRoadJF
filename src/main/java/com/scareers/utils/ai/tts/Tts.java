package com.scareers.utils.ai.tts;


import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpDownloader;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    //
    public static void main(String[] args)
            throws Exception {
//        while (true) {
//            playSound("买入贵州茅台, 100股, 资金增加1000.2元", true); // 完美互斥
//            playSound("买入贵州茅台,100股", true);
//            Thread.sleep(20);
//        }


//        thsAreaBkPoetryPlay(false, 500, 1, 0, 100); // 同花顺地域板块 助记诗 播放, 一般均按同花顺默认排序
        thsIndustryBkPoetryPlay(false, 500, 1, 0, 100); // 同花顺地域板块 助记诗 播放, 一般均按同花顺默认排序

        return;

    }


    /**
     * 同花顺行业板块 助记诗播放, 可指定 两个sleep, 和 全诗区间, 区间已经标准化, 可大于size
     *
     * @param explanationPlay
     * @param sleep1
     * @param sleep2
     * @param start
     * @param end
     */
    public static void thsIndustryBkPoetryPlay(boolean explanationPlay, long sleep1,
                                               long sleep2, int start, int end) {
        String allIndustryBkStr = "种养农农煤油化,化化钢金建建通,专仪电半光其消,汽汽非汽通计白,黑饮食纺服造包,家化中生医医电,燃港公机物房银,保证零贸景酒通,计传综国非工贵,小自电小厨医房,互教其石环美";

        List<List<String>> poetryContent = new ArrayList<>();


        List<String> content1 = new ArrayList<>();
        content1.add("种养农农煤油化");
        content1.addAll(StrUtil.split("种植业与林业,养殖业,农产品加工,农业服务,煤炭开采加工,油气开采及服务,化学原料", ","));

        List<String> content2 = new ArrayList<>();
        content2.add("化化钢金建建通");
        content2.addAll(StrUtil.split("化学制品,化工合成材料,钢铁,金属新材料,建筑材料,建筑装饰,通用设备", ","));

        List<String> content3 = new ArrayList<>();
        content3.add("专仪电半光其消");
        content3.addAll(StrUtil.split("专用设备,仪器仪表,电力设备,半导体及元件,光学光电子,其他电子,消费电子", ","));

        List<String> content4 = new ArrayList<>();
        content4.add("汽汽非汽通计白");
        content4.addAll(StrUtil.split("汽车整车,汽车零部件,非汽车交运,汽车服务,通信设备,计算机设备,白色家电", ","));

        List<String> content5 = new ArrayList<>();
        content5.add("黑饮食纺服造包");
        content5.addAll(StrUtil.split("黑色家电,饮料制造,食品加工制造,纺织制造,服装家纺,造纸,包装印刷", ","));

        List<String> content6 = new ArrayList<>();
        content6.add("家化中生医医电");
        content6.addAll(StrUtil.split("家用轻工,化学制药,中药,生物制品,医药商业,医疗器械,电力", ","));

        List<String> content7 = new ArrayList<>();
        content7.add("燃港公机物房银");
        content7.addAll(StrUtil.split("燃气,港口航运,公路铁路运输,机场航运,物流,房地产开发,银行", ","));

        List<String> content8 = new ArrayList<>();
        content8.add("保证零贸景酒通");
        content8.addAll(StrUtil.split("保险及其他,证券,零售,贸易,景点及旅游,酒店及餐饮,通信服务", ","));

        List<String> content9 = new ArrayList<>();
        content9.add("计传综国非工贵");
        content9.addAll(StrUtil.split("计算机应用,传媒,综合,国防军工,非金属材料,工业金属,贵金属", ","));

        List<String> content10 = new ArrayList<>();
        content10.add("小自电小厨医房");
        content10.addAll(StrUtil.split("小金属,自动化设备,电子化学品,小家电,厨卫电器,医疗服务,房地产服务", ","));

        List<String> content11 = new ArrayList<>();
        content11.add("互教其石环美");
        content11.addAll(StrUtil.split("互联网电商,教育,其他社会服务,石油加工贸易,环保,美容护理", ","));

        poetryContent.add(content1);
        poetryContent.add(content2);
        poetryContent.add(content3);
        poetryContent.add(content4);
        poetryContent.add(content5);
        poetryContent.add(content6);
        poetryContent.add(content7);
        poetryContent.add(content8);
        poetryContent.add(content9);
        poetryContent.add(content10);
        poetryContent.add(content11);
        thsBkPoetryPlay(poetryContent.subList(Math.max(0, start), Math.min(end, poetryContent.size())), explanationPlay,
                sleep1, sleep2);
    }

    /**
     * 同花顺地域板块 助记诗播放, 可指定 两个sleep, 和 全诗区间, 区间已经标准化, 可大于size
     *
     * @param explanationPlay
     * @param sleep1
     * @param sleep2
     * @param start
     * @param end
     */
    public static void thsAreaBkPoetryPlay(boolean explanationPlay, long sleep1,
                                           long sleep2, int start, int end) {
        String allAreaBkStr = "安北重黑福甘深,广广贵海河河湖,湖吉江江辽内宁,青山山陕浦上四,天西新云浙";

        List<List<String>> poetryContent = new ArrayList<>();


        List<String> content1 = new ArrayList<>();
        content1.add("安北重黑福甘深");
        content1.addAll(StrUtil.split("安徽,北京,重庆,黑龙江,福建,甘肃,深圳", ","));

        List<String> content2 = new ArrayList<>();
        content2.add("广广贵海河河湖");
        content2.addAll(StrUtil.split("广东,广西,贵州,海南,河北,河南,湖北", ","));

        List<String> content3 = new ArrayList<>();
        content3.add("湖吉江江辽内宁");
        content3.addAll(StrUtil.split("湖南,吉林,江苏,江西,辽宁,内蒙古,宁夏", ","));

        List<String> content4 = new ArrayList<>();
        content4.add("青山山陕浦上四");
        content4.addAll(StrUtil.split("青海,山东,山西,陕西,浦东,上海,四川", ","));

        List<String> content5 = new ArrayList<>();
        content5.add("天西新云浙");
        content5.addAll(StrUtil.split("天津,西藏,新疆,云南,浙江", ","));

        poetryContent.add(content1);
        poetryContent.add(content2);
        poetryContent.add(content3);
        poetryContent.add(content4);
        poetryContent.add(content5);
        thsBkPoetryPlay(poetryContent.subList(Math.max(0, start), Math.min(end, poetryContent.size())), explanationPlay,
                sleep1, sleep2);
    }


    /**
     * 同花顺板块背诵 助记诗 无限循环播放
     *
     * @param poetryContent   单元素, 是 助记一句七言 + 七个字的解释; 因此sleep有所不同
     * @param sleep1          单七言绝句睡眠
     * @param sleep2          解释之间睡眠, 一般短一点
     * @param explanationPlay 是否播放解释, 若否, 则只播放七言绝句
     */
    public static void thsBkPoetryPlay(List<List<String>> poetryContent, boolean explanationPlay, long sleep1,
                                       long sleep2) {
        while (true) {
            for (List<String> stringList : poetryContent) {
                for (int i = 0; i < stringList.size(); i++) {
                    playSound(stringList.get(i), false);
                    if (i == 0) {
//                        playSound(stringList.get(i), false); // 两遍七言
                    }
                    if (!explanationPlay || stringList.size() == 1) { // 可不播放解释, 只播放七言绝句
                        break;
                    }
                    ThreadUtil.sleep(sleep2);
                }
                ThreadUtil.sleep(sleep1);
            }

        }
    }

    public static void playSound(String content, boolean async) {
        if (async) {
            threadPoolExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    playSoundCoreTts(content);
                }
            }, true);
        } else {
            playSoundCoreTts(content);
        }
    }

    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8, 100, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    public static void playSound(File file, boolean async) {
        if (async) {
            threadPoolExecutor.submit(new Runnable() {
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
     * 从tts api, 下载的音频流, 放入缓存池; 1小时缓存, 最多512; key为播放内容
     */
    public static LRUCache<String, ByteArrayInputStream> ttsSoundCache = CacheUtil.newLRUCache(512, 3600 * 1000);

    /**
     * 有道tts api, 播放文字
     *
     * @param content
     */
    private static void playSoundCoreTts(String content) {
        ByteArrayOutputStream bos = null;
        ByteArrayInputStream bis = ttsSoundCache.get(content);
        synchronized (soundLock) {
            try {
                if (bis == null) {
                    bos = new ByteArrayOutputStream(); // 输出流
                    HttpDownloader.download(
                            StrUtil.format("http://tts.youdao.com/fanyivoice?word={}&le=zh&keyfrom=speaker-target",
                                    content), bos, false, null);
                    bis = new ByteArrayInputStream(bos.toByteArray()); // 输入流，需要源。
                    ttsSoundCache.put(content, bis);
                }
                bis.reset(); // 因为播放底层将读取流, 位置变化, 因此需要重置到位置0
                playSoundCore(bis); // 有缓存将不会下载
            } catch (Exception e) {
                log.error("audio error: 语音播放失败,内容: {}", content);
                e.printStackTrace();
            } finally {
                try {
                    if (bos != null) {
                        bos.close();
                    }
                } catch (IOException e) {

                }
                try {
                    if (bis != null) {
                        bis.close();
                    }
                } catch (IOException e) {

                }

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

    /**
     * 播放声音, 仅仅需要提供 音频输入流
     *
     * @param file
     */
    private static void playSoundCore(InputStream stream) {
        synchronized (soundLock) {
            try {
                Player player = new Player(stream);
                player.play();
            } catch (Exception e) {
                log.error("audio error: 语音播放失败");
                e.printStackTrace();
            }
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
