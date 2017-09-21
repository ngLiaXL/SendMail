package ngliaxl.sendmail;

import android.util.Base64;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;


/**
 * LOGIN 口令-应答过程如下：
 * 1 C: AUTH LOGIN
 * 2 S: 334 dXNlcm5hbWU6
 * 3 C: dXNlcm5hbWU6
 * 4 S: 334 cGFzc3dvcmQ6
 * 5 C: cGFzc3dvcmQ6
 * 6 S: 235 Authentication successful.
 *
 * 1 为客户端向服务器发送认证指令。
 * 2 服务端返回base64编码串，成功码为334。编码字符串解码后为“username:”，说明要求客户端发送用户名。
 * 3 客户端发送用base64编码的用户名，此处为“username:”。
 * 4 服务端返回base64编码串，成功码为334。编码字符串解码后为“passWord:”，说明要求客户端发送用户口令。
 * 5 客户端发送用base64编码的口令，此处为“password:”。
 * 6 成功后，服务端返回码为235，表示认证成功可以发送邮件了。
 */
public class SendMail {

    private static final String L = "\r\n";
    private static final String EHLO = "EHLO pop.exmail.qq.com" + L;
    private static final String AUTH = "AUTH LOGIN" + L;
    private static final String DEFAULT_USER_NAME = "username";
    private static final String DEFAULT_PASSWORD = "password";

    private String mHost;
    private int mPort;
    private String mAddresser = "send";
    private String mAddressee = "<xxx.com>;";
    private String mSubject;
    private String mContent;

    private String mUserName;
    private String mPassword;

    private Socket mSocket;
    private DataOutputStream mOutputStream;
    private DataInputStream mInputStream;

    private SendMailListener mSendMailListener;

    public interface SendMailListener {
        void onSendFinished();
    }


    public void sendAsync() {
        Thread sender = new Thread("sendMailThread") {
            @Override
            public void run() {
                send();
            }
        };
        sender.start();
    }


    public void send() {
        if (mHost == null || mPort == 0) {
            throw new RuntimeException("You must set host and port by calling withHost/withPort " +
                    "method");
        }
        if (mAddresser == null || mAddressee == null) {
            throw new RuntimeException("You must set addresser and addressee by calling " +
                    "withAddresser/withAddressee " +
                    "method");
        }
        initSocket(mHost, mPort);

        int loop = 0;
        int index = 0;
        String[] addressees = mAddressee.split(";");
        while (true) {
            if (!mSocket.isConnected()) {
                sleep(3000);
                continue;
            }
            final String cmd = read();
            if (loop == 0 && cmd.contains("220")) {
                writeBytes(EHLO);
            } else if (loop == 1) {
                writeBytes(AUTH);
            } else if (loop == 2 && cmd.contains("334")) {
                writeBytes(getUserName());
            } else if (loop == 3 && cmd.contains("334")) {
                writeBytes(getPassword());
            } else if (loop == 4 && cmd.contains("235")) {
                writeBytes(getMailFrom());
            } else if (loop >= 5 && loop <= (5 + addressees.length - 1) && cmd.contains("250")) {
                writeBytes(getMailTo(index++, addressees));
            } else if (loop == (6 + addressees.length - 1) && cmd.contains("250")) {
                writeBytes(getMailData());
            } else if (loop == (7 + addressees.length - 1) && cmd.contains("354")) {
                write(getContent());
            } else if (loop == (8 + addressees.length - 1) && cmd.contains("250")) {
                writeBytes("QUIT\r\n");
                break;
            } else if (loop == (9 + addressees.length - 1) && cmd.contains("221")) {
                break;
            } else {
                break;
            }
            loop++;

        }
        close();

        if (mSendMailListener != null) {
            mSendMailListener.onSendFinished();
        }

    }


    public SendMail withHost(String host) {
        this.mHost = host;
        return this;
    }

    public SendMail withPort(int port) {
        this.mPort = port;
        return this;
    }

    public SendMail withAddresser(String addresser) {
        this.mAddresser = addresser;
        return this;
    }

    public SendMail withAddressee(String mAddressee) {
        this.mAddressee = mAddressee;
        return this;
    }

    public SendMail withSubject(String subject) {
        this.mSubject = subject;
        return this;
    }

    public SendMail withContent(String content) {
        this.mContent = content;
        return this;
    }

    public SendMail withMailListener(SendMailListener l) {
        this.mSendMailListener = l;
        return this;
    }

    private String getUserName() {
        if (mUserName == null) {
            mUserName = DEFAULT_USER_NAME;
        }
        byte[] buffer = mUserName.getBytes();
        return Base64.encodeToString(buffer, Base64.NO_WRAP) + L;
    }

    private String getPassword() {
        if (mPassword == null) {
            mPassword = DEFAULT_PASSWORD;
        }
        byte[] buffer = mPassword.getBytes();
        return Base64.encodeToString(buffer, Base64.NO_WRAP) + L;
    }

    private String getMailFrom() {
        return new StringBuffer()
                .append("MAIL FROM:<")
                .append(mAddresser).append(">").append(L).toString();
    }

    private String getMailTo(int index, String[] addressees) {
        return new StringBuffer().append("RCPT TO:")
                .append(addressees[index]).append(L).toString();
    }

    private String getMailData() {
        return new StringBuffer().append("DATA").append(L).toString();
    }


    private byte[] getContent() {
        StringBuffer mailbody = new StringBuffer();
        mailbody.append("From:<");
        mailbody.append(mAddresser);
        mailbody.append(">");
        mailbody.append("\r\n");


        mailbody.append("Subject:").append(mSubject);
        mailbody.append(L);
        mailbody.append("To:");
        mailbody.append(mAddressee);
        mailbody.append(L);

        mailbody.append(L);
        mailbody.append(mContent);
        mailbody.append(L);
        mailbody.append(L);
        mailbody.append(L);
        mailbody.append(L);
        mailbody.append(".");
        mailbody.append(L);
        try {
            return mailbody.toString().getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void initSocket(String host, int port) {
        try {
            mSocket = new Socket(host, port);
            mOutputStream = new DataOutputStream(mSocket.getOutputStream());
            mInputStream = new DataInputStream(mSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String read() {
        byte[] buffer = new byte[300];
        if (mInputStream != null) {
            try {
                mInputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new String(buffer);
    }

    private void write(byte b[]) {
        if (mOutputStream == null) {
            return;
        }
        try {
            mOutputStream.write(b);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeBytes(String s) {
        if (mOutputStream == null) {
            return;
        }
        try {
            mOutputStream.writeBytes(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            mSocket.close();
            mSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
