package team.se.acommunity;

import java.io.IOException;

public class WkTests {
    public static void main(String[] args) {
        String cmd = "C:/Program Files/wkhtmltopdf/bin/wkhtmltoimage --quality 75 https://www.nowcoder.com c:/Users/97307/Desktop/wk-images/1.png";

        try {
            // 执行这个cmd命令
            // 注意下面这个执行的是操作系统的命令，执行操作系统的命令和这个程序是并发的，是异步的
            // 当告诉操作系统要执行这条命令后，程序会接着执行下面的代码，与此同时操作系统也在执行命令，两个是同时进行的
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
