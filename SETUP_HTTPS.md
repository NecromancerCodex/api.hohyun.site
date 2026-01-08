# HTTPS 설정 가이드 (Let's Encrypt)

## 빠른 설정 (5분)

### 1. EC2 서버에 SSH 접속
```bash
ssh ubuntu@your-ec2-ip
```

### 2. Certbot 설치
```bash
sudo apt update
sudo apt install certbot python3-certbot-nginx -y
```

### 3. SSL 인증서 발급 및 자동 설정
```bash
sudo certbot --nginx -d api.hohyun.site
```

**질문에 답변:**
- Email: 본인 이메일 입력
- Terms of Service: Y (동의)
- Share email: N (선택)
- HTTP → HTTPS 리다이렉트: 2 (Redirect 선택)

### 4. 자동 갱신 테스트
```bash
sudo certbot renew --dry-run
```

### 5. Nginx 재시작
```bash
sudo nginx -t
sudo systemctl restart nginx
```

### 6. 확인
```bash
# HTTPS 접근 테스트
curl https://api.hohyun.site/actuator/health

# 브라우저에서 확인
# https://api.hohyun.site/actuator/health
```

## 완료!

이제 `https://api.hohyun.site`로 접근할 수 있습니다.

## 자동 갱신 설정 (선택)

Let's Encrypt 인증서는 90일마다 갱신이 필요합니다. 자동 갱신은 기본적으로 설정되어 있지만, 확인:

```bash
# 자동 갱신 확인
sudo systemctl status certbot.timer

# 수동 갱신 테스트
sudo certbot renew --dry-run
```

## 문제 해결

### 인증서 발급 실패 시
1. **DNS 확인**: `nslookup api.hohyun.site`로 IP 확인
2. **포트 80 열림 확인**: EC2 보안 그룹에서 포트 80 허용 확인
3. **Nginx 실행 확인**: `sudo systemctl status nginx`

### 인증서 갱신 실패 시
```bash
# 로그 확인
sudo tail -50 /var/log/letsencrypt/letsencrypt.log

# 수동 갱신
sudo certbot renew
```

