package com.scareers.gui.ths.simulation.interact.gui.model;

import com.alee.laf.list.ListDataAdapter;

import javax.swing.*;
import java.util.List;

/**
 * 泛型不要求可比较
 *
 * @author: admin
 * @date: 2022/1/18/018-09:44:56
 */
public class DefaultListModelS2<T> extends DefaultListModel<T> {
    public DefaultListModelS2() {
        super();
        this.addListDataListener(new ListDataAdapter());
    }

    /**
     * model更新全部传递来的数据. 数量最大数量由调用方控制,非model控制. 且顺序也由调用方控制
     *
     * @param newList
     */
    public void flush(List<T> newList) {
        // 当元素发生变化, 才更新
        for (int i = 0; i < Math.min(newList.size(), this.getSize()); i++) {
            try {
                if (!this.get(i).equals(newList.get(i))) {
                    this.set(i, newList.get(i));
                }
            } catch (Exception ignored) {
            }
        }
        if (newList.size() > this.getSize()) {
            this.addAll(newList.subList(this.getSize(), newList.size()));
        } else if (newList.size() < this.getSize()) {
            this.removeRange(newList.size(), this.getSize() - 1); // 注意大的index需要-1
        }
    }
}
