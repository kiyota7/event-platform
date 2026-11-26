package com.example.eventplatform.auth;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 本番(prodプロファイル)で、開発用のJWT秘密鍵のまま起動しないようにする。
 * application.yml のデフォルト値(dev-only-...)は、公開リポジトリに載っている既知の値のため、
 * JWT_SECRET の設定漏れに気づかず、既知の値で署名してしまうのを防ぐ。
 */
@Component
@Profile("prod")
public class JwtSecretGuard {
    /** application.yml のデフォルト値の先頭。環境変数 JWT_SECRET が未設定のときに使われる */
    static final String DEV_DEFAULT_PREFIX = "dev-only-";
    /** HMAC-SHA256 の鍵として必要な長さ(バイト) */
    static final int MIN_BYTES = 32;

    public JwtSecretGuard(@Value("${app.jwt.secret}") String secret) {
        validate(secret);
    }

    static void validate(String secret) {
        if (secret == null || secret.isBlank() || secret.startsWith(DEV_DEFAULT_PREFIX)) {
            throw new IllegalStateException(
                    "JWT_SECRET が未設定です(開発用のデフォルト値のままです)。本番では、環境変数 JWT_SECRET に十分に長いランダムな値を設定してください");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET が短すぎます(" + MIN_BYTES + "バイト以上が必要です)。十分に長いランダムな値を設定してください");
        }
    }
}
