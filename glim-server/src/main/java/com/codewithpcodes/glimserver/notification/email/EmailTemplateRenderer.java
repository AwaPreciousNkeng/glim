package com.codewithpcodes.glimserver.notification.email;

import com.codewithpcodes.glimserver.user.User;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateRenderer {

    private static final String SHELL = """
        <div style="font-family:system-ui,-apple-system,sans-serif;max-width:480px;
                    margin:0 auto;padding:24px;color:#1A1D1A">
          <div style="background:#1B6B3A;color:#fff;padding:20px;border-radius:12px 12px 0 0">
            <h2 style="margin:0;font-size:20px">GLIM City</h2>
          </div>
          <div style="background:#fff;border:1px solid #E5E9E5;border-top:none;
                      padding:24px;border-radius:0 0 12px 12px">
            %s
          </div>
          <p style="color:#6B726B;font-size:12px;text-align:center;margin-top:16px">
            Grace Love International Ministries
          </p>
        </div>
        """;

    public String verificationSubject(String lang) {
        return "FRENCH".equals(lang) ? "Confirmez votre adresse e-mail" : "Confirm your email address";
    }

    public String verificationBody(User user, String link) {
        boolean fr = "FRENCH".equals(user.getLanguage().name());
        String inner = fr ? """
            <p>Bonjour %s,</p>
            <p>Confirmez votre adresse e-mail pour sécuriser votre compte.</p>
            <p style="text-align:center;margin:28px 0">
              <a href="%s" style="background:#1B6B3A;color:#fff;padding:14px 28px;
                 border-radius:10px;text-decoration:none;font-weight:600">Confirmer</a>
            </p>
            <p style="color:#6B726B;font-size:13px">Ce lien expire dans 24 heures.</p>
            """.formatted(user.getFirstName(), link)
                : """
            <p>Hello %s,</p>
            <p>Confirm your email address to secure your account.</p>
            <p style="text-align:center;margin:28px 0">
              <a href="%s" style="background:#1B6B3A;color:#fff;padding:14px 28px;
                 border-radius:10px;text-decoration:none;font-weight:600">Confirm email</a>
            </p>
            <p style="color:#6B726B;font-size:13px">This link expires in 24 hours.</p>
            """.formatted(user.getFirstName(), link);

        return SHELL.formatted(inner);
    }

    public String resetSubject(String lang) {
        return "FRENCH".equals(lang) ? "Votre code de réinitialisation" : "Your password reset code";
    }

    public String resetBody(User user, String code) {
        boolean fr = "FRENCH".equals(user.getLanguage().name());
        String inner = fr ? """
            <p>Bonjour %s,</p>
            <p>Voici votre code pour réinitialiser votre mot de passe :</p>
            <p style="text-align:center;font-size:34px;font-weight:700;letter-spacing:8px;
                      color:#1B6B3A;margin:28px 0">%s</p>
            <p style="color:#6B726B;font-size:13px">Ce code expire dans 15 minutes.
            Si vous n'avez pas fait cette demande, ignorez ce message.</p>
            """.formatted(user.getFirstName(), code)
                : """
            <p>Hello %s,</p>
            <p>Here is your code to reset your password:</p>
            <p style="text-align:center;font-size:34px;font-weight:700;letter-spacing:8px;
                      color:#1B6B3A;margin:28px 0">%s</p>
            <p style="color:#6B726B;font-size:13px">This code expires in 15 minutes.
            If you didn't request it, ignore this message.</p>
            """.formatted(user.getFirstName(), code);

        return SHELL.formatted(inner);
    }

    public String render(String title, String body, String actionUrl, String actionLabel) {
        String action = (actionUrl == null || actionUrl.isBlank()) ? "" : """
            <p style="text-align:center;margin:24px 0 0">
              <a href="%s" style="background:#1B6B3A;color:#fff;padding:12px 24px;
                 border-radius:10px;text-decoration:none;font-weight:600;
                 display:inline-block">%s</a>
            </p>
            """.formatted(actionUrl, actionLabel == null ? "Open" : actionLabel);

        return SHELL.formatted(title, body, action);
    }
}
