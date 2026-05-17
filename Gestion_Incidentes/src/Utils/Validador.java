package Utils;

import java.util.regex.Pattern;

/**
 * Correcciones aplicadas:
 *  [BUG-005] Regex de email débil reemplazado por uno que exige:
 *             - parte local válida
 *             - dominio con al menos un punto
 *             - TLD de al menos 2 caracteres
 *             - no permite dominios como "a", ".com", "com.", etc.
 */
public class Validador {

    // [BUG-005] Antes: "^[A-Za-z0-9+_.-]+@(.+)$"  ← acepta user@a, user@.com, etc.
    // Ahora: exige dominio con etiquetas separadas por puntos, TLD >= 2 chars
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                "^[A-Za-z0-9+_.-]+" +           // parte local
                "@" +
                "(?:[A-Za-z0-9-]+\\.)" +         // al menos un subdominio seguido de punto
                "[A-Za-z]{2,}$"                   // TLD de mínimo 2 letras
            );

    public boolean validarEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public boolean validarPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public boolean validarNombre(String nombre) {
        return nombre != null && !nombre.trim().isEmpty() && nombre.length() <= 100;
    }
}
