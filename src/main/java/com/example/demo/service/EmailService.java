package com.example.demo.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmail(String destinatario, String titulo, String descricao) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("senai@participativo.com.br");
        helper.setTo(destinatario);
        helper.setSubject(titulo);
        helper.setText(descricao);

        mailSender.send(message);
    }

    public void enviarEmailFromTemplate(String destinatario, String titulo, String fileName)
            throws MessagingException, IOException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("senai@participativo.com.br");
        helper.setTo(destinatario);
        helper.setSubject(titulo);

        ClassPathResource resource = new ClassPathResource("templates/email/" + fileName);

        String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        helper.setText(html, true);

        mailSender.send(message);
    }

}
