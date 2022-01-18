package com.scareers.gui.ths.simulation.interact.gui.model;

import com.alee.laf.list.ListDataAdapter;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * description: 列表使用的可变model, 新增 刷新功能(检测新的项目并添加), 新增 排序功能,
 * 泛型T 应当实现比较接口,  OrderSimple 依据时间戳进行比较
 *
 * @author: admin
 * @date: 2022/1/18/018-09:44:56
 */
public class DefaultListModelS<T extends Comparable> extends DefaultListModel<T> {
    public DefaultListModelS() {
        super();
        this.addListDataListener(new ListDataAdapter());
    }

    public void flush(List<T> newList) {
        Collections.sort(newList);

        synchronized (this) {

            for (int i = 0; i < Math.min(newList.size(), this.getSize()); i++) {
                this.set(i, newList.get(i));
            }
            if (newList.size() > this.getSize()) {

                this.addAll(newList.subList(this.getSize(), newList.size()));
            } else if (newList.size() < this.getSize()) {
                this.removeRange(newList.size(), this.getSize() - 1); // 注意大的index需要-1
            }
        }


    }
}
