/**
 * Combinatorics Library 3
 * Copyright 2009-2016 Dmytro Paukov d.paukov@gmail.com
 */
package com.scareers.utils.combinpermu;


import cn.hutool.core.lang.Console;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 因未知原因, org.paukov.combinatorics3 的maven库, idea识别不到里面的类, 只能识别包. 因此这里全部复制包内所有类
 * <p>
 * 生成的流, 单个组合, 是 List<xx> 类型, 例如 Integer, String 均可
 */
public class Generator {

    public static void main(String[] args) {
        ArrayList<List<Integer>> result = new ArrayList<>();
        Generator.combination(1, 2, 3, 4)
                .simple(3)
                .stream()
                .forEach(result::add);
        Console.log(result);

//        ArrayList<List<Integer>> allIndexCombinations = new ArrayList<>();
//        for (int j = 0; j < 7 + 1; j++) {// 单组合取 0,1,2,3,4,5,6,7 个; 8种
//            Generator.combination(0, 1, 2, 3, 4, 5, 6) // 全部可取索引, 7个,固定的
//                    .simple(j)
//                    .stream()
//                    .forEach(allIndexCombinations::add);
//        }
//        Console.log(allIndexCombinations);
//        Console.log(allIndexCombinations.size());


    }

    public static <T> CombinationGenerator<T> combination(T... args) {
        return new CombinationGenerator<>(Arrays.asList(args));
    }

    public static <T> CombinationGenerator<T> combination(Collection<T> collection) {
        return new CombinationGenerator<>(collection);
    }

    public static <T> PermutationGenerator<T> permutation(T... args) {
        return new PermutationGenerator<>(Arrays.asList(args));
    }

    public static <T> PermutationGenerator<T> permutation(Collection<T> collection) {
        return new PermutationGenerator<>(collection);
    }

    public static <T> SubSetGenerator<T> subset(Collection<T> collection) {
        return new SubSetGenerator<>(collection);
    }

    public static <T> SubSetGenerator<T> subset(T... args) {
        return new SubSetGenerator<>(Arrays.asList(args));
    }

    public static IGenerator<List<Integer>> partition(Integer value) {
        return new IntegerPartitionGenerator(value);
    }
}
