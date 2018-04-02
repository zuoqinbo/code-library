package telnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * 服务端线程类
 *
 * @author ray
 */
public class EchoServerThread implements Runnable {

    private Socket client;

    public EchoServerThread(Socket client) {
        this.client = client;
    }

    public void run() {
        PrintStream out = null;
        BufferedReader in = null;

        try {
            in = new BufferedReader(new InputStreamReader(this.client.getInputStream()));  //获取客户端输入的数据
            out = new PrintStream(this.client.getOutputStream()); //向客户端响应数据

            boolean isEnd = false;
            while (!isEnd) {
                String tmp = in.readLine();
                System.out.println("client echo: " + tmp);
                if (tmp != null && !"bye".equalsIgnoreCase(tmp.trim())) {
                    out.println("server echo: " + tmp);
                } else {
                    isEnd = true;
                }
            }
            in.close();  //关闭客户端输入流
            out.close();  //关闭客户端输出流
            this.client.close();
//与客户端断开连接


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}