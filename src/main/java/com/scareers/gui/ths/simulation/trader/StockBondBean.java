package com.scareers.gui.ths.simulation.trader;

import com.scareers.datasource.selfdb.HibernateSessionFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.persistence.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/22/022-18:48:38
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "stock_bond_arbitrage_tts")
public class StockBondBean {

    @Id
    @GeneratedValue
    @Column(name = "id", unique = true)
    Long id;
    @Column(name = "stockName", columnDefinition = "varchar(32)")
    String stockName;
    @Column(name = "stockCode", columnDefinition = "varchar(32)")
    String stockCode;
    @Column(name = "bondName", columnDefinition = "varchar(32)")
    String bondName;
    @Column(name = "bondCode", columnDefinition = "varchar(32)")
    String bondCode;
    @Column(name = "hotRank", columnDefinition = "int")
    Integer hotRank;

    @Column(name = "currentDiff", columnDefinition = "double")
    Double currentDiff; // 正股-转债  涨跌幅差距

    public StockBondBean(String stockName, String stockCode, String bondName, String bondCode, Integer hotRank) {
        this.stockName = stockName;
        this.stockCode = stockCode;
        this.bondName = bondName;
        this.bondCode = bondCode;
        this.hotRank = hotRank;
    }

    public static SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactoryOfEastMoney();

    public static void saveBean(StockBondBean bean) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(bean);
        transaction.commit();
        session.close();
    }

}
