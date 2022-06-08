package com.scareers.utils.charts;

/**
 * 十字星线, 其x轴竖线, 位置变化时, 显然对应了 时间轴索引的变化 ! 本接口, 就是index变化的回调!
 * CrossLineListenerForFsXYPlot 和 CrossLineListenerForKLineXYPlot 类均可设置本属性, 则将被自动调用
 * 他们的此属性可以为null, 不做回调
 *
 * @author admin
 */
public interface CrossLineXIndexChangeCallback {
    public abstract void call(int newIndex);
}
