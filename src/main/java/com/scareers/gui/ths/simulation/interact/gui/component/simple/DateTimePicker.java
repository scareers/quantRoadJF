package com.scareers.gui.ths.simulation.interact.gui.component.simple;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * @ClassName DateTimePicker
 * @Author guokai2012
 * @Date 2021/8/1 17:53
 * @Description 时间选择插件
 */
public class DateTimePicker {


    /**
     * 时间选择组件，即在哪个组件上 操作 时间选择
     */
    private JComponent component;

    /**
     * 时间格式，默认时 2021-08-01 17:20，没有秒
     */
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private SimpleDateFormat sdfYm = new SimpleDateFormat("yyyy年MM月");

    /**
     * 日期，默认是当前时间
     */
    private Date select = new Date();

    /**
     * 时间 选择插件大小
     */
    private int width = 200;

    private int height = 200;

    /**
     * 底层 菜单，在该菜单上 绘制功能
     */
    private JPopupMenu popup;
    /**
     * 弹出框，利用弹出框，实现点击其他位置，关闭时间选择
     */
    private JDialog dialog;
    /**
     * 是否启用
     */
    private boolean isEnable = true;
    /**
     * 显示 年月面板
     */
    private TopPanel topPanel;
    /**
     * 日历
     */
    private final Calendar calendar = Calendar.getInstance();
    /**
     * 用于对比使用时的日历
     */
    private final Calendar nowCalendar = Calendar.getInstance();
    /**
     * 显示 日期 面板
     */
    private CenterPanel centerPanel;
    /**
     * 显示 时间面板
     */
    private BottomPanel bottomPanel;
    /**
     * 鼠标 事件（被绑定的组件）
     */
    private MouseAdapter componentMostListener;
    /**
     * 回调函数
     */
    private Consumer<DateTimePicker> callFun;
    private boolean isTimeEnable = true;

    /**
     * 构造方法
     */
    public DateTimePicker() {
    }

    public DateTimePicker(String sdf) {
        this.sdf = new SimpleDateFormat(sdf);
    }

    public DateTimePicker(String sdf, int w, int h) {
        this.sdf = new SimpleDateFormat(sdf);
        this.width = w;
        this.height = h;
    }

    /**
     * textField 注册插件
     */
    public void register(JTextField textField) {
        this.component = textField;
        initDateTimePicker();
    }

    /**
     * Label 注册插件
     */
    public void register(JLabel label) {
        this.component = label;
        initDateTimePicker();
    }

    /**
     * 获取选中的时间
     */
    public String getSelect() {
        return this.sdf.format(select);
    }

    /**
     * 设置 默认选中的时间
     */
    public DateTimePicker setSelect(Date select) {
        this.select = select;
        this.calendar.setTime(this.select);
        return this;
    }

    /**
     * 获取是否启用
     */
    public boolean isEnable() {
        return isEnable;
    }

    /**
     * 设置启用，不启用
     */
    public DateTimePicker setEnable(boolean enable) {
        isEnable = enable;
        reInitDateTimePicker();
        return this;
    }

    /**
     * 获取 是否启用 时间输入框
     */
    public boolean isTimeEnable() {
        return isTimeEnable;
    }

    /**
     * 设置 是否启用时间 输入框
     */
    public DateTimePicker setTimeEnable(boolean timeEnable) {
        isTimeEnable = timeEnable;
        reInitDateTimePicker();
        return this;
    }

    /**
     * 选中后的 回调函数
     */
    public DateTimePicker changeDateEvent(Consumer<DateTimePicker> consumer) {
        this.callFun = consumer;
        return this;
    }

