package com.scareers.gui.ths.simulation.strategy.stockselector;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.lang.Console;
import cn.hutool.core.math.MathUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;
import org.hibernate.SessionFactory;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.*;

/**
 * description: 对于消息面加持的可能主线, 尝试选择多概念加身的个股; 经人工筛选极易连板;
 * 1.需给定 行业/概念 列表; 调用问财api, 循环查找多概念加身个股;
 * 2.将访问同花顺二级行业列表以及所有概念列表;
 * 3.将访问热度前200股票, 与主线可能概念列表重合
 *
 * @author: admin
 * @date: 2022/3/27/027-20:52:08
 */
public class WenCaiMultiConceptSelector {
    public static List<String> conceptOrIndustryMainLineList = Arrays.asList(
            "医药商业",
            "化学制药",
            "元宇宙",
            "数字货币",
            "黄金概念",
            "石油加工贸易",
            "房地产开发",
            "盐湖提锂",
            "证券",
            "参股券商",
            "预制菜",
            "猪肉",
            "农业种植",
            "工业大麻",
            "氢能源"

    );     // 主线可能概念列表设定

    public static String resColName = "code"; // 读取问财的哪一列结果, 例如 code, 股票简称
    public static String resColName2 = "股票简称"; // 读取问财的哪一列结果, 例如 code, 股票简称

    public static void main(String[] args) {
        HashMap<String, List<String>> res = hotAndMainLineAnalyze(200);
        String resStr = JSONUtilS.toJsonPrettyStr(res);
        Console.log(resStr);
        HashMap<String, List<String>> res2 = multiConceptAnalyze();
        Console.log(JSONUtilS.toJsonPrettyStr(res2));


    }

    private static HashMap<String, List<String>> multiConceptAnalyze() {
        Console.log("开始多概念分析");
        String[] concepts = new String[conceptOrIndustryMainLineList.size()];
        for (int i = 0; i < conceptOrIndustryMainLineList.size(); i++) {
            concepts[i] = conceptOrIndustryMainLineList.get(i);
        }

        HashSet<String> allConcept = new HashSet<>(ThsDbApi.getConceptNameList(DateUtil.today()));
//        HashSet<String> allIndustry = new HashSet<>(
//                ThsDbApi.getIndustryNameLevel23WithFullCodeMap(DateUtil.today()).keySet());

        HashMap<String, List<String>> res = new HashMap<>();

        List<String[]> strings = MathUtil.combinationSelect(concepts, 2);
        for (String[] string : strings) {
            ThreadUtil.sleep(1000);
            String name1 = string[0];
            String name2 = string[1];

            Console.log("解析: {} -- {}", name1, name2);

            StringBuilder question = new StringBuilder();
            question.append("非创业板;非科创板;非st;");
            if (allConcept.contains(name1)) {
                question.append(StrUtil.format("所属概念包含{}", name1));
            } else {
                question.append(StrUtil.format("所属同花顺行业是{}", name1));
            }
            question.append(";");

            if (allConcept.contains(name2)) {
                question.append(StrUtil.format("所属概念包含{}", name2));
            } else {
                question.append(StrUtil.format("所属同花顺行业是{}", name2));
            }

            String key = name1 + "___" + name2;

            DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery(question.toString());
            if (dataFrame == null || dataFrame.length() == 0) {
                continue;
            }

            List<String> codes = DataFrameS.getColAsStringList(dataFrame, resColName);
            List<String> names = DataFrameS.getColAsStringList(dataFrame, resColName2);

            List<String> value = new ArrayList<>();
            for (int i = 0; i < codes.size(); i++) {
                value.add(StrUtil.format("{} -- {}", codes.get(i), names.get(i)));
            }
            res.put(key, value);


        }

        FileSystemView fsv = FileSystemView.getFileSystemView();
        File com = fsv.getHomeDirectory();
        FileWriter writer = new FileWriter(com.getPath() + "\\多重主线加身.json");
        writer.write(JSONUtilS.toJsonPrettyStr(res));


        return res;
    }

    /**
     * 热度排名前200, 且带有主线概念
     *
     * @return
     */
    private static HashMap<String, List<String>> hotAndMainLineAnalyze(int hot) {
        Console.log("开始带热度主线可能分析");

        HashSet<String> allConcept = new HashSet<>(ThsDbApi.getConceptNameList(DateUtil.today()));
//        HashSet<String> allIndustry = new HashSet<>(
//                ThsDbApi.getIndustryNameLevel23WithFullCodeMap(DateUtil.today()).keySet());

        HashMap<String, List<String>> res = new HashMap<>();
        for (String concept : conceptOrIndustryMainLineList) {
            ThreadUtil.sleep(1000);
            Console.log(concept);
            StringBuilder question = new StringBuilder();
            question.append("非创业板;非科创板;非st;");
            question.append(StrUtil.format("个股热度排名<={}", hot));
            question.append(";");
            if (allConcept.contains(concept)) {
                question.append(StrUtil.format("所属概念包含{}", concept));
            } else {
                question.append(StrUtil.format("所属同花顺行业是{}", concept));
            }

            String key = "hot" + hot + "___" + concept;

            DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery(question.toString());
            if (dataFrame == null || dataFrame.length() == 0) {
                continue;
            }

            List<String> codes = DataFrameS.getColAsStringList(dataFrame, resColName);
            List<String> names = DataFrameS.getColAsStringList(dataFrame, resColName2);

            List<String> value = new ArrayList<>();
            for (int i = 0; i < codes.size(); i++) {
                value.add(StrUtil.format("{} -- {}", codes.get(i), names.get(i)));
            }
            res.put(key, value);
        }

        FileSystemView fsv = FileSystemView.getFileSystemView();
        File com = fsv.getHomeDirectory();
        FileWriter writer = new FileWriter(com.getPath() + StrUtil.format("\\热度主线可能_{}.json", hot));
        writer.write(JSONUtilS.toJsonPrettyStr(res));


        return res;
    }

}
