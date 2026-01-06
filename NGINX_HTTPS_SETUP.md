# Nginx 리버스 프록시 + HTTPS 설정 가이드

## 현재 구조
```
GitHub Actions → DockerHub → EC2 (SSH 배포)
API Gateway: http://localhost:8080 (Docker Container)
도메인: api.hohyun.site
EC2 IP: 54.180.100.87
```

## 목표
- Nginx를 EC2에 설치하여 리버스 프록시 구성
- Let's Encrypt로 HTTPS 인증서 발급
- HTTP → HTTPS 자동 리다이렉트
- API Gateway (포트 8080)를 HTTPS로 노출

---

## 사전 체크리스트

### 1. DNS 설정 확인
가비아 또는 도메인 제공자에서:
```
api.hohyun.site → 54.180.100.87 (A 레코드)
```
**확인 방법:**
```bash
# 로컬에서 확인
nslookup api.hohyun.site
# 또는
dig api.hohyun.site
```

### 2. EC2 보안 그룹 확인
AWS Console → EC2 → Security Groups:
- **포트 80 (HTTP)**: `0.0.0.0/0` 허용
- **포트 443 (HTTPS)**: `0.0.0.0/0` 허용
- **포트 22 (SSH)**: 기존 설정 유지

### 3. API Gateway 동작 확인
EC2에 SSH 접속 후:
```bash
# 컨테이너가 실행 중인지 확인
sudo docker ps | grep api-gateway

# Health Check
curl http://localhost:8080/actuator/health
```

---

## 단계별 설치 가이드

### Step 1: EC2에 SSH 접속
```bash
ssh ubuntu@54.180.100.87
# 또는
ssh -i your-key.pem ubuntu@54.180.100.87
```

### Step 2: Nginx 설치
```bash
# 패키지 목록 업데이트
sudo apt update

# Nginx 설치
sudo apt install -y nginx

# Nginx 자동 시작 설정
sudo systemctl enable nginx

# Nginx 시작
sudo systemctl start nginx

# 상태 확인
sudo systemctl status nginx
```

**예상 출력:** `Active: active (running)`

### Step 3: 방화벽 설정 (UFW 사용 시)
```bash
# Nginx Full (80, 443) 허용
sudo ufw allow 'Nginx Full'

# 상태 확인
sudo ufw status
```

### Step 4: 기본 Nginx 설정 테스트
브라우저에서 확인:
```
http://54.180.100.87
```
**예상:** "Welcome to nginx!" 페이지 표시

### Step 5: Nginx 리버스 프록시 설정

#### 5-1) 설정 파일 생성
```bash
sudo nano /etc/nginx/sites-available/api.hohyun.site
```

#### 5-2) 다음 내용 입력 (포트 8080으로 프록시)
```nginx
server {
    listen 80;
    server_name api.hohyun.site;

    # 로그 설정
    access_log /var/log/nginx/api.hohyun.site.access.log;
    error_log /var/log/nginx/api.hohyun.site.error.log;

    # API Gateway로 프록시
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        
        # 프록시 헤더 설정
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        
        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # WebSocket 지원 (필요 시)
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 큰 파일 업로드 지원
        client_max_body_size 100M;
    }
    
    # Health Check 엔드포인트 (선택사항)
    location /actuator/health {
        proxy_pass http://127.0.0.1:8080/actuator/health;
        access_log off;
    }
}
```

**저장:** `Ctrl + O`, `Enter`, `Ctrl + X`

#### 5-3) 설정 파일 활성화
```bash
# 심볼릭 링크 생성
sudo ln -s /etc/nginx/sites-available/api.hohyun.site /etc/nginx/sites-enabled/

# 기본 설정 비활성화 (선택사항)
sudo rm /etc/nginx/sites-enabled/default
```

#### 5-4) Nginx 설정 문법 검사
```bash
sudo nginx -t
```

**예상 출력:**
```
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

#### 5-5) Nginx 재시작
```bash
sudo systemctl reload nginx
# 또는
sudo systemctl restart nginx
```

### Step 6: HTTP 프록시 테스트
브라우저에서 확인:
```
http://api.hohyun.site
http://api.hohyun.site/docs
http://api.hohyun.site/actuator/health
```

**예상:** API Gateway가 정상적으로 응답

---

## HTTPS 설정 (Let's Encrypt)

### Step 7: Certbot 설치
```bash
sudo apt install -y certbot python3-certbot-nginx
```

### Step 8: SSL 인증서 발급 및 자동 설정
```bash
sudo certbot --nginx -d api.hohyun.site
```

**진행 과정:**
1. **이메일 입력**: 인증서 만료 알림용
2. **약관 동의**: `Y` 입력
3. **이메일 공유 여부**: 선택 (보통 `N`)
4. **HTTP → HTTPS 리다이렉트**: `2` 선택 (권장)

**예상 출력:**
```
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/api.hohyun.site/fullchain.pem
Key is saved at:         /etc/letsencrypt/live/api.hohyun.site/privkey.pem
```

### Step 9: 자동 갱신 테스트
```bash
# 테스트 실행 (실제 갱신은 안 함)
sudo certbot renew --dry-run
```

**예상 출력:**
```
Congratulations, all renewals succeeded.
```

### Step 10: 자동 갱신 타이머 확인
```bash
systemctl list-timers | grep certbot
```

**예상 출력:**
```
certbot.timer  certbot.service  ...  n/a  ...  ...
```

---

## 최종 확인

### 1. HTTPS 접속 테스트
브라우저에서:
```
https://api.hohyun.site
https://api.hohyun.site/docs
https://api.hohyun.site/actuator/health
```

### 2. HTTP → HTTPS 리다이렉트 확인
```
http://api.hohyun.site
```
**예상:** 자동으로 `https://api.hohyun.site`로 리다이렉트

