package com.scareers.gui.thstrader;


import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RuntimeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.xvolks.jnative.JNative;
import org.xvolks.jnative.Type;

/**
 * description:  Window 句柄, int. 方便理解,与其余的 int 做区分. Integer final了不能继承
 *
 * @author: admin
 * @date: 2021/12/10/010-3:27
 */
@Data
@AllArgsConstructor
public class Window {
    private Integer coreHandleInt;

    public static void main(String[] args) throws Exception {
//        String str = RuntimeUtil.execForStr("ipconfig");
        Process process = RuntimeUtil.exec("calc.exe");
        RuntimeUtil.addShutdownHook(new Runnable() {
            @Override
            public void run() {
                Console.log("同花顺主程序已关闭");
            }
        });
        Window handle = findMainWindow("Afx:00400000:b:00010003:00000006:1D08081D");
        Console.log(handle);
        Console.log(handle.toHexString());

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
    public static Window findMainWindow(String windowClassName, String windowTitle) throws Exception {
        JNative findWindowNative = new JNative("user32.dll", "FindWindowA");
        findWindowNative.setRetVal(Type.INT);
        findWindowNative.setParameter(0, Type.STRING, windowClassName);
        findWindowNative.setParameter(1, Type.STRING, windowTitle);
        findWindowNative.invoke();
        return new Window(Integer.parseInt(findWindowNative.getRetVal()));
    }

    public static Window findMainWindow(String windowClassName) throws Exception {
        return findMainWindow(windowClassName, null);
    }

    public String toHexString() {
        return Integer.toHexString(coreHandleInt);
    }
}