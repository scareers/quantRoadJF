package com.scareers.pandasdummy;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import joinery.DataFrame;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * update:
 * 已经将toSql, 修改为 静态方法.
 * <p>
 * ---
 * 对 joinery.DataFrame 进行修改封装, 使得接近python, 主要是 writeSql 相关方法
 * 1. writeSql 为final方法, 不用即可, 改写为 toSql(), 同python.
 * 2. 原readSql, 仅有 conn和sql, 不可注入参数
 * 3. SqlUtil.execSql  多用于建表等等,修改表结构等, 不需要读取返回的boolean, 因此 可选择传递参数, 控制是否关闭掉连接.
 * 在未来代码需要用到同一个conn对象时, 不需要关闭连接. 传递 closeConn=true 即可.
 * DataFrame 相关sql操作, 默认均不会关闭连接, 如果不再复用conn, 则应当关闭掉连接对象.
 * <p>
 * inaword:
 * 如果某api未传递conn参数, 则自身内部获取conn对象, 并close对象, 谁使用谁关闭;
 * 如果传递conn对象, execSql /Update 可选择是否关闭, 其他情况默认不关闭, 需要手动关闭
 *
 * @param <V> 数据类型, 常用 Object
 */
public class DataFrameS<V> extends joinery.DataFrame<V> {
    public DataFrameS() {
        super();
    }

    public DataFrameS(String... columns) {
        super(columns);
    }

    public DataFrameS(Collection<?> columns) {
        super(columns);
    }

    public DataFrameS(Collection<?> index, Collection<?> columns) {
        super(index, columns);
    }

    public DataFrameS(List<? extends List<? extends V>> data) {
        super(data);
    }

    public DataFrameS(Collection<?> index, Collection<?> columns, List<? extends List<? extends V>> data) {
        super(index, columns, data);
    }

    public ArrayList<String> getColumnsAsString() {
        ArrayList<String> columnsAsString = new ArrayList<>();
        for (Object column : columns()) {
            columnsAsString.add(column.toString());
            // columnsAsString.add(String.class.cast(column));
        }
        return columnsAsString;
    }

    public ArrayList<String> getIndexesAsString() { // 注意调用的是 toString(), 数字也会被转换为String
        ArrayList<String> indexesAsString = new ArrayList<>();
        for (Object index_ : index()) {
            indexesAsString.add(index_.toString());
            // 默认index是 1,2,3,  Integer无法cast到 String
        }
        return indexesAsString;
    }

    public static Log log = LogFactory.get();

