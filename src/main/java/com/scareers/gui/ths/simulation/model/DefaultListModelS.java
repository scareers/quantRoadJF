package com.scareers.gui.ths.simulation.model;

import javax.swing.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * description: 列表使用的可变model, 新增 刷新功能(检测新的项目并添加), 新增 排序功能,
 * 泛型T 应当实现比较接口,  OrderSimpleDisplay 依据时间戳进行比较
 *
 * @author: admin
 * @date: 2022/1/18/018-09:44:56
 */
public class DefaultListModelS<T extends Comparable> extends DefaultListModel<T> {
    public void flush(List<T> newList) {
        Collections.sort(newList);
        this.clear();
        this.addAll(newList);
    }
}
