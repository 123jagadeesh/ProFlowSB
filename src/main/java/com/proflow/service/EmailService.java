package com.proflow.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendEmail(String to, String subject, String htmlContent, String companyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            String senderName = (companyName != null && !companyName.isBlank()) ? companyName + " ProFlow" : "ProFlow Notifications";
            helper.setFrom(fromEmail, senderName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {} with subject: '{}'", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
            // Do not rethrow in async method to prevent impacting caller workflow
        }
    }

    public void sendTaskAssignedEmail(String toEmail, String employeeName, String taskTitle, String companyName) {
        String html = String.format("""
                <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                    <h2 style="color: #2563eb;">%s ProFlow - New Task Assigned</h2>
                    <p>Hi <strong>%s</strong>,</p>
                    <p>A new task has been assigned to you: <strong>%s</strong>.</p>
                    <p>Please login to your ProFlow dashboard to view details and start working on it.</p>
                    <br>
                    <p style="color: #6b7280; font-size: 13px;">This is an automated notification from %s ProFlow.</p>
                </div>
                """, companyName, employeeName, taskTitle, companyName);

        sendEmail(toEmail, String.format("[%s] New Task Assigned", companyName), html, companyName);
    }

    public void sendTaskUpdatedEmail(String toEmail, String employeeName, String taskTitle, String companyName) {
        String html = String.format("""
                <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                    <h2 style="color: #2563eb;">%s ProFlow - Task Updated</h2>
                    <p>Hi <strong>%s</strong>,</p>
                    <p>The task <strong>%s</strong> assigned to you has been updated.</p>
                    <p>Please login to your ProFlow dashboard to view latest changes.</p>
                    <br>
                    <p style="color: #6b7280; font-size: 13px;">This is an automated notification from %s ProFlow.</p>
                </div>
                """, companyName, employeeName, taskTitle, companyName);

        sendEmail(toEmail, String.format("[%s] Task Updated/Assigned", companyName), html, companyName);
    }

    public void sendTaskDeletedEmail(String toEmail, String employeeName, String taskTitle, String companyName) {
        String html = String.format("""
                <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                    <h2 style="color: #dc2626;">%s ProFlow - Task Deleted</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>The task <strong>%s</strong> has been <strong>deleted</strong> from %s ProFlow.</p>
                    <br>
                    <p style="color: #6b7280; font-size: 13px;">This is an automated notification from %s ProFlow.</p>
                </div>
                """, companyName, employeeName, taskTitle, companyName, companyName);

        sendEmail(toEmail, String.format("[%s] Task Deleted", companyName), html, companyName);
    }

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        String html = String.format("""
                <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                    <h2 style="color: #2563eb;">ProFlow Password Reset Request</h2>
                    <p>You requested a password reset. Please click the link below to reset your password:</p>
                    <p><a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #2563eb; color: #fff; text-decoration: none; border-radius: 5px;">Reset Password</a></p>
                    <p>Or copy this link to your browser:</p>
                    <p><a href="%s">%s</a></p>
                    <p style="color: #ef4444; font-size: 13px;">This link will expire in 10 minutes.</p>
                </div>
                """, resetUrl, resetUrl, resetUrl);

        sendEmail(toEmail, "ProFlow Password Reset Request", html, "ProFlow");
    }

    public void sendZoomInviteEmail(String toEmail, String topic, String formattedTime, String joinUrl, String creatorName, String companyName) {
        String html = String.format("""
                <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                    <h2 style="color: #2563eb;">Zoom Meeting Invitation</h2>
                    <p>You are invited to a Zoom meeting scheduled by <strong>%s</strong>.</p>
                    <p><b>Topic:</b> %s</p>
                    <p><b>Time:</b> %s</p>
                    <p><a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #0284c7; color: #fff; text-decoration: none; border-radius: 5px;">Join Zoom Meeting</a></p>
                </div>
                """, creatorName, topic, formattedTime, joinUrl);

        sendEmail(toEmail, String.format("Zoom Invite: %s", topic), html, companyName);
    }
}
