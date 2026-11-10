package com.notas.app.backup

import java.util.Date
import java.util.Properties
import javax.activation.CommandMap
import javax.activation.DataHandler
import javax.activation.MailcapCommandMap
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

/** Envia e-mail pelo SMTP do Gmail usando a conta (e senha de app) do próprio usuário. */
object Mailer {

    class Attachment(val name: String, val mime: String, val bytes: ByteArray)

    fun send(
        user: String,
        password: String,
        to: String,
        subject: String,
        body: String,
        attachments: List<Attachment>
    ) {
        // Necessário no Android para a biblioteca de e-mail achar seus "handlers"
        Thread.currentThread().contextClassLoader = Mailer::class.java.classLoader
        (CommandMap.getDefaultCommandMap() as? MailcapCommandMap)?.apply {
            addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
            addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
            addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
        }

        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "465")
            put("mail.smtp.ssl.enable", "true")
            put("mail.smtp.auth", "true")
            put("mail.smtp.connectiontimeout", "20000")
            put("mail.smtp.timeout", "30000")
            put("mail.smtp.writetimeout", "30000")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(user, password)
        })

        val multipart = MimeMultipart()
        val textPart = MimeBodyPart()
        textPart.setText(body, "UTF-8")
        multipart.addBodyPart(textPart)
        for (a in attachments) {
            val part = MimeBodyPart()
            part.dataHandler = DataHandler(ByteArrayDataSource(a.bytes, a.mime))
            part.fileName = a.name
            multipart.addBodyPart(part)
        }

        val message = MimeMessage(session)
        message.setFrom(InternetAddress(user))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
        message.setSubject(subject, "UTF-8")
        message.sentDate = Date()
        message.setContent(multipart)

        Transport.send(message)
    }
}