### 3. SSL 인증서 정보 확인
브라우저 주소창의 자물쇠 아이콘 클릭 → "인증서 보기"

---

## Nginx 설정 파일 최종 확인

설정 파일이 자동으로 업데이트되었는지 확인:
```bash
sudo cat /etc/nginx/sites-available/api.hohyun.site
```

**예상 내용:**
- `listen 443 ssl;` 블록 추가됨
- `ssl_certificate` 및 `ssl_certificate_key` 설정됨
- `listen 80;` 블록에 리다이렉트 추가됨

---

## 문제 해결

### Nginx가 시작되지 않을 때
```bash
# 에러 로그 확인
sudo tail -f /var/log/nginx/error.log

# 설정 파일 문법 검사
sudo nginx -t
```

### 프록시가 작동하지 않을 때
```bash
# API Gateway가 실행 중인지 확인
sudo docker ps | grep api-gateway

# 포트 8080이 리스닝 중인지 확인
sudo ss -tulpn | grep :8080

# Nginx 액세스 로그 확인
sudo tail -f /var/log/nginx/api.hohyun.site.access.log
```

### SSL 인증서 발급 실패 시
```bash
# DNS가 올바르게 설정되었는지 확인
nslookup api.hohyun.site

# 포트 80이 열려있는지 확인
sudo ufw status
sudo ss -tulpn | grep :80

# Certbot 로그 확인
sudo tail -f /var/log/letsencrypt/letsencrypt.log
```

### 인증서 갱신 실패 시
```bash
# 수동 갱신 시도
sudo certbot renew --force-renewal

# 갱신 후 Nginx 재시작
sudo systemctl reload nginx
```

---

## 추가 설정 (선택사항)

### 1. 보안 헤더 강화
`/etc/nginx/sites-available/api.hohyun.site`의 `server` 블록에 추가:
```nginx
# HSTS (HTTP Strict Transport Security)
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

# X-Frame-Options
add_header X-Frame-Options "SAMEORIGIN" always;

# X-Content-Type-Options
add_header X-Content-Type-Options "nosniff" always;

# X-XSS-Protection
add_header X-XSS-Protection "1; mode=block" always;
```

### 2. Gzip 압축 활성화
`/etc/nginx/nginx.conf`의 `http` 블록에 추가 (이미 있을 수 있음):
```nginx
gzip on;
gzip_vary on;
gzip_min_length 1024;
gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
```

---

## 최종 구조

```
사용자 브라우저
    ↓ (HTTPS)
Nginx (포트 443)
    ↓ (HTTP, 내부)
API Gateway Docker Container (포트 8080)
    ↓
NeonDB, Redis, AI Services
```

---

## 다음 단계

### Vercel 환경 변수 업데이트
Vercel 대시보드에서:
```
NEXT_PUBLIC_API_URL=https://api.hohyun.site
```

### www.hohyun.site 환경 변수 업데이트
`.env.local` 또는 Vercel 환경 변수:
```
NEXT_PUBLIC_API_URL=https://api.hohyun.site
```

---

## 유지보수

### 인증서 갱신 확인 (매월)
```bash
sudo certbot renew --dry-run
```

### Nginx 로그 확인
```bash
# 액세스 로그
sudo tail -f /var/log/nginx/api.hohyun.site.access.log

# 에러 로그
sudo tail -f /var/log/nginx/api.hohyun.site.error.log
```

### Nginx 재시작 (설정 변경 후)
```bash
sudo nginx -t && sudo systemctl reload nginx
```

---

## 완료 체크리스트

- [ ] DNS 설정 완료 (api.hohyun.site → 54.180.100.87)
- [ ] EC2 보안 그룹 포트 80, 443 열림
- [ ] Nginx 설치 및 시작 완료
- [ ] HTTP 프록시 작동 확인
- [ ] SSL 인증서 발급 완료
- [ ] HTTPS 접속 확인
- [ ] HTTP → HTTPS 리다이렉트 확인
- [ ] 자동 갱신 테스트 성공
- [ ] Vercel 환경 변수 업데이트

---

## 참고사항

- **인증서 만료**: 90일마다 자동 갱신 (Let's Encrypt)
- **Nginx 재시작**: 설정 변경 후 `sudo systemctl reload nginx`
- **로그 위치**: `/var/log/nginx/`
- **설정 파일**: `/etc/nginx/sites-available/api.hohyun.site`

