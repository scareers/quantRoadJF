package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import org.jdesktop.swingx.JXImageView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;
import static com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout.TOP;

/**
 * description: 同花顺概念行业 k线和分时图显示! 类似问财显示panel
 *
 * @author: admin
 * @date: 2022/4/4/004-02:45:02
 */
public class KlineFsDisplayPanelForThsConcept extends DisplayPanel {
    public static void main(String[] args) {

    }

    public static double fsDivideKLineHeightRate = 365.0 / 276;
    public static int buttonContainerHeight = 30; // 按钮panel高度
    public static int fsImageHeight = 300; // 按钮panel高度


    SecurityBeanEm beanEm; // 资产bean

    JPanel buttonContainerFs;
    JPanel buttonContainerKline;
    JXImageView fsImageView; // 左分时
    JXImageView kLineImageView; // 右k线

    JPanel fsPanel; // 左大容器
    JPanel klinePanel;


    public KlineFsDisplayPanelForThsConcept() {
//        this.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        this.setLayout(new VerticalFlowLayout(TOP)); // 平均分左右两边
        this.setPreferredSize(new Dimension(600,
                (int) (buttonContainerHeight * 2 + fsImageHeight * (1 + fsDivideKLineHeightRate)) + 20));

        initLeftPanel();
        initRightPanel();
        this.add(fsPanel);
        this.add(klinePanel);

    }

    private void initRightPanel() {
        klinePanel = new JPanel();
        klinePanel.setLayout(new BorderLayout());
//        klinePanel.setBorder(BorderFactory.createLineBorder(Color.red, 2));

        buttonContainerKline = new JPanel();
        buttonContainerKline.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonContainerKline.setBackground(COLOR_THEME_MINOR);
        buttonContainerKline.setPreferredSize(new Dimension(800, buttonContainerHeight));

        addCommonKLineButtons();

        kLineImageView = new JXImageView();
        // 默认k线图比分时图 高度要高一些; 大约 365/276
        kLineImageView.setPreferredSize(new Dimension(800, (int) (fsImageHeight * fsDivideKLineHeightRate)));
        kLineImageView.setBackground(Color.white);

        klinePanel.add(buttonContainerKline, BorderLayout.NORTH);
        klinePanel.add(kLineImageView, BorderLayout.CENTER);
        klinePanel.setPreferredSize(
                new Dimension(800, (int) (fsImageHeight * fsDivideKLineHeightRate) + buttonContainerHeight));
    }

    private void addCommonKLineButtons() {
        List<String> names = Arrays.asList("日K", "周K", "月K", "5分", "15分", "30分", "60分");
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);

