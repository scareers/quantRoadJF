package com.scareers.gui.rabbitmq;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/14/014-13:41
 */
public class SettingsOfRb {
    // 连接项设定
    public static String host = "localhost";
    public static int port = 5672;
    public static String virtualHost = "/";
    public static String username = "scareers";
    public static String password = "liu121812+++";

    // "双通道设定"...

    // @noti: 为了与python代码相似, 变量名就不变为大写了
    // java 发送消息到 python 的单通道设定
    public static String ths_trader_j2p_exchange = "ths_trader_j2p_exchange";
    public static String ths_trader_j2p_queue = "ths_trader_j2p_queue";
    public static String ths_trader_j2p_routing_key = "ths_trader_j2p_routing_key";
    // python 发送消息到 java 的单通道设定
    public static String ths_trader_p2j_exchange = "ths_trader_p2j_exchange";
    public static String ths_trader_p2j_queue = "ths_trader_p2j_queue";
    public static String ths_trader_p2j_routing_key = "ths_trader_p2j_routing_key";

}
