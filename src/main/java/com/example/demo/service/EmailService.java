package com.example.demo.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async; // IMPORTAR ASYNC
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Async // <─── Diz ao Spring para rodar isso em segundo plano
    public void enviarEmail(String destinatario, String titulo, String descricao) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("senai@participativo.com.br");
            helper.setTo(destinatario);
            helper.setSubject(titulo);
            helper.setText(descricao);

            mailSender.send(message);
            log.info("E-mail enviado com sucesso para: {}", destinatario);
        } catch (MessagingException e) {
            log.error("Falha ao enviar e-mail assíncrono: {}", e.getMessage());
        }
    }

    @Async
    public void enviarEmailComCodigo(String destinatario, String nome, String codigo, String link, String mensagem) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("senai@participativo.com.br");
            helper.setTo(destinatario);
            helper.setSubject("Seu código de acesso — Biblioteca");

            ClassPathResource resource = new ClassPathResource("templates/email/codigo-acesso.html");
            String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            html = html.replace("{{nome}}", nome)
                       .replace("{{codigo}}", codigo)
                       .replace("{{link}}", link)
                       .replace("{{mensagem}}", mensagem);

            helper.setText(html, true);

            mailSender.send(message);
            log.info("E-mail de código enviado para: {}", destinatario);
        } catch (MessagingException | IOException e) {
            log.error("Falha ao enviar e-mail de código: {}", e.getMessage());
        }
    }

    @Async // <─── Também em segundo plano
    public void enviarEmailFromTemplate(String destinatario, String titulo, String fileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("senai@participativo.com.br");
            helper.setTo(destinatario);
            helper.setSubject(titulo);

            ClassPathResource resource = new ClassPathResource("templates/email/" + fileName);
            String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            helper.setText(html, true);

            mailSender.send(message);
            log.info("E-mail template enviado com sucesso para: {}", destinatario);
        } catch (MessagingException | IOException e) {
            log.error("Falha ao enviar e-mail template: {}", e.getMessage());
        }
    }
}