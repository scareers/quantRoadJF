package com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist;

import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.fs.FsFetcher;
import com.scareers.datasource.eastmoney.fs.FsTransactionFetcher;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.display.SecurityDfDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import joinery.DataFrame;
import lombok.SneakyThrows;

import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * description: 1分钟分时图爬虫, 相关数据展示panel
 *
 * @author: admin
 * @date: 2022/2/12/012-12:56:23
 */
public class FsTransFetcherListAndDataPanel extends SecurityListAndTablePanel {
    private static FsTransFetcherListAndDataPanel INSTANCE;

    public static FsTransFetcherListAndDataPanel getInstance(MainDisplayWindow mainDisplayWindow, int jListWidth) {
        if (INSTANCE == null) {
            INSTANCE = new FsTransFetcherListAndDataPanel(mainDisplayWindow, jListWidth);
        }
        return INSTANCE;
    }

    protected FsTransFetcherListAndDataPanel(
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
//        securityEmPos = FsFetcher.getStockPool().stream()
//                .map(SecurityBeanEm.SecurityEmPo::new).collect(Collectors.toCollection(Vector::new));

        while (true) {
            securityEmPos = FsTransactionFetcher.getStockPool().stream()
                    .map(SecurityBeanEm.SecurityEmPo::new).collect(Collectors.toCollection(Vector::new));
            Thread.sleep(100);
        }
    }

    @Override
    protected SecurityDfDisplayPanel buildDisplayPanel() {
        return new DisplayTablePanelFs1MSecurity(this, this.jListWidth);
    }

    public static class DisplayTablePanelFs1MSecurity extends SecurityDfDisplayPanel {
        public DisplayTablePanelFs1MSecurity(
                SecurityListAndTablePanel parent, int listWidth) {
            super(parent, listWidth);
        }

        @Override
        public DataFrame<Object> getShowDf(SecurityBeanEm currentBean) {
            return FsTransactionFetcher.getFsTransData(currentBean);
        }
    }
}
