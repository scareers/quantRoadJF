package com.scareers.gui.thstrader;

import cn.hutool.core.lang.Console;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.xvolks.jnative.JNative;
import org.xvolks.jnative.Type;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/10/010-0:40
 */
public class JNativeTest {
    public static void main(String[] args) throws Exception {
//        String str = RuntimeUtil.execForStr("ipconfig");
//        Process process = RuntimeUtil.exec("calc.exe");
//        RuntimeUtil.addShutdownHook(new Runnable() {
//            @Override
//            public void run() {
//                Console.log("程序完成");
//            }
//        });

        Console.log(findMainWindow("Afx:00400000:b:00010003:00000006:1D08081D"));


    }

    /**
     * //@noti: className 由spy++ 等软件查询.  一般不需要传递title, null即可.
     * <p>
     * 依据 className 查找程序主界面.
     *
     * @param windowClassName
     * @param windowTitle
     * @return
     * @throws Exception
     */
    public static Handle findMainWindow(String windowClassName, String windowTitle) throws Exception {
        JNative findWindowNative = new JNative("user32.dll", "FindWindowA");
        findWindowNative.setRetVal(Type.INT);
        findWindowNative.setParameter(0, Type.STRING, windowClassName);
        findWindowNative.setParameter(1, Type.STRING, windowTitle);
        findWindowNative.invoke();
        return new Handle(Integer.parseInt(findWindowNative.getRetVal()));
    }

    public static Handle findMainWindow(String windowClassName) throws Exception {
        return findMainWindow(windowClassName, null);
    }

    @Data
    @AllArgsConstructor
    public static class Handle {
        private int coreHandleInt;
    }

}
