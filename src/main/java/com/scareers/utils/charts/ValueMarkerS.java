package com.scareers.utils.charts;

import lombok.Getter;
import lombok.Setter;
import org.jfree.chart.plot.ValueMarker;

import java.awt.*;

/**
 * description: 自定义 ValueMarker, 主要添加 Type属性, 以在遍历时, 可以区分不同类型的marker
 *
 * @author: admin
 * @date: 2022/2/24/024-17:14:35
 */
@Setter
@Getter
public class ValueMarkerS extends ValueMarker {
    public enum Type {
        MOUSE_CROSS_MARKER, // 代表鼠标十字星的marker
        CURRENT_PRICE, // 代表当前价格含义的marker
        OTHER, // 其他
        HOLD_COST, // 持仓成本线
    }

    Type type;

    public ValueMarkerS(double value) {
        super(value);
        this.type = Type.OTHER;
    }

    public ValueMarkerS(double value, Paint paint, Stroke stroke) {
        super(value, paint, stroke);
        this.type = Type.OTHER;
    }

    public ValueMarkerS(double value, Paint paint, Stroke stroke, Paint outlinePaint, Stroke outlineStroke,
                        float alpha) {
        super(value, paint, stroke, outlinePaint, outlineStroke, alpha);
        this.type = Type.OTHER;
    }
}