    /**
     * joinery.DataFrame 自带 writeSql(), 与python to_sql() 方法差距较大, 且使用较为繁琐. 因此封装一下
     * 仅保留了 if_exists 判定, 和 dtype 指定字段名称的方法.
     * public final void writeSql(final Connection c, final String sql)
     * throws SQLException {
     * writeSql(c.prepareStatement(sql));
     * 原定实现为, 给定未填充数据的  import java.sql.PreparedStatement对象, 将会填充 df 的所有数据, 最后执行保存
     * 即给定了 sql语句形式, writeSql 将填充数据, 最后保存.
     * //        conn.createStatement().execute()/executeQuery/executeUpdate; sql 语句执行
     * 总之: 增加了 ifExists 功能. 可自动建表
     *
     * @param tablename      保存的表名称
     * @param conn           java.sql.Connection 连接对象
     * @param ifExists       如果原表存在, 如何处理? 模拟pandas, 可取值 "fail", "append","replace".
     * @param sqlCreateTable 在 replace 和 append 模式都会用到, 建表语句. append时只在第一次建表.
     *                       因此注意: sql建表形如 create table xx  if not exists;
     *                       append 模式可以不传递, 但必须保证已经建表.  replace 模式必须传递
     *                       至少需要保证 append 时列数 在 原表中全部有;  replace则取决于建表语句
     * @throws SQLException
     */
    public static void toSql(DataFrame<Object> df, String tablename, Connection conn, String ifExists,
                             String sqlCreateTable) throws SQLException {
        //        conn.setAutoCommit(true);
        if (df == null) {
            log.warn("df is null, skip save");
            return;
        }
        if (ifExists == null) {
            ifExists = "fail";
            // 不传递则报错
        }
        if ("fail".equals(ifExists)) {
            throw new SQLException(String.format("数据表已存在,写入失败: %s", "ifExists==fail"));
        } else if ("replace".equals(ifExists)) {
            conn.createStatement().execute(String.format("drop table if exists `%s`", tablename));
            // 删除原来的表
        }
        // replace 和 append 模式, 都需要尝试建表
        if (sqlCreateTable != null) {
            conn.createStatement().execute(sqlCreateTable);
        }


        //        conn.setAutoCommit(false);
        // 将调用prest.executeBatch(), 手动提交
        // 第二个%s是字段列表, 第三个是 相同数量的 ?
        String sqlSave = "insert into `{}`({}) values ({})";
        ArrayList<String> questionMarks = new ArrayList<>();
        for (int i = 0; i < df.columns().size(); i++) {
            questionMarks.add("?");
        }
        ArrayList<String> columnsAsString = new ArrayList<>();
        for (Object column : df.columns()) {
            columnsAsString.add(column.toString());
        }
        sqlSave = StrUtil.format(sqlSave, tablename, String.join(",", columnsAsString),
                String.join(",", questionMarks));
        // 逻辑从 writeSql 底层 抄袭而来. 遍历df 行列, 使用 PreparedStatement 批量插入; 使用的 setObject
        PreparedStatement stmt = conn.prepareStatement(sqlSave);
        for (int r = 0; r < df.length(); r++) {
            for (int c = 1; c <= df.size(); c++) {
                Object value = df.get(r, c - 1);
                stmt.setObject(c, value);
            }
            stmt.addBatch();
            //Console.com.scareers.log(stmt.toString()); //可以看到sql语句
        }

        try {
            stmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
//            Console.com.scareers.log(df);
            throw e;
        } finally {

        }
//        conn.commit();
//        conn.setAutoCommit(true);
        // 注意并未关闭conn, 同原生 writeSql 一样. 这样可以复用 连接
    }

    /**
     * 某些情况下方便查看, 将df, 按照 列 的名称进行排序!
     *
     * @param df
     * @return
     */
    public static DataFrame<Object> sortByColumnName(DataFrame<Object> df) {
        Set<Object> colset = df.columns();
        ArrayList<String> colList = new ArrayList<>();
        for (Object o : colset) {
            colList.add(o.toString());
        }
        DataFrame<Object> newDf = new DataFrame<>();
        colList.sort(Comparator.naturalOrder());
        for (String colName : colList) {
            newDf.add(colName, df.col(colName));
        }
        return newDf;
    }


    public static List<Object> getColAsObjectList(DataFrame<Object> df, Object colNameOrIndex) {
        //System.out.println(df.columns());
        List<Object> col;
        try {
            col = df.col(colNameOrIndex);
        } catch (Exception e) {
            // e.printStackTrace();
            col = df.col(Integer.parseInt(colNameOrIndex.toString()));
        }
        return col; // 可能某些值为null
    }

    public static List<Double> getColAsDoubleList(DataFrame<Object> df, Object colNameOrIndex) {
        List<Object> col = getColAsObjectList(df, colNameOrIndex);
        List<Double> res = new ArrayList<>();
        for (Object o : col) {
            if (o == null) {
                res.add(null);
            } else {
                res.add(Double.valueOf(o.toString()));
            }
        }
        return res;
    }

    public static double[] getColAsDoubleArray(DataFrame<Object> df, Object colNameOrIndex) {
        List<Object> col = getColAsObjectList(df, colNameOrIndex);
        double[] res = new double[df.length()];
        for (int i = 0; i < df.length(); i++) {
            res[i] = Double.parseDouble(col.get(i).toString());
        }
        return res;
    }


    public static List<String> getColAsStringList(DataFrame<Object> df, Object colNameOrIndex) {
        List<Object> col = getColAsObjectList(df, colNameOrIndex);
        List<String> res = new ArrayList<>();
        for (Object o : col) {
            if (o == null) {
                res.add(null);
            } else {
                res.add(o.toString());
            }
        }
        return res;
    }