            JButton klineButton = getCommonButton(name);
            buttonContainerKline.add(klineButton);
            klineButtons.add(klineButton);
            int finalI1 = i;
            klineButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    klineButtonActionListener(getUrlStrOfKline(finalI1), name);
                }
            });
        }

    }

    JButton beforeOpenMarketButton; // 盘前分时按钮, update中, 若bean为指数, 隐藏

    private void initLeftPanel() {
        fsPanel = new JPanel();
        fsPanel.setLayout(new BorderLayout());
//        fsPanel.setBorder(BorderFactory.createLineBorder(Color.red, 2));

        buttonContainerFs = new JPanel();
        buttonContainerFs.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonContainerFs.setBackground(COLOR_THEME_MINOR);
        buttonContainerFs.setPreferredSize(new Dimension(800, buttonContainerHeight));

        beforeOpenMarketButton = getCommonButton("盘前");
        buttonContainerFs.add(beforeOpenMarketButton);
        beforeOpenMarketButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fsButtonActionListener(getUrlStrOfFsBeforeMarketOpen(), "盘前");
            }
        });

        addCommonFsButtons();

        fsImageView = new JXImageView();
        fsImageView.setBackground(Color.red);
        fsImageView.setForeground(Color.red);
        fsImageView.setPreferredSize(new Dimension(800, fsImageHeight));


        fsPanel.add(buttonContainerFs, BorderLayout.NORTH);
        fsPanel.add(fsImageView, BorderLayout.CENTER);
        fsPanel.setPreferredSize(new Dimension(800, buttonContainerHeight + fsImageHeight));

    }

    List<JButton> fsButtons = new ArrayList<>(); // 只包含5个
    List<JButton> klineButtons = new ArrayList<>(); // 只包含5个

    private void addCommonFsButtons() {
        List<String> names = Arrays.asList("一天", "两天", "三天", "四天", "五天");
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);

            JButton fs1d = getCommonButton(name);
            buttonContainerFs.add(fs1d);
            fsButtons.add(fs1d);
            int finalI = i;
            fs1d.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fsButtonActionListener(getUrlStrOfFs(finalI + 1), name);
                }
            });
        }


    }

    private void fsButtonActionListener(String urlStrOfFs, String buttonName) {
        try {
            fsImageView.setImageString(urlStrOfFs);
        } catch (IOException ex) {
            ex.printStackTrace();
            if (beanEm == null) {
                ManiLog.put(StrUtil.format("股票分时图[{}]获取失败: {}", buttonName, "股票为null"));
            } else {
                ManiLog.put(StrUtil.format("股票分时图[{}]获取失败: {}", buttonName, beanEm.getName()));
            }
        }
    }

    private void klineButtonActionListener(String urlStrOfKline, String buttonName) {
        try {
            kLineImageView.setImageString(urlStrOfKline);
        } catch (IOException ex) {
            ex.printStackTrace();
            if (beanEm == null) {
                ManiLog.put(StrUtil.format("股票k线[{}]获取失败: {}", buttonName, "股票为null"));
            } else {
                ManiLog.put(StrUtil.format("股票k线[{}]获取失败: {}", buttonName, beanEm.getName()));
            }
        }
    }

    public String getUrlStrOfFsBeforeMarketOpen() { // 带盘前分时, 只有股票有
        if (beanEm != null && beanEm.isStock()) {
            if (beanEm.isHuA() || beanEm.isHuB()) {
                return StrUtil
                        .format("http://webquotepic.eastmoney.com/GetPic" +
                                        ".aspx?id={}1&imageType=rc&token=44c9d251add88e27b65ed86506f6e5da&rt={}",
                                beanEm.getSecCode(), System.currentTimeMillis());
            } else {
                return StrUtil
                        .format("http://webquotepic.eastmoney.com/GetPic" +
                                        ".aspx?id={}2&imageType=rc&token=44c9d251add88e27b65ed86506f6e5da&rt={}",
                                beanEm.getSecCode(), System.currentTimeMillis());
            }

            // http://webquotepic.eastmoney.com/GetPic.aspx?id=0008582&imageType=rc&token=44c9d251add88e27b65ed86506f6e5da&rt=1649015770025
        }
        return StrUtil
                .format("http://webquotepic.eastmoney.com/GetPic" +
                                ".aspx?id={}x&imageType=rc&token=44c9d251add88e27b65ed86506f6e5da&rt={}",
                        "", System.currentTimeMillis()); // 不显示
    }

    public String getUrlStrOfFs(int dayN) { // 1天分时
        if (beanEm != null) {
            if (dayN == 1) {
                if (beanEm.isIndex()) {
                    return StrUtil
                            .format("http://webquotepic.eastmoney.com/GetPic" +
                                            ".aspx?imageType=r&type=&token=44c9d251add88e27b65ed86506f6e5da&nid" +
                                            "={}&timespan={}",
                                    beanEm.getQuoteId(),
                                    System.currentTimeMillis()
                            );
                } else if (beanEm.isHuA() || beanEm.isHuB()) {
                    return StrUtil
                            .format("http://webquotepic.eastmoney.com/GetPic" +
                                            ".aspx?id={}1&imageType=r&token=44c9d251add88e27b65ed86506f6e5da&rt" +
                                            "={}",
                                    beanEm.getSecCode(),
                                    System.currentTimeMillis()
                            );
                } else if (beanEm.isShenA() || beanEm.isShenB()) {
                    return StrUtil
                            .format("http://webquotepic.eastmoney.com/GetPic" +
                                            ".aspx?id={}2&imageType=r&token=44c9d251add88e27b65ed86506f6e5da&rt" +
                                            "={}",
                                    beanEm.getSecCode(),
                                    System.currentTimeMillis()
                            );
                }
            } else {
                return StrUtil
                        .format("https://webquotepic.eastmoney.com/GetPic" +
                                        ".aspx?imageType=t&type=M{}&token=44c9d251add88e27b65ed86506f6e5da&nid" +
                                        "={}&timespan={}", dayN - 1,
                                beanEm.getQuoteId(),
                                System.currentTimeMillis()
                        );
            }
        }
        return StrUtil
                .format("http://webquotepic.eastmoney.com/GetPic" +
                                ".aspx?id={}x&imageType=rc&token=44c9d251add88e27b65ed86506f6e5da&rt={}",
                        "000001", System.currentTimeMillis()); // 不显示
    }

    public String getUrlStrOfKline(int typeN) { // 7种k线
        if (beanEm != null) {
            // 指数,股票url规律相同, 

            String typeStr = "";
            if (typeN == 0) {
                typeStr = "";
            } else if (typeN == 1) {
                typeStr = "W";
            } else if (typeN == 2) {
                typeStr = "M";
            } else if (typeN == 3) {
                typeStr = "M5";
            } else if (typeN == 4) {
                typeStr = "M15";
            } else if (typeN == 5) {
                typeStr = "M30";
            } else if (typeN == 6) {
                typeStr = "M60";
            }

            return StrUtil.format("http://webquoteklinepic.eastmoney.com/GetPic" +
                            ".aspx?nid={}&UnitWidth=-6&imageType=KXL&EF=&Formula=RSI&AT=0&&type={}&token" +
                            "=44c9d251add88e27b65ed86506f6e5da&_=0.1784729548131334",
                    this.beanEm.getQuoteId(), typeStr
            );
        }
        return StrUtil
                .format("http://webquotepic.eastmoney.com/GetPic" +
                                ".aspx?id={}x&imageType=rc&token=44c9d251add88e27b65ed86506f6e5da&rt={}",
                        "000001", System.currentTimeMillis()); // 不显示
    }

    @Override
    public void update() {
        if (this.beanEm == null) {
            return;
        }
        if (!beanEm.isStock()) { // A股才显示盘前按钮
            beforeOpenMarketButton.setVisible(false);
        } else {
            beforeOpenMarketButton.setVisible(true);
        }
        try {
            fsButtons.get(0).doClick();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            klineButtons.get(0).doClick();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        int heightImageFs = fsImageView.getImage().getHeight(null);
//        Console.log(heightImageFs);
//        if (heightImageFs != -1) {
//            fsImageView.setScale(fsImagePreferHeight * 1.0 / heightImageFs);
//        }
//
//        int heightImageKline = kLineImageView.getImage().getHeight(null);
//        Console.log(heightImageKline);
//        if (heightImageKline != -1) {
//            kLineImageView.setScale(fsImagePreferHeight * 1.0 / heightImageKline);
//        }

//        ThreadUtil.execAsync(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }, true);


    }

    public void update(SecurityBeanEm bean) {
        this.beanEm = bean;
        this.update();
    }

    public static JButton getCommonButton(String text) {
        FuncButton button = ButtonFactory.getButton(text);
        button.setBorder(BorderFactory.createLineBorder(Color.black, 1, true));
//        button.setBackground(Color.black);
        button.setForeground(Color.green);
        button.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        return button;
    }
}
