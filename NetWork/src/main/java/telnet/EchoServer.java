package telnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Echo服务端代码
 *
 * @author ray
 */
public class EchoServer {


    public static void main(String[] args) {
        ServerSocket server = null; //服务端实例
        Socket client = null;
//客户端实例

        PrintStream out = null;
//用于响应客户端
        BufferedReader in = null;
//读取客户端发送过来的请求报文

        try {
            server = new ServerSocket(8888); //实例化服务器，并指定端口8888

            while (true) {
                client = server.accept();
//等待客户端连接请求
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));  //获取客户端输入的数据
                out = new PrintStream(client.getOutputStream()); //向客户端响应数据

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
                client.close();
//与客户端断开连接

            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

