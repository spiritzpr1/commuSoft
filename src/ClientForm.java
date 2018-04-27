import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * 客户端
 *
 * @author 陈浩翔
 * @version 1.0 2016-5-13
 */
public class ClientForm extends JFrame implements ActionListener {
    private JTextField tfdUserName;
    private JList<String> list;
    private DefaultListModel<String> lm;
    private JTextArea allMsg;
    private JTextField tfdMsg;
    private JButton btnCon;
    private JButton btnExit;
    private JButton btnSend;

    // private static String HOST="192.168.31.168";
    private static String HOST = "127.0.0.1";// 自己机子，服务器的ip地址
    private static int PORT = 9090;// 服务器的端口号
    private Socket clientSocket;
    private PrintWriter pw;

    public ClientForm() {

        super("即时通讯工具1.0");
        // 菜单条
        addJMenu();

        // 上面的面板
        JPanel p = new JPanel();
        JLabel jlb1 = new JLabel("用户标识:");
        tfdUserName = new JTextField(10);
        // tfdUserName.setEnabled(false);//不能选中和修改
        // dtfdUserName.setEditable(false);//不能修改

        // 链接按钮
        ImageIcon icon = new ImageIcon("a.png");
        btnCon = new JButton("", icon);
        btnCon.setActionCommand("c");
        btnCon.addActionListener(this);

        // 退出按钮
        icon = new ImageIcon("b.jpg");
        btnExit = new JButton("", icon);
        btnExit.setActionCommand("exit");

        btnExit.addActionListener(this);
        btnExit.setEnabled(false);
        p.add(jlb1);
        p.add(tfdUserName);
        p.add(btnCon);
        p.add(btnExit);
        getContentPane().add(p, BorderLayout.NORTH);

        // 中间的面板
        JPanel cenP = new JPanel(new BorderLayout());
        this.getContentPane().add(cenP, BorderLayout.CENTER);

        // 在线列表
        lm = new DefaultListModel<String>();
        list = new JList<String>(lm);
        lm.addElement("全部");
        list.setSelectedIndex(0);// 设置默认显示
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);// 只能选中一行
        list.setVisibleRowCount(2);
        JScrollPane js = new JScrollPane(list);
        Border border = new TitledBorder("在线");
        js.setBorder(border);
        Dimension preferredSize = new Dimension(70, cenP.getHeight());
        js.setPreferredSize(preferredSize);
        cenP.add(js, BorderLayout.EAST);

        // 聊天消息框
        allMsg = new JTextArea();
        allMsg.setEditable(false);
        cenP.add(new JScrollPane(allMsg), BorderLayout.CENTER);

        // 消息发送面板
        JPanel p3 = new JPanel();
        JLabel jlb2 = new JLabel("消息:");
        p3.add(jlb2);
        tfdMsg = new JTextField(20);
        p3.add(tfdMsg);
        btnSend = new JButton("发送");
        btnSend.setEnabled(false);
        btnSend.setActionCommand("send");
        btnSend.addActionListener(this);
        p3.add(btnSend);
        this.getContentPane().add(p3, BorderLayout.SOUTH);

        // *************************************************
        // 右上角的X-关闭按钮-添加事件处理
        addWindowListener(new WindowAdapter() {
            // 适配器
            @Override
            public void windowClosing(WindowEvent e) {
                if (pw == null) {
                    System.exit(0);
                }
                String msg = "exit@#@#全部@#@#null@#@#" + tfdUserName.getText();
                pw.println(msg);
                pw.flush();
                System.exit(0);
            }
        });

