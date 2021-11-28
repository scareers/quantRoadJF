package com.scareers.utils;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ArrayUtil;

import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/29/029-2:37
 */
public class StrUtil {
    public static void main(String[] args) {

    }

    /**
     * hutool 的 StrUtil.format, 调用 CharSequenceUtil.format.  因检测null和空值, 浪费时间. 自己研究时不判定,  可以提高性能
     *
     * @param template
     * @param params
     * @return
     */
    public static String format(String template, Object... params) {
        return formatWith(template, cn.hutool.core.util.StrUtil.EMPTY_JSON, params);
    }

    /**
     * 辅助 format., 删除了 空字符串 等判定
     *
     * @param strPattern
     * @param placeHolder
     * @param argArray
     * @return
     */
    public static String formatWith(String strPattern, String placeHolder, Object... argArray) {
        final int strPatternLength = strPattern.length();
        final int placeHolderLength = placeHolder.length();

        // 初始化定义好的长度以获得更好的性能
        final StringBuilder sbuf = new StringBuilder(strPatternLength + 50);

        int handledPosition = 0;// 记录已经处理到的位置
        int delimIndex;// 占位符所在位置
        for (int argIndex = 0; argIndex < argArray.length; argIndex++) {
            delimIndex = strPattern.indexOf(placeHolder, handledPosition);
            if (delimIndex == -1) {// 剩余部分无占位符
                if (handledPosition == 0) { // 不带占位符的模板直接返回
                    return strPattern;
                }
                // 字符串模板剩余部分不再包含占位符，加入剩余部分后返回结果
                sbuf.append(strPattern, handledPosition, strPatternLength);
                return sbuf.toString();
            }

            // 转义符
            if (delimIndex > 0 && strPattern.charAt(delimIndex - 1) == cn.hutool.core.util.StrUtil.C_BACKSLASH) {// 转义符
                if (delimIndex > 1 && strPattern
                        .charAt(delimIndex - 2) == cn.hutool.core.util.StrUtil.C_BACKSLASH) {// 双转义符
                    // 转义符之前还有一个转义符，占位符依旧有效
                    sbuf.append(strPattern, handledPosition, delimIndex - 1);
                    sbuf.append(cn.hutool.core.util.StrUtil.utf8Str(argArray[argIndex]));
                    handledPosition = delimIndex + placeHolderLength;
                } else {
                    // 占位符被转义
                    argIndex--;
                    sbuf.append(strPattern, handledPosition, delimIndex - 1);
                    sbuf.append(placeHolder.charAt(0));
                    handledPosition = delimIndex + 1;
                }
            } else {// 正常占位符
                sbuf.append(strPattern, handledPosition, delimIndex);
                sbuf.append(cn.hutool.core.util.StrUtil.utf8Str(argArray[argIndex]));
                handledPosition = delimIndex + placeHolderLength;
            }
        }

        // 加入最后一个占位符后所有的字符
        sbuf.append(strPattern, handledPosition, strPatternLength);
        return sbuf.toString();
    }

    public static List<String> split(CharSequence str, CharSequence separator) {
        return cn.hutool.core.util.StrUtil.split(str, separator); // 直接调用
    }
}