    /**
     * 初始化 组件
     */
    private void initDateTimePicker() {
        if (isEnable) {
            this.componentMostListener = new MouseAdapter() {
                // 鼠标按下抬起
                @Override
                public void mouseReleased(MouseEvent e) {
                    showBasePop();
                }

                // 鼠标划入
                @Override
                public void mouseEntered(MouseEvent e) {
                    component.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }

                // 鼠标划出
                @Override
                public void mouseExited(MouseEvent e) {
                    component.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            };
            this.component.addMouseListener(this.componentMostListener);
            // 初始化时，将值赋值到 绑定的组件
            commit();
            initBasePop();
            initTopPanel();
            initCenterPanel();
            if (isTimeEnable) {
                initBottomPanel();
            }
        }
    }

    /**
     * 重新初始化组件
     */
    private void reInitDateTimePicker() {
        if (null != this.component) {
            this.component.removeMouseListener(this.componentMostListener);
            initDateTimePicker();
        }
    }

    /**
     * 初始化 年月显示面板
     */
    private void initTopPanel() {
        // TODO 这里 要做入参配置
        this.topPanel = new TopPanel(true, true);
        this.topPanel.setBackground(Color.RED);
        this.popup.add(topPanel, BorderLayout.NORTH);
        this.topPanel.updateDate();
    }

    /**
     * 初始化 日期显示面板
     */
    private void initCenterPanel() {
        this.centerPanel = new CenterPanel(1);
        this.centerPanel.setBackground(Color.YELLOW);
        this.popup.add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * 初始化 时间面板
     */
    private void initBottomPanel() {
        this.bottomPanel = new BottomPanel();
        this.bottomPanel.setBackground(Color.MAGENTA);
        this.popup.add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 显示 插件
     */
    private void showBasePop() {
        // 先确定 point点
        Point point = new Point(0, this.component.getHeight());
        // 将 point点，相对与组件 component的点转化成屏幕的点
        SwingUtilities.convertPointToScreen(point, this.component);
        this.dialog.setLocation(point);
        this.dialog.setVisible(true);
        // 将 popup 显示在 dialog上。这样可以 在popup失去焦点时，自动关闭
        this.popup.show(this.dialog, 0, 0);
    }

    /**
     * 初始化 底层 菜单
     */
    private void initBasePop() {
        this.dialog = new JDialog();
        this.dialog.setSize(0, 0);
        // 设置为 没有 关闭按钮
        this.dialog.setUndecorated(true);

        // 创建 弹出菜单，并重写 即将隐藏方法。
        this.popup = new JPopupMenu() {
            @Override
            protected void firePopupMenuWillBecomeInvisible() {
                // 将 dialog也设置为 不可见
                dialog.setVisible(false);
            }
        };
        this.popup.setPopupSize(new Dimension(this.width, this.height));
        // TODO 颜色
        this.popup.setBackground(Color.RED);
        this.popup.setBorder(BorderFactory.createEmptyBorder());
        this.popup.setLayout(new BorderLayout());
    }

    /**
     * 刷新 日期组件
     */
    public void refresh() {
        this.topPanel.updateDate();
        this.centerPanel.updateDate();
        SwingUtilities.updateComponentTreeUI(this.popup);
    }

    /**
     * 选中日期方法
     */
    public void selectDate(int year, int month, int day) {
        Calendar tmp = Calendar.getInstance();
        if (isTimeEnable && null != bottomPanel) {
            int hour = Integer.parseInt(bottomPanel.hourText.getText());
            int min = Integer.parseInt(bottomPanel.minText.getText());
            int sec = Integer.parseInt(bottomPanel.secText.getText());
            tmp.set(year, month, day, hour, min, sec);
        } else {
            tmp.set(year, month, day);
        }
        setSelect(tmp.getTime());
        commit();
        refresh();
    }

    /**
     * 确认 时间胡触发
     */
    public void commit() {
        if (this.component instanceof JTextField) {
            ((JTextField) this.component).setText(sdf.format(select));
        }
        if (this.component instanceof JLabel) {
            ((JLabel) this.component).setText(sdf.format(select));
        }
        if (null != this.callFun) {
            this.callFun.accept(this);
        }
        if (null != this.popup) {
            this.popup.setVisible(false);
        }
    }

    /**
     * 最上面的 面板，显示 年月日，并支持年月左右移动调整
     */
    private class TopPanel extends JPanel {
        private final JPanel leftP = new JPanel();
        private final JPanel rightP = new JPanel();
        // 年选择按钮
        private final boolean yearClick;
        // 月按钮
        private final boolean monthClick;
        private JLabel yearL;
        private JLabel yearR;
        private final JLabel center;
        private JLabel monthL;
        private JLabel monthR;

        private final MouseListener listener = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                // 根据 点击事件，显示值
                JLabel source = (JLabel) e.getSource();
                switch (source.getName()) {
                    case "yearL":
                        lastYear();
                        break;
                    case "yearR":
                        nextYear();
                        break;
                    case "monthL":
                        lastMonth();
                        break;
                    case "monthR":
                        nextMonth();
                        break;
                    default:
                        break;
                }
                refresh();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (yearClick) {
                    yearL.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    yearR.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
                if (monthClick) {
                    monthL.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    monthR.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (yearClick) {
                    yearL.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    yearR.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
                if (monthClick) {
                    monthL.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    monthR.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        };


        public TopPanel(boolean yearClick, boolean monthClick) {
            this.yearClick = yearClick;
            this.monthClick = monthClick;
            setLayout(new BorderLayout());
            this.center = new JLabel(sdfYm.format(select), JLabel.CENTER);
            int hGap = ((width - 100) / 4) - 10;
            this.leftP.setLayout(new FlowLayout(FlowLayout.LEFT, hGap, 5));
            this.rightP.setLayout(new FlowLayout(FlowLayout.RIGHT, hGap, 5));
            intTopPanel();
        }

        private void intTopPanel() {
            if (yearClick) {
                this.yearL = new JLabel("<<", JLabel.CENTER);
                this.yearL.setToolTipText("上一年");
                this.yearL.setName("yearL");
                this.yearL.addMouseListener(this.listener);

                this.yearR = new JLabel(">>", JLabel.CENTER);
                this.yearR.setToolTipText("下一年");
                this.yearR.setName("yearR");
                this.yearR.addMouseListener(this.listener);
            }

            if (monthClick) {
                this.monthL = new JLabel("<", JLabel.CENTER);
                this.monthL.setToolTipText("上个月");
                this.monthL.setName("monthL");
                this.monthL.addMouseListener(this.listener);

                this.monthR = new JLabel(">", JLabel.CENTER);
                this.monthR.setToolTipText("下个月");
                this.monthR.setName("monthR");
                this.monthR.addMouseListener(this.listener);
            }

            if (yearClick && monthClick) {
                this.leftP.add(this.yearL);
                this.leftP.add(this.monthL);
                this.rightP.add(this.monthR);
                this.rightP.add(this.yearR);
                this.leftP.setVisible(true);
                this.rightP.setVisible(true);
            } else if (yearClick) {
                this.leftP.add(this.yearL);
                this.rightP.add(this.yearR);
                this.leftP.setVisible(true);
                this.rightP.setVisible(true);
            } else if (monthClick) {
                this.leftP.add(this.monthL);
                this.rightP.add(this.monthR);
                this.leftP.setVisible(true);
                this.rightP.setVisible(true);
            } else {
                this.leftP.setVisible(false);
                this.rightP.setVisible(false);
            }

            add(this.leftP, BorderLayout.WEST);
            add(this.center, BorderLayout.CENTER);
            add(this.rightP, BorderLayout.EAST);
            setVisible(true);
        }

        @Override
        public void setBackground(Color bg) {
            super.setBackground(bg);
            if (null != this.leftP) {
                this.leftP.setBackground(bg);
            }
            if (null != rightP) {
                this.rightP.setBackground(bg);
            }
        }

        // 年月 上下翻方法
        private void nextMonth() {
            calendar.add(Calendar.MONTH, 1);
        }

        private void lastMonth() {
            calendar.add(Calendar.MONTH, -1);
        }

        private void nextYear() {
            calendar.add(Calendar.YEAR, 1);
        }

        private void lastYear() {
            calendar.add(Calendar.YEAR, -1);
        }

        // 更新 年月 显示
        public void updateDate() {
            this.center.setText(sdfYm.format(calendar.getTime()));
        }
    }

    /**
     * 日期 插件，中间选择日期功能
     */
    private class CenterPanel extends JPanel {
        // 默认 星期天，为一周的第一天
        private final int startWeek;
        // 默认 星期日为第一天，即0下标
        private final String[] weekName = {"日", "一", "二", "三", "四", "五", "六"};
        private final List<MyLabel> myLabelList = new ArrayList<>();

        public CenterPanel(int startWeek) {
            this.startWeek = startWeek > 7 ? 6 : (Math.max(startWeek, 0));
            setLayout(new GridLayout(7, 7, 5, 5));
            initCenterPanel();
        }

        private void initCenterPanel() {
            // 表格第一行，显示 星期
            // 重切数组
            cutArrays();
            for (String s : this.weekName) {
                add(new JLabel(s, JLabel.CENTER));
            }
            updateDate();
            setVisible(true);
        }

        // 绘制 JLable 日期组件
        private void updateDate() {
            removeAllJLabel();
            Date temp = calendar.getTime();
            Calendar tmpCalendar = Calendar.getInstance();
            tmpCalendar.setTime(temp);
            tmpCalendar.set(Calendar.DAY_OF_MONTH, 1);
            int index = tmpCalendar.get(Calendar.DAY_OF_WEEK);
            // 先通过 计算，将星期和日期能够在表格中对其
            int sum = (index == 1 ? 8 : index) - startWeek;
            // 再处理一下，对于特殊值的问题
            if (sum == 1) {
                sum = 8;
            } else if (sum < 0) {
                sum += 7;
            } else if (sum == 0) {
                sum = 7;
            }
            tmpCalendar.add(Calendar.DAY_OF_MONTH, -sum);
            for (int i = 0; i < 42; i++) {
                tmpCalendar.add(Calendar.DAY_OF_MONTH, 1);
                MyLabel myLabel = new MyLabel(tmpCalendar.get(Calendar.YEAR),
                        tmpCalendar.get(Calendar.MONTH), tmpCalendar.get(Calendar.DAY_OF_MONTH));
                myLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        MyLabel myLabel = (MyLabel) e.getSource();
                        selectDate(myLabel.year, myLabel.month, myLabel.day);
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        myLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        myLabel.isHover = true;
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        myLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        myLabel.isHover = false;
                    }
                });
                myLabelList.add(myLabel);
                add(myLabel);
            }
        }

        // 删除 日期 JLable组件
        private void removeAllJLabel() {
            for (MyLabel myLabel : myLabelList) {
                remove(myLabel);
            }
        }


        @Override
        public void setBackground(Color bg) {
            super.setBackground(bg);
        }

        // 处理数组，按照 指定的开始星期处理
        private void cutArrays() {
            String[] e = Arrays.copyOfRange(this.weekName, this.startWeek, this.weekName.length);
            String[] s = Arrays.copyOfRange(this.weekName, 0, this.startWeek);
            System.arraycopy(e, 0, this.weekName, 0, e.length);
            System.arraycopy(s, 0, this.weekName, e.length, s.length);
        }

        /**
         * 自定义 JLabel
         */
        private class MyLabel extends JLabel {
            private final int year;
            private final int month;
            private final int day;
            private boolean opaque = false;
            private Border currBorder = null;
            public boolean isHover;

            public MyLabel(int year, int month, int day) {
                super("" + day, JLabel.CENTER);
                this.year = year;
                this.month = month;
                this.day = day;
                nowCalendar.set(year, month, day);

                // 当月的天设置为黑色，否则设置为 亮灰色
                if (this.month == calendar.get(Calendar.MONTH)) {
                    // 将当月 周六、周日 标记为红色
                    if (nowCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || nowCalendar
                            .get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        this.setForeground(Color.RED);
                    } else {
                        this.setForeground(Color.BLACK);
                    }
                } else {
                    this.setForeground(Color.LIGHT_GRAY);
                }

                // 设置选择的日期
                nowCalendar.setTime(select);
                if (this.day == nowCalendar.get(Calendar.DAY_OF_MONTH) && this.month ==
                        nowCalendar.get(Calendar.MONTH) && this.year == nowCalendar.get(Calendar.YEAR)) {
                    this.opaque = true;
                    setOpaque(this.opaque);
                    this.setBackground(Color.CYAN);
                    this.setForeground(Color.WHITE);
                }

                // 设置当前日期，给出红色边框
                // 先把日历重置回来
                nowCalendar.setTime(new Date());
                if (this.year == nowCalendar.get(Calendar.YEAR) && this.month == nowCalendar.get(Calendar.MONTH) &&
                        this.day == nowCalendar.get(Calendar.DAY_OF_MONTH)) {
                    this.currBorder = BorderFactory.createLineBorder(Color.RED);
                    this.setBorder(this.currBorder);
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                // 如果被选中了就画出一个虚线框出来
                if (isHover) {
                    Stroke s = new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
                            BasicStroke.JOIN_BEVEL, 1.0f,
                            new float[]{2.0f, 2.0f}, 1.0f);
                    Graphics2D gd = (Graphics2D) g;
                    gd.setStroke(s);
                    // 设置虚线颜色
                    gd.setColor(Color.BLACK);
                    Polygon p = new Polygon();
                    p.addPoint(0, 0);
                    p.addPoint(getWidth() - 1, 0);
                    p.addPoint(getWidth() - 1, getHeight() - 1);
                    p.addPoint(0, getHeight() - 1);
                    gd.drawPolygon(p);
                    // 针对 选中日期 & 当前日期 已存在边框的，做特殊处理
                    if (null != this.currBorder) {
                        this.setBorder(null);
                    }
                    if (this.opaque) {
                        this.setOpaque(false);
                    }
                } else {
                    this.setBorder(this.currBorder);
                    this.setOpaque(this.opaque);
                }
                super.paintComponent(g);
                this.repaint();
            }
        }
    }

    /**
     * 最地下，设置事件
     */
    private class BottomPanel extends JPanel {
        private JTextField hourText;
        private JTextField minText;
        private JTextField secText; // 自定义添加, 秒数

        public BottomPanel() {
            super();
            initBottomPanel();
        }

        // 初始化 组件
        private void initBottomPanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            int col = Math.floorDiv(((width - 110) >> 1), 11);
            JLabel title = new JLabel("时间", JLabel.CENTER);

            // 小时文本框
            this.hourText = new JTextField(null, col);
            // 设置 小时显示
            this.hourText.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            this.hourText.setHorizontalAlignment(SwingConstants.CENTER);
            // 设置 自定义Document，主要用户控制输入和删除问题
            MyDocument docHour = new MyDocument(23);
            this.hourText.setDocument(docHour);
            // 设置好document后，才可以设置默认text，否则无法显示
            docHour.insertString(0, String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)), null);
            // 禁止赋值/粘贴
            this.hourText.setTransferHandler(null);

            // 时间分隔符
            JLabel timeSeparator = new JLabel(":", JLabel.CENTER);
            timeSeparator.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));

