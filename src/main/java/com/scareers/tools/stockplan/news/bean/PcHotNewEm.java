package com.scareers.tools.stockplan.news.bean;

import joinery.DataFrame;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/6/14/014-08:35:51
 */
@Data
@Entity
@Table(name = "pc_new_hots",
        indexes = {@Index(name = "ordertime_index", columnList = "ordertime"),
                @Index(name = "pushtime_index", columnList = "pushtime"),
                @Index(name = "title_index", columnList = "title"),
                @Index(name = "showtime_index", columnList = "showtime")})
public class PcHotNewEm {
    @Id
    @GeneratedValue // 默认就是auto
    @Column(name = "id", unique = true)
    private Long id;
    @Column(name = "code", columnDefinition = "longtext")
    private String code;
    @Column(name = "title", columnDefinition = "varchar(2048)")
    private String title;
    @Column(name = "digest", columnDefinition = "longtext")
    private String digest;
    @Column(name = "simtitle", columnDefinition = "longtext")
    private String simtitle;
    @Column(name = "titlecolor", columnDefinition = "longtext")
    private String titlecolor;
    @Column(name = "showtime", columnDefinition = "varchar(64)")
    private String showtime;
    @Column(name = "ordertime", columnDefinition = "varchar(64)")
    private String ordertime;
    @Column(name = "pushtime", columnDefinition = "varchar(64)")
    private String pushtime;
    @Column(name = "url", columnDefinition = "longtext")
    private String url;
    @Column(name = "image", columnDefinition = "longtext")
    private String image;
    @Column(name = "author", columnDefinition = "longtext")
    private String author;
    @Column(name = "source", columnDefinition = "longtext")
    private String source;
    @Column(name = "columns", columnDefinition = "longtext")
    private String columns;
    @Column(name = "channels", columnDefinition = "longtext")
    private String channels;
    @Column(name = "interact", columnDefinition = "longtext")
    private String interact;
    @Column(name = "sort")
    private Long sort;
    @Column(name = "type")
    private Long type;
    @Column(name = "briefly", columnDefinition = "longtext")
    private String briefly;
    @Column(name = "relatedObject", columnDefinition = "longtext")
    private String relatedObject;
    @Column(name = "trend", columnDefinition = "double")
    private Double trend;
    @Column(name = "remark", columnDefinition = "longtext")
    private String remark;
    @Column(name = "lastModified", columnDefinition = "datetime")
    private Date lastModified;
    @Column(name = "marked", columnDefinition = "boolean")
    private Boolean marked = false;

    public static List<String> allCol = Arrays
            .asList("id",  "pushtime", "title",  "showtime", "ordertime","digest", "simtitle", "titlecolor","code",
                    "url", "image", "author", "source", "columns", "channels",
                    "interact", "sort", "type",
                    "briefly", "relatedObject", "trend", "remark", "lastModified", "marked"
            ); // 多了id列

    /**
     * list bean 转换为全字段完整df
     *
     * @param news
     * @return
     */
    public static DataFrame<Object> buildDfFromBeanList(List<PcHotNewEm> news) {
        DataFrame<Object> res = new DataFrame<>(allCol);
        for (PcHotNewEm bean : news) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getId());
            row.add(bean.getPushtime());
            row.add(bean.getTitle());
            row.add(bean.getShowtime());
            row.add(bean.getOrdertime());
            row.add(bean.getDigest());
            row.add(bean.getSimtitle());
            row.add(bean.getTitlecolor());
            row.add(bean.getCode());
            row.add(bean.getUrl());
            row.add(bean.getImage());
            row.add(bean.getAuthor());
            row.add(bean.getSource());
            row.add(bean.getColumns());
            row.add(bean.getChannels());
            row.add(bean.getInteract());
            row.add(bean.getSort());
            row.add(bean.getType());


            row.add(bean.getBriefly());
            row.add(bean.getRelatedObject());
            row.add(bean.getTrend());
            row.add(bean.getRemark());
            row.add(bean.getLastModified());
            row.add(bean.getMarked());
            res.append(row);
        }
        return res;
    }


    public PcHotNewEm() {
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PcHotNewEm) {
            PcHotNewEm other = (PcHotNewEm) obj;
            return this.title.equals(other.title) && this.pushtime.equals(other.pushtime);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (pushtime != null ? pushtime.hashCode() : 0);
        return result;
    }
}
