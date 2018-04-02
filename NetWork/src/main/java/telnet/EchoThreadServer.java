package telnet;
        import java.io.IOException;
        import java.net.ServerSocket;
        import java.net.Socket;

public class EchoThreadServer {
    public static void main(String[] args) {

        ServerSocket server = null;
        Socket client = null;

        try {
            server = new ServerSocket(8888);
            while (true) {
                System.out.println("服务器运行，等待客户端连接...");
                client = server.accept();

//启动服务器线程处理类
                new Thread(new EchoServerThread(client)).start();
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