        setBounds(300, 300, 400, 300);
        setVisible(true);
    }

    private void addJMenu() {
        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);

        JMenu menu = new JMenu("选项");
        menuBar.add(menu);

        JMenuItem menuItemSet = new JMenuItem("设置");
        JMenuItem menuItemHelp = new JMenuItem("帮助");
        menu.add(menuItemSet);
        menu.add(menuItemHelp);

        menuItemSet.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final JDialog dlg = new JDialog(ClientForm.this);// 弹出一个界面
                // 不能直接用this

                dlg.setBounds(ClientForm.this.getX()+20, ClientForm.this.getY()+30,
                        350, 150);
                dlg.setLayout(new FlowLayout());
                dlg.add(new JLabel("服务器IP和端口:"));

                final JTextField tfdHost = new JTextField(10);
                tfdHost.setText(ClientForm.HOST);
                dlg.add(tfdHost);

                dlg.add(new JLabel(":"));

                final JTextField tfdPort = new JTextField(5);
                tfdPort.setText(""+ClientForm.PORT);
                dlg.add(tfdPort);

                JButton btnSet = new JButton("设置");
                dlg.add(btnSet);
                btnSet.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String ip = tfdHost.getText();//解析并判断ip是否合法
                        String strs[] = ip.split("\\.");
                        if(strs==null||strs.length!=4){
                            JOptionPane.showMessageDialog(ClientForm.this, "IP类型有误！");
                            return ;
                        }
                        try {
                            for(int i=0;i<4;i++){
                                int num = Integer.parseInt(strs[i]);
                                if(num>255||num<0){
                                    JOptionPane.showMessageDialog(ClientForm.this, "IP类型有误！");
                                    return ;
                                }
                            }
                        } catch (NumberFormatException e2) {
                            JOptionPane.showMessageDialog(ClientForm.this, "IP类型有误！");
                            return ;
                        }

                        ClientForm.HOST=tfdHost.getText();//先解析并判断ip是否合法

                        try {
                            int port = Integer.parseInt( tfdPort.getText() );
                            if(port<0||port>65535){
                                JOptionPane.showMessageDialog(ClientForm.this, "端口范围有误！");
                                return ;
                            }
                        } catch (NumberFormatException e1) {
                            JOptionPane.showMessageDialog(ClientForm.this, "端口类型有误！");
                            return ;
                        }
                        ClientForm.PORT=Integer.parseInt( tfdPort.getText() );

                        dlg.dispose();//关闭这个界面
                    }
                });
                dlg.setVisible(true);//显示出来
            }
        });

        menuItemHelp.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dlg = new JDialog(ClientForm.this);

                dlg.setBounds(ClientForm.this.getX()+30,ClientForm.this.getY()+30, 400, 100);
                dlg.setLayout(new FlowLayout());
                dlg.add(new JLabel("版本所有@陈浩翔.2016.5.16 我的主页:http://chenhaoxiang.github.io"));
                dlg.setVisible(true);
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("c")) {
            if (tfdUserName.getText() == null
                    || tfdUserName.getText().trim().length() == 0
                    || "@#@#".equals(tfdUserName.getText())
                    || "@#".equals(tfdUserName.getText())) {
                JOptionPane.showMessageDialog(this, "用户名输入有误，请重新输入！");
                return;
            }

            connecting();// 连接服务器的动作
            if (pw == null) {
                JOptionPane.showMessageDialog(this, "服务器未开启或网络未连接，无法连接！");
                return;
            }

            ((JButton) (e.getSource())).setEnabled(false);
            // 获得btnCon按钮--获得源
            // 相当于btnCon.setEnabled(false);
            btnExit.setEnabled(true);
            btnSend.setEnabled(true);
            tfdUserName.setEditable(false);
        } else if (e.getActionCommand().equals("send")) {
            if (tfdMsg.getText() == null
                    || tfdMsg.getText().trim().length() == 0) {
                return;
            }
            String msg = "on@#@#" + list.getSelectedValue() + "@#@#"
                    + tfdMsg.getText() + "@#@#" + tfdUserName.getText();
            pw.println(msg);
            pw.flush();

            // 将发送消息的文本设为空
            tfdMsg.setText("");
        } else if (e.getActionCommand().equals("exit")) {
            //先把自己在线的菜单清空
            lm.removeAllElements();

            sendExitMsg();
            btnCon.setEnabled(true);
            btnExit.setEnabled(false);
            tfdUserName.setEditable(true);
        }

    }

    // 向服务器发送退出消息
    private void sendExitMsg() {
        String msg = "exit@#@#全部@#@#null@#@#" + tfdUserName.getText();
        System.out.println("退出:" + msg);
        pw.println(msg);
        pw.flush();
    }

    private void connecting() {
        try {
            // 先根据用户名防范
            String userName = tfdUserName.getText();
            if (userName == null || userName.trim().length() == 0) {
                JOptionPane.showMessageDialog(this, "连接服务器失败!\r\n用户名有误，请重新输入！");
                return;
            }

            clientSocket = new Socket(HOST, PORT);// 跟服务器握手
            pw = new PrintWriter(clientSocket.getOutputStream(), true);// 加上自动刷新
            pw.println(userName);// 向服务器报上自己的用户名
            this.setTitle("用户[ " + userName + " ]上线...");

            new ClientThread().start();// 接受服务器发来的消息---一直开着的
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ClientThread extends Thread {
        @Override
        public void run() {
            try {
                Scanner sc = new Scanner(clientSocket.getInputStream());
                while (sc.hasNextLine()) {
                    String str = sc.nextLine();
                    String msgs[] = str.split("@#@#");
                    System.out.println(tfdUserName.getText() + ": " + str);
                    if ("msg".equals(msgs[0])) {
                        if ("server".equals(msgs[1])) {// 服务器发送的官方消息
                            str = "[ 通知 ]:" + msgs[2];
                        } else {// 服务器转发的聊天消息
                            str = "[ " + msgs[1] + " ]说: " + msgs[2];
                        }
                        allMsg.append("\r\n" + str);
                    }
                    if ("cmdAdd".equals(msgs[0])) {
                        boolean eq = false;
                        for (int i = 0; i < lm.getSize(); i++) {
                            if (lm.getElementAt(i).equals(msgs[2])) {
                                eq = true;
                            }
                        }
                        if (!eq) {
                            lm.addElement(msgs[2]);// 用户上线--添加
                        }
                    }
                    if ("cmdRed".equals(msgs[0])) {
                        lm.removeElement(msgs[2]);// 用户离线了--移除
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);// 设置装饰
        new ClientForm();
    }
}