            // 分钟文本
            this.minText = new JTextField(null, col);
            this.minText.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            this.minText.setHorizontalAlignment(SwingConstants.CENTER);
            // 设置 自定义Document，主要用户控制输入和删除问题。
            MyDocument docMin = new MyDocument(59);
            this.minText.setDocument(docMin);
            docMin.insertString(0, String.valueOf(calendar.get(Calendar.MINUTE)), null);
            // 禁止赋值/粘贴
            this.minText.setTransferHandler(null);

            // 时间分隔符2
            JLabel timeSeparator2 = new JLabel(":", JLabel.CENTER);
            timeSeparator2.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));

            // @noti: 自增秒数
            this.secText = new JTextField(null, col);
            this.secText.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            this.secText.setHorizontalAlignment(SwingConstants.CENTER);
            // 设置 自定义Document，主要用户控制输入和删除问题。
            MyDocument docSec = new MyDocument(59);
            this.secText.setDocument(docSec);
            docSec.insertString(0, String.valueOf(calendar.get(Calendar.SECOND)), null);
            // 禁止赋值/粘贴
            this.secText.setTransferHandler(null);

            add(title);
            add(hourText);
            add(timeSeparator);
            add(minText);
            add(timeSeparator2);
            add(secText);
            setVisible(true);
        }

        // 设置 自定义Document，主要用户控制输入和删除问题
        private class MyDocument extends PlainDocument {
            private int max;
            private DecimalFormat dcf = new DecimalFormat("00");

            public MyDocument(int max) {
                this.max = max;
            }

            @Override
            public void insertString(int offs, String str, AttributeSet a) {
                int i = checkInput(offs, str);
                if (i < 0 || i > max) {
                    return;
                }
                try {
                    remove(0, getLength());
                    String s = dcf.format(i);
                    super.insertString(0, s, a);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }

            /**
             * 输入验证
             * 文本框已输入的值，不能超过max值。
             */
            private int checkInput(int offs, String str) {
                try {
                    StringBuilder text = new StringBuilder(getText(0, getLength())).insert(offs, str);
                    String s = StrUtil.trim(text.toString());
                    return Integer.parseInt(s);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException | BadLocationException e) {
                    e.printStackTrace();
                    return -1;
                }
            }
        }
    }


    public static void main(String[] args) {
        DateTimePicker dateTimePicker = new DateTimePicker("yyyy-MM-dd HH:mm:ss.SSS", 160, 200);

        JTextField showDate1 = new JTextField("单击选择日期");
        dateTimePicker.setEnable(true).setSelect(new Date()).changeDateEvent(new Consumer<DateTimePicker>() {
            @Override
            public void accept(DateTimePicker o) {
                System.out.println(o.getSelect());
            }
        }).register(showDate1);


        DateTimePicker dateTimePicker1 = new DateTimePicker("yyyy-MM-dd", 200, 200);
        JLabel label = new JLabel("单击选中日期");
        dateTimePicker1.setEnable(true).setTimeEnable(false).changeDateEvent(new Consumer<DateTimePicker>() {
            @Override
            public void accept(DateTimePicker o) {
                System.out.println(o.getSelect());
            }
        }).register(label);

        JFrame jf = new JFrame("测试日期选择器");
        jf.add(showDate1, BorderLayout.NORTH);
        jf.add(label, BorderLayout.SOUTH);
        jf.setBounds(500, 500, 500, 500);
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
