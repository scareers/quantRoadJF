package com.scareers.tools.stockplan.bean;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.tools.stockplan.bean.dao.SimpleNewEmDao;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.persistence.*;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.TemporalType.TIMESTAMP;

/**
 * description: 资讯面个人总结 bean
 *
 * @author: admin
 * @date: 2022/3/17/017-19:12:42
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "new_aspect_summary")
public class NewAspectSummary {

    public static void main(String[] args) {
        NewAspectSummary bean = new NewAspectSummary();
        bean.setRemark("测试bean");
        bean.setGeneratedTime(DateUtil.date());

        SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactoryOfEastMoney();
        Session session = sessionFactory.openSession();

        Transaction transaction = session.beginTransaction();
        session.save(bean);
        transaction.commit();

    }


    @Id
    @GeneratedValue // 自动就是auto
    @Column(name = "id", unique = true)
    Long id;
    @Column(name = "type", length = 64)
    String type; // 操盘? 复盘?
    @Column(name = "generatedTime", columnDefinition = "datetime")
    Date generatedTime; // 首次初始化 (new)
    @Column(name = "lastModified", columnDefinition = "datetime")
    Date lastModified; // 手动修改最后时间;

    @Column(name = "dateStr", length = 32)
    String dateStr; // 简单日期字符串
    @Column(name = "trend")
    Double trend; // -1.0 - 1.0 利空利好偏向自定义
    @Column(name = "remark", columnDefinition = "longtext")
    String remark; // 备注

    // 维护四大列表, 为了数据库便于维护, 不使用外键方式, 简单保存 jsonStr;
    // 这些字段均不保存到数据库
    @Transient
    List<String> bullishViews = new ArrayList<>(); // 看涨因素
    @Transient
    List<String> bearishViews = new ArrayList<>(); // 看跌因素
    @Transient
    List<String> neutralViews = new ArrayList<>(); // 中性因素
    @Transient
    List<String> otherViews = new ArrayList<>(); // 其他因素

    // 与之对应的4大字符串. 这些字符串不手动设定, 当每次修改列表时, 将自动转换json, 自动设置!
    @Column(name = "bullishViews", columnDefinition = "longtext")
    String bullishViewsJsonStr = "";
    @Column(name = "bearishViews", columnDefinition = "longtext")
    String bearishViewsJsonStr = "";
    @Column(name = "neutralViews", columnDefinition = "longtext")
    String neutralViewsJsonStr = "";
    @Column(name = "otherViews", columnDefinition = "longtext")
    String otherViewsJsonStr = "";


}
