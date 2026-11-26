package com.example.eventplatform;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eventplatform.auth.JwtSecretGuard;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** prodプロファイルでは、開発用・短いJWT秘密鍵のままだと起動に失敗する(開発・テストでは起動できる)。 */
class JwtSecretGuardTest {
    private static final String DEV_DEFAULT = "dev-only-secret-change-me-dev-only-secret-change-me";
    private static final String STRONG = "k8Qz3vP0mXw7TnR2sLb9YcHf5JdGa1Ue";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(JwtSecretGuard.class);

    @Test
    void prodRejectsDevDefaultSecret() {
        runner.withPropertyValues("spring.profiles.active=prod", "app.jwt.secret=" + DEV_DEFAULT)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).hasRootCauseMessage(
                            "JWT_SECRET が未設定です(開発用のデフォルト値のままです)。本番では、環境変数 JWT_SECRET に十分に長いランダムな値を設定してください");
                });
    }

    @Test
    void prodRejectsShortSecret() {
        runner.withPropertyValues("spring.profiles.active=prod", "app.jwt.secret=short-secret-31-bytes-xxxxxxxxx")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).rootCause().hasMessageContaining("短すぎます");
                });
    }

    @Test
    void prodAcceptsStrongSecret() {
        runner.withPropertyValues("spring.profiles.active=prod", "app.jwt.secret=" + STRONG)
                .run(ctx -> assertThat(ctx).hasNotFailed().hasSingleBean(JwtSecretGuard.class));
    }

    @Test
    void devAndTestAreNotChecked() {
        // prod以外では、ガード自体が作られず、デフォルト値でも起動できる
        runner.withPropertyValues("app.jwt.secret=" + DEV_DEFAULT)
                .run(ctx -> assertThat(ctx).hasNotFailed().doesNotHaveBean(JwtSecretGuard.class));
    }
}
