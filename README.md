# Event Platform

勉強会・セミナーの作成と参加管理ができるイベント管理・予約プラットフォーム。

## 技術スタック
- Backend: Spring Boot 3 (Java 21) / Spring Security + JWT / Spring Data JPA / PostgreSQL
- Frontend: React + Vite (ポート5173)
- メール: SendGrid(開発中はログ出力)
- リアルタイム通知: SSE (Server-Sent Events)
- デプロイ: Render

## 技術的ポイント
- タイムゾーン: DBはUTC(`Instant`)保存、ユーザー/イベントのTZで表示変換
- 同時申込: `@Version`による楽観的ロック + リトライ
- メール通知: 参加確定・リマインダー(スケジューラ)

## 機能
ユーザー登録/ログイン(JWT)、イベントCRUD+カテゴリ・タグ、参加申込/キャンセル/定員管理(キャンセル待ち繰上げ)、
検索・フィルタ、リアルタイム通知、主催者ダッシュボード、カレンダー表示、Renderデプロイ

## 開発ルール
`CLAUDE.md`参照(Issue→ブランチ→PR、mainへの直接pushは禁止)。

## ローカル起動
```bash
cd backend && mvn spring-boot:run     # http://localhost:8080 (H2インメモリ)
cd frontend && npm install && npm run dev   # http://localhost:5173 (/apiは8080へプロキシ)
```

## Renderへのデプロイ
`render.yaml`(Blueprint)でPostgreSQL・バックエンド(Docker)・フロント(静的サイト)を作成する。

1. Renderで **New → Blueprint** を選び、このGitHubリポジトリを接続する
2. 作成されるサービス名が`event-platform-api`/`event-platform-web`でない(名前重複で末尾に文字が付く)場合は、`render.yaml`の`destination`のバックエンドURLを実URLに直す
3. `event-platform-api`の環境変数を設定する
   - `CORS_ALLOWED_ORIGIN`: フロントのURL(例 `https://event-platform-web.onrender.com`)
   - `SENDGRID_API_KEY` / `MAIL_FROM`: メールを実際に送る場合のみ(SendGridでSender認証済みのアドレス)
   - `JWT_SECRET`は自動生成、DB接続情報はDBから自動注入
4. デプロイ後、`https://<api>/actuator/health` が`UP`、フロントで登録→ログインできることを確認する

### 注意
- 無料枠のWebサービスは15分無通信でスリープし、初回アクセスが遅い。スリープ中はリマインダーのスケジューラも動かない
- 無料のPostgreSQLは一定期間で失効する(Renderの現行の無料枠の条件を確認すること)
- SSE通知は単一インスタンス前提(スケールアウトにはRedis等が必要)
- スキーマは`ddl-auto: update`で管理している。本格運用ではFlyway等のマイグレーションに置き換える
