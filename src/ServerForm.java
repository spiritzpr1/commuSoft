import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public class ServerForm extends JFrame {
    private JList<String> list;
    private JTextArea area;
    private DefaultListModel<String> lm;

    public ServerForm() {
        JPanel p = new JPanel(new BorderLayout());
        // 最右边的用户在线列表
        lm = new DefaultListModel<String>();
        list = new JList<String>(lm);
        JScrollPane js = new JScrollPane(list);
        Border border = new TitledBorder("在线");
        js.setBorder(border);
        Dimension d = new Dimension(100, p.getHeight());
        js.setPreferredSize(d);// 设置位置
        p.add(js, BorderLayout.EAST);

        // 通知文本区域
        area = new JTextArea();
        //area.setEnabled(false);//不能选中和修改
        area.setEditable(false);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        this.getContentPane().add(p);

        // 添加菜单项
        JMenuBar bar = new JMenuBar();// 菜单条
        this.setJMenuBar(bar);
        JMenu jm = new JMenu("控制(C)");
        jm.setMnemonic('C');// 设置助记符---Alt+'C'，显示出来，但不运行
        bar.add(jm);
        final JMenuItem jmi1 = new JMenuItem("开启");
        jmi1.setAccelerator(KeyStroke.getKeyStroke('R', KeyEvent.CTRL_MASK));// 设置快捷键Ctrl+'R'
        jmi1.setActionCommand("run");
        jm.add(jmi1);

        JMenuItem jmi2 = new JMenuItem("退出");
        jmi2.setAccelerator(KeyStroke.getKeyStroke('E', KeyEvent.CTRL_MASK));// 设置快捷键Ctrl+'R'
        jmi2.setActionCommand("exit");
        jm.add(jmi2);


        // 监听
        ActionListener a1 = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("run")) {
                    startServer();
                    jmi1.setEnabled(false);// 内部方法~访问的只能是final对象
                } else {
                    System.exit(0);
                }
            }
        };

        jmi1.addActionListener(a1);

        Toolkit tk = Toolkit.getDefaultToolkit();
        int width = (int) tk.getScreenSize().getWidth();
        int height = (int) tk.getScreenSize().getHeight();
        this.setBounds(width / 4, height / 4, width / 2, height / 2);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);// 关闭按钮器作用

        setVisible(true);
    }

    private static final int PORT = 9090;

    protected void startServer() {
        try {
            ServerSocket server = new ServerSocket(PORT);
            area.append("启动服务：" + server);
            new ServerThread(server).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 用来保存所有在线用户的名字和Socket----池
    private Map<String, Socket> usersMap = new HashMap<String, Socket>();

    class ServerThread extends Thread {
        private ServerSocket server;

        public ServerThread(ServerSocket server) {
            this.server = server;
        }

        @Override
        public void run() {
            try {// 和客户端握手
                while (true) {
                    Socket socketClient = server.accept();
                    Scanner sc = new Scanner(socketClient.getInputStream());
                    if (sc.hasNext()) {
                        String userName = sc.nextLine();
                        area.append("\r\n用户[ " + userName + " ]登录 " + socketClient);// 在客户端通知
                        lm.addElement(userName);// 添加到用户在线列表

                        new ClientThread(socketClient).start();// 专门为这个客户端服务

                        usersMap.put(userName, socketClient);// 把当前登录的用户加到“在线用户”池中

                        msgAll(userName);// 把“当前用户登录的消息即用户名”通知给所有其他已经在线的人
                        msgSelf(socketClient);// 通知当前登录的用户，有关其他在线人的信息

                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    class ClientThread extends Thread {
        private Socket socketClient;

        public ClientThread(Socket socketClient) {
            this.socketClient = socketClient;
        }

        @Override
        public void run() {
            System.out.println("一个与客户端通讯的线程启动并开始通讯...");
            try {
                Scanner sc = new Scanner(socketClient.getInputStream());
                while (sc.hasNext()) {
                    String msg = sc.nextLine();
                    System.out.println(msg);
                    String msgs[] = msg.split("@#@#");
                    //防黑
                    if(msgs.length!=4){
                        System.out.println("防黑处理...");
                        continue;
                    }

                    if("on".equals(msgs[0])){
                        sendMsgToSb(msgs);
                    }

                    if("exit".equals(msgs[0])){
                        //服务器显示
                        area.append("\r\n用户[ " + msgs[3] + " ]已退出!" + usersMap.get(msgs[3]));

                        //从在线用户池中把该用户删除
                        usersMap.remove(msgs[3]);

                        //服务器的在线列表中把该用户删除
                        lm.removeElement(msgs[3]);

                        //通知其他用户，该用户已经退出
                        sendExitMsgToAll(msgs);
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    //通知其他用户。该用户已经退出
    private void sendExitMsgToAll(String[] msgs) throws IOException {
        Iterator<String> userNames = usersMap.keySet().iterator();

        while(userNames.hasNext()){
            String userName = userNames.next();
            Socket s = usersMap.get(userName);
            PrintWriter pw = new PrintWriter(s.getOutputStream(),true);
            String str = "msg@#@#server@#@#用户[ "+msgs[3]+" ]已退出！";
            pw.println(str);
            pw.flush();

            str = "cmdRed@#@#server@#@#"+msgs[3];
            pw.println(str);
            pw.flush();
        }

    }


    //服务器把客户端的聊天消息转发给相应的其他客户端
    public void sendMsgToSb(String[] msgs) throws IOException {

        if("全部".equals(msgs[1])){
            Iterator<String> userNames = usersMap.keySet().iterator();
            //遍历每一个在线用户，把聊天消息发给他
            while(userNames.hasNext()){
                String userName = userNames.next();
                Socket s = usersMap.get(userName);
                PrintWriter pw = new PrintWriter(s.getOutputStream(),true);
                String str = "msg@#@#"+msgs[3]+"@#@#"+msgs[2];
                pw.println(str);
                pw.flush();
            }
        }else{
            Socket s = usersMap.get(msgs[1]);
            PrintWriter pw = new PrintWriter(s.getOutputStream(),true);
            String str = "msg@#@#"+msgs[3]+"对你@#@#"+msgs[2];
            pw.println(str);
            pw.flush();
        }
    }



    /**
     * 把“当前用户登录的消息即用户名”通知给所有其他已经在线的人
     *
     * @param userName
     */
    // 技术思路:从池中依次把每个socket(代表每个在线用户)取出，向它发送userName
    public void msgAll(String userName) {
        Iterator<Socket> it = usersMap.values().iterator();
        while (it.hasNext()) {
            Socket s = it.next();
            try {
                PrintWriter pw = new PrintWriter(s.getOutputStream(), true);// 加true为自动刷新
                String msg = "msg@#@#server@#@#用户[ " + userName + " ]已登录!";// 通知客户端显示消息
                pw.println(msg);
                pw.flush();
                msg = "cmdAdd@#@#server@#@#" + userName;// 通知客户端在在线列表添加用户在线。
                pw.println(msg);
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 通知当前登录的用户，有关其他在线人的信息
     *
     * @param socketClient
     */
    // 把原先已经在线的那些用户的名字发给该登录用户，让他给自己界面中的lm添加相应的用户名
    public void msgSelf(Socket socketClient) {
        try {
            PrintWriter pw = new PrintWriter(socketClient.getOutputStream(),true);
            Iterator<String> it = usersMap.keySet().iterator();
            while (it.hasNext()) {
                String msg = "cmdAdd@#@#server@#@#" + it.next();
                pw.println(msg);
                pw.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);// 设置装饰
        new ServerForm();
    }
}