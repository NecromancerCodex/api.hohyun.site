# CORS 중복 헤더 문제 해결 가이드

## 문제
- `Access-Control-Allow-Origin` 헤더가 중복으로 설정됨
- `*`와 특정 도메인(`https://hohyun.site`)이 함께 사용됨
- Spring Boot `CorsConfig.java`와 Nginx 양쪽에서 CORS 헤더 추가

## 해결 완료

### 1. Spring Boot CORS 설정 제거 ✅
- `CorsConfig.java` 삭제 완료
- `WebConfig.java`에서 CORS 설정 제거 완료
- `GroupChatSSEController.java`에서 `@CrossOrigin` 제거 완료

### 2. Nginx 설정 수정 ✅
- `Access-Control-Allow-Origin: *` → `Access-Control-Allow-Origin: https://hohyun.site`로 변경
- `*`와 `Access-Control-Allow-Credentials: true`는 함께 사용 불가능하므로 특정 도메인으로 변경

## EC2 서버에서 해야 할 작업

### 1. Nginx 설정 파일 확인
```bash
sudo cat /etc/nginx/sites-enabled/api.hohyun.site | grep -A 5 -B 5 'Access-Control'
```

### 2. CORS 헤더가 중복되어 있는지 확인
다음과 같이 여러 개가 나오면 안 됩니다:
```
Access-Control-Allow-Origin: https://hohyun.site
Access-Control-Allow-Origin: *
```

### 3. Nginx 설정 파일 수정
```bash
sudo nano /etc/nginx/sites-enabled/api.hohyun.site
```

**수정할 부분:**
1. SSE 엔드포인트 (`location /api/groupchat/stream`)에서:
   ```nginx
   add_header 'Access-Control-Allow-Origin' 'https://hohyun.site' always;
   ```
   - `*`가 있으면 제거
   - 하나만 남기기

2. HTTPS 설정 블록 (443 포트)에서도 동일하게:
   ```nginx
   add_header 'Access-Control-Allow-Origin' 'https://hohyun.site' always;
   ```

### 4. Nginx 설정 테스트
```bash
sudo nginx -t
```

### 5. Nginx 재시작
```bash
sudo systemctl restart nginx
```

### 6. 확인
```bash
# 헤더 확인
curl -I https://api.hohyun.site/api/groupchat/stream?lastId=0

# Access-Control-Allow-Origin이 한 번만 나와야 함
```

## 브라우저에서 테스트

1. **브라우저 캐시 지우기**
   - Chrome: `Ctrl + Shift + Delete` → 캐시된 이미지 및 파일 삭제
   - 또는 개발자 도구(F12) → Network 탭 → "Disable cache" 체크

2. **프론트엔드 접속**
   - `https://hohyun.site` 접속
   - 단체 채팅방 접속

3. **개발자 도구 확인**
   - Console 탭: CORS 오류가 사라져야 함
   - Network 탭: `api/groupchat/stream` 요청 → Headers 확인
     - `Access-Control-Allow-Origin: https://hohyun.site` (하나만)

## 예상 결과

✅ CORS 오류 해결
✅ SSE 연결 성공
✅ 실시간 메시징 정상 작동

