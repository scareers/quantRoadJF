package com.scareers.gui.ths.simulation.interact.gui.component.combination.state;

import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SecurityPool;
import com.scareers.datasource.eastmoney.fetcher.FsFetcher;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.SecurityListAndDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.display.SecurityDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import lombok.SneakyThrows;

import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * description: 高卖股票列表的 高卖状态列表展示.  核心显示 List<HsState>
 *
 * @author: admin
 * @date: 2022/2/12/012-12:56:23
 */
public class SellStockListAndHsStatePanel extends SecurityListAndDisplayPanel {
    private static SellStockListAndHsStatePanel INSTANCE;

    public static SellStockListAndHsStatePanel getInstance(MainDisplayWindow mainDisplayWindow, int jListWidth) {
        if (INSTANCE == null) {
            INSTANCE = new SellStockListAndHsStatePanel(mainDisplayWindow, jListWidth);
        }
        return INSTANCE;
    }

    protected SellStockListAndHsStatePanel(
            MainDisplayWindow mainDisplayWindow, int jListWidth) {
        super(mainDisplayWindow, jListWidth);
    }

    @SneakyThrows
    @Override
    protected void waitAndFlushSecurityListAsync() {
        try {
            FsFetcher.waitInstanceNotNull();
            FsFetcher.getInstance().waitFirstEpochFinish();
        } catch (TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
        mainDisplayWindow.flushBounds();

        while (true) {
            securityEmPos = SecurityPool.getYesterdayHoldStocks().stream()
                    .map(SecurityBeanEm.SecurityEmPo::new).collect(Collectors.toCollection(Vector::new));
            Thread.sleep(100);
        }
    }

    @Override
    protected SecurityDisplayPanel buildDisplayPanel() {
        return new HsStateListPanel();
    }
}
