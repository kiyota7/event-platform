# Event Platform

勉強会・セミナーの作成と参加管理ができるイベント管理・予約プラットフォーム。

## 作成背景
スクールの最終課題として作成した。題材は、勉強会やセミナーの作成と参加管理ができる「イベント管理・予約プラットフォーム」。

課題の要件は次のとおり。

- 機能: ユーザー登録・ログイン(JWT)、イベントCRUD+カテゴリ・タグ、参加申込・キャンセル・定員管理、イベント検索・フィルタ、リアルタイム通知(参加確定・リマインダー)、主催者ダッシュボード(参加者一覧・統計)、カレンダー表示、AWSまたはRenderへのデプロイ
- 技術的ポイント: 日時のタイムゾーン処理、同時申込の排他制御(楽観的ロック)、メール通知連携(SendGrid等)

これらを、設計から公開まで通して作ることを目的とした。デプロイ先はRender、メール送信はSendGridを選んだ。開発は、機能ごとにIssue・ブランチ・Pull Requestを分けて進めた。設計と動作確認の詳細は、提出資料(設計・報告書)にまとめている。

## 公開URL(Render)
- アプリ: https://event-platform-web-g55k.onrender.com
- APIヘルスチェック: https://event-platform-api-s0kk.onrender.com/actuator/health

> 無料枠のため、しばらくアクセスがないとサーバーがスリープします。初回の表示に1分ほどかかることがあります。

## 技術スタック
- Backend: Spring Boot 3 (Java 21) / Spring Security + JWT / Spring Data JPA / PostgreSQL
- Frontend: React + Vite (ポート5173)
- メール: SendGrid(APIキー未設定の場合はログ出力)
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

## テスト
```bash
cd backend && mvn test
```

バックエンドに15件の自動テストがある(認証、イベントCRUD、検索、申込・定員、メール通知、SSE通知、ダッシュボード、エラー応答)。同時申込のテストは、定員5のイベントに30スレッドが同時に申込んでも、定員を超えないことを確認している。

## Renderへのデプロイ
`render.yaml`(Blueprint)でPostgreSQL・バックエンド(Docker)・フロント(静的サイト)を作成する。

1. Renderで **New → Blueprint** を選び、このGitHubリポジトリを接続する
2. 作成されるサービス名が`event-platform-api`/`event-platform-web`でない(名前重複で末尾に文字が付く)場合は、`render.yaml`の`destination`のバックエンドURLを実URLに直す
3. `event-platform-api`の環境変数を設定する。`JWT_SECRET`は自動生成、DB接続情報はDBから自動注入される。次の値は、Blueprintの作成時に入力を求められず、一覧にも出ないため、**Environment**で手動で追加する(保存すると自動で再デプロイされる)
   - `SENDGRID_API_KEY` / `MAIL_FROM`: メールを実際に送る場合のみ。設定方法は次の「メールの設定」を参照
   - `CORS_ALLOWED_ORIGIN`: 任意。フロントが`/api`を中継するため、設定しなくても動く
4. デプロイ後、`https://<api>/actuator/health` が`UP`、フロントで登録→ログインできることを確認する

### メールの設定(SendGrid)
1. SendGridでアカウントを作り、**Settings → Sender Authentication** で **Single Sender** の送信元アドレスを認証する
2. **Settings → API Keys** で、**Restricted Access** のAPIキーを作る。権限は **Mail Send** のみでよい。キーは作成時に一度だけ表示される
3. Renderの`event-platform-api` → **Environment** で、`SENDGRID_API_KEY`(手順2のキー)と`MAIL_FROM`を追加して保存する。`MAIL_FROM`は、認証した送信元アドレスと**完全に同じ**にする(違うとSendGridが403で拒否する)
4. 新しいアカウントで、未来のイベントに申込み、参加確定のメールが届くことを確認する。リマインダーは、開始まで24時間以内のイベントに申込むと、最長10分ほどで届く

APIキーはリポジトリに書かず、Renderの画面だけで設定する。キーが未設定の場合、メールは送られず、ログに出力されるだけ。

### 注意
- 無料枠のWebサービスは15分無通信でスリープし、初回アクセスが遅い。スリープ中はリマインダーのスケジューラも動かない。対策として、外部の監視サービス(UptimeRobot等)で、バックエンドの`/actuator/health`を5分ごとに確認する設定にしている
- 無料のPostgreSQLは一定期間で失効する(Renderの現行の無料枠の条件を確認すること)
- SSE通知は単一インスタンス前提(スケールアウトにはRedis等が必要)。Renderの中継を通した長時間の接続は、本番では確認できていない
- スキーマは`ddl-auto: update`で管理している。本格運用ではFlyway等のマイグレーションに置き換える
