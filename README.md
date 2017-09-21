# SendMail
Android SMTP send mail


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