    public static List<DateTime> getColAsDateList(DataFrame<Object> df, Object colNameOrIndex) {
        List<Object> col = getColAsObjectList(df, colNameOrIndex);
        List<DateTime> res = new ArrayList<>();
        for (Object o : col) {
            if (o == null) {
                res.add(null);
            } else {
                res.add(DateUtil.parse(o.toString()));
            }
        }
        return res;
    }

    public static List<Integer> getColAsIntegerList(DataFrame<Object> df, Object colNameOrIndex) {
        List<Object> col = getColAsObjectList(df, colNameOrIndex);
        List<Integer> res = new ArrayList<>();
        for (Object o : col) {
            if (o == null) {
                res.add(null);
            } else {
                res.add(Integer.valueOf(o.toString()));
            }
        }
        return res;
    }

    public static List<Long> getColAsLongList(DataFrame<Object> df, Object colNameOrIndex) {
        List<Object> col = getColAsObjectList(df, colNameOrIndex);
        List<Long> res = new ArrayList<>();
        for (Object o : col) {
            if (o == null) {
                res.add(null);
            } else {
                res.add(Long.valueOf(o.toString()));
            }
        }
        return res;
    }

    /**
     * dfo 转换为二维列表, 可控制是否包含表头
     * 方便json保存.
     * 反向为: TraderUtil.payloadArrayToDf  // 包含header
     *
     * @param df
     * @param withHeader
     * @return
     */
    public static List<List<Object>> to2DList(DataFrame<Object> dfRaw, boolean withHeader) {
        List<List<Object>> res = new ArrayList<>();

        if (withHeader) {
            res.add(new ArrayList<>(dfRaw.columns()));
        }
        for (int i = 0; i < dfRaw.length(); i++) {
            res.add(dfRaw.row(i));
        }
        return res;
    }

//    public static List<String> getColAsStringList(DataFrame<Object> df, Object colNameOrIndex) {
//        List<Object> col;
//        try {
//            col = df.col(colNameOrIndex);
//        } catch (Exception e) {
//            col = df.col(Integer.parseInt(colNameOrIndex.toString()));
//        }
//        List<String> res = new ArrayList<>();
//        for (Object o : col) {
//            res.add(o.toString());
//        }
//        return res;
//    }

    /**
     * 给定html的 表格元素, 解析为 df. 使用标准格式 // 数据去掉两端空白
     * --> thead --> tr -> th
     * --> tbody --> tr --> td
     * 这里不解析thead和tbody, 解析tr列表, 第一个为表头, 后面为行
     *
     * @param tableNode
     * @return 常规性失败返回null
     */
    public static DataFrame<Object> parseHtmlTable(Element tableNode) {
        Elements trs = tableNode.getElementsByTag("tr");
        if (trs.size() == 0) {
            return null;
        }
        Element headEle = trs.get(0);
        Elements ths = headEle.getElementsByTag("th");
        List<String> columns = new ArrayList<>();
        for (Element th : ths) {
            columns.add(StrUtil.trim(th.text()));
        }

        DataFrame<Object> res = new DataFrame<>(columns);
        for (int i = 1; i < trs.size(); i++) {
            Element rowEle = trs.get(i); // 每行
            Elements tds = rowEle.getElementsByTag("td");
            List<Object> row = new ArrayList<>();
            for (Element td : tds) {
                row.add(StrUtil.trim(td.text()));
            }
            res.append(row);
        }
        return res;
    }

    /**
     * 数据复制, 本身算是浅复制, 也能应对多数情况了 --> 行号已经重置标准
     *
     * @param rawDf
     * @return
     */
    public static DataFrame<Object> copy(DataFrame<Object> rawDf) {
        if (rawDf == null) {
            return null;
        }
        DataFrame<Object> res = new DataFrame<>(rawDf.columns());
        for (int i = 0; i < rawDf.length(); i++) {
            res.append(rawDf.row(i));
        }
        return res.resetIndex();
    }
}



