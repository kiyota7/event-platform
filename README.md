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
