package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.DateTimePicker;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.JavaBean;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.Consumer;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;

/**
 * description: 操盘/复盘, 往往需要一个时间, 以决定 该时刻 哪些信息应当放入 复盘/操盘报告中
 * 常规情况下, 这个时间应当是 now == DateUtil.date()
 * 本控件, 可控制一个等价时间, 操盘与复盘均使用 此时间, 作为 "NOW" 的含义!
 * 简而言之: 当复选框选中, 则启用 "等价时间设定机制", 若未选中, 则统一返回真实的now!
 *
 * @noti : 非单例模式!  复盘/操盘 就不使用单独的时间了; 所有实例的时间修改, 都将反映到 静态属性 uniqueDatetime 上面
 * 请使用静态方法 getUniqueDatetime 设置.
 * @author: admin
 * @date: 2022/3/18/018-17:42:41
 */
@Getter
public class PlanReviewDateTimeDecider extends JPanel {
    // 维护所有实例.(就2个), 当其中实例改变时间设定时, 其他实例也应当修改时间显示
    public static java.util.List<PlanReviewDateTimeDecider> instanceList = new ArrayList<>();

    public static PlanReviewDateTimeDecider newInstance() {
        PlanReviewDateTimeDecider planReviewDateTimeDecider = new PlanReviewDateTimeDecider();
        instanceList.add(planReviewDateTimeDecider);
        return planReviewDateTimeDecider;
    }

    private static Date uniqueDatetime = DateUtil.date();

    /**
     * 全局核心方法. 获取 操盘复盘等价时间;  当复选框选中, 并选择事件, 默认
     *
     * @return
     */
    public static Date getUniqueDatetime() {
        if (instanceList.size() > 0) {
            PlanReviewDateTimeDecider decider = instanceList.get(0); // 理论上每个实例均被同步
            if (decider.getJCheckBox().isSelected()) { // 被选中时, 返回 被设置的 uniqueDatetime
                return uniqueDatetime;
            }
        }
        return DateUtil.date(); // 否则now
    }

    JCheckBox jCheckBox; // 复选框启用本机制, 否则统一返回真实的now
    JTextField showDate; // 显示时间设定!
    DateTimePicker dateTimePicker; // 第三方时间选择器实现, 它本质上绑定到 showDate1

    private PlanReviewDateTimeDecider() {
        this.setLayout(new FlowLayout(FlowLayout.LEFT)); // 左浮动布局
        jCheckBox = getCommonCheckBox();
        showDate = new JTextField("单击选择日期");
        dateTimePicker = new DateTimePicker("yyyy-MM-dd HH:mm:ss", 160, 200);
        PlanReviewDateTimeDecider decider = this;
        dateTimePicker.setEnable(true).setSelect(uniqueDatetime).changeDateEvent(new Consumer<DateTimePicker>() {
            @Override
            public void accept(DateTimePicker o) {
                if (jCheckBox.isSelected()) {
                    uniqueDatetime = DateUtil.parse(o.getSelect());
                    // 同步所有日期显示
                    for (PlanReviewDateTimeDecider planReviewDateTimeDecider : new ArrayList<>(instanceList)) {
                        if (planReviewDateTimeDecider != decider) {
                            DateTimePicker dateTimePicker = planReviewDateTimeDecider.getDateTimePicker();
                            dateTimePicker.setSelect(uniqueDatetime);
                            dateTimePicker.refresh();
                            planReviewDateTimeDecider.getShowDate().setText(o.getSelect());
                        }
                    }
                    ManiLog.put(StrUtil.format("操盘/复盘 等价时间改变: {}", o.getSelect()));
                }
            }
        }).register(showDate);
        jCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jCheckBox.isSelected()) {
                    uniqueDatetime = DateUtil.parse(dateTimePicker.getSelect());
                }

                for (PlanReviewDateTimeDecider planReviewDateTimeDecider : new ArrayList<>(instanceList)) {
                    // 同步选中状态
                    if (planReviewDateTimeDecider != decider) { // 同步所有选中状态
                        planReviewDateTimeDecider.getJCheckBox().setSelected(jCheckBox.isSelected());
                    }
                    // 当切换到被选中状态, 应当刷新为 当前这个DateTimePicker 时间
                    if (jCheckBox.isSelected()) {
                        DateTimePicker dateTimePicker = planReviewDateTimeDecider.getDateTimePicker();
                        dateTimePicker.setSelect(uniqueDatetime);
                        dateTimePicker.refresh();
                        planReviewDateTimeDecider.getShowDate().setText(dateTimePicker.getSelect());
                    }
                }


            }
        });
        this.add(jCheckBox);
        jCheckBox.setSelected(true);
        this.add(showDate);
    }


    public static JCheckBox getCommonCheckBox() {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        checkBox.setBackground(COLOR_THEME_MINOR);
        checkBox.setForeground(Color.pink);
        return checkBox;
    }

}
