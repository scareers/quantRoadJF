1.jnative -- java调用dll

https://blog.csdn.net/lzwglory/article/details/44653433

下载地址:

JNative_1.4RC2_src.zip : http://jaist.dl.sourceforge.net/sourceforge/jnative/JNative_1.4RC2_src.zip

JNative.jar : http://nchc.dl.sourceforge.net/sourceforge/jnative/JNative.jar

如果以上版本不能完成下载,说明版本有可能更新,请从以下地址中下载:

Resource URL: http://jnative.sourceforge.net/ 

Source Code: http://sourceforge.net/projects/jnative 

Detailed Review: http://jnative.free.fr 

JavaDOC： http://jnative.free.fr/docs/

JNative相对于其它同类开源组件的优点:

1.容易使用

2.对数据类型的处理做的出色

3.支持CallBack

下面以一个小Demo来学习一下JNative:

1.理解文件用途

JNative_1.4RC2_src.zip是JNative源代码的压缩包把它解压后从中找到libJNativeCpp.so和JNativeCpp.dll两个文件.JNativeCpp.dll应用在Windows平台下.把它放在c:\windows\system32目录下.libJNativeCpp.so应用在Linux平台下.放在哪个目录,这个我不知道.

把JNative.jar加入到所需的工程中.

把要调用的dll文件也放在c:\windows\system32目录下, 这个目录存在一个文件，

2.测试类



------------
2.user32.dll 函数

https://blog.csdn.net/chocolateboy/article/details/5493791
https://blog.csdn.net/qq_30122639/article/details/61620324

msdn 文档: 
https://docs.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-findwindowa

https://docs.microsoft.com/zh-cn/windows/win32/winmsg/windows


