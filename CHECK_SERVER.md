# 백엔드 서버 상태 확인 및 재시작 가이드

## 문제: ERR_CONNECTION_REFUSED

이 오류는 백엔드 서버가 실행되지 않았거나 접근할 수 없을 때 발생합니다.

## 해결 방법

### 1. EC2 서버에서 백엔드 서버 상태 확인

```bash
# EC2 서버에 SSH 접속
ssh your-ec2-user@your-ec2-ip

# Spring Boot 애플리케이션이 실행 중인지 확인
ps aux | grep java
# 또는
systemctl status your-spring-boot-service

# 포트 8080이 리스닝 중인지 확인
sudo netstat -tlnp | grep 8080
# 또는
sudo ss -tlnp | grep 8080
```

### 2. Docker로 실행 중인 경우

```bash
# Docker 컨테이너 상태 확인
docker ps -a | grep gateway

# 컨테이너가 중지되어 있으면 시작
docker start api-gateway

# 또는 docker-compose 사용 시
cd /path/to/your/project
docker-compose up -d gateway
```

### 3. Spring Boot 애플리케이션 직접 실행 중인 경우

```bash
# 프로세스 확인
ps aux | grep "gateway"

# 실행 중이 아니면 시작
cd /path/to/api.hohyun.site
./gradlew :gateway:bootRun

# 또는 JAR 파일로 실행
java -jar gateway/build/libs/gateway-*.jar
```

### 4. Nginx 설정 적용 확인

```bash
# Nginx 설정 테스트
sudo nginx -t

# Nginx 재시작
sudo systemctl restart nginx
# 또는
sudo service nginx restart

# Nginx 상태 확인
sudo systemctl status nginx
```

### 5. 방화벽 확인

```bash
# 포트 8080이 열려있는지 확인
sudo ufw status
# 또는
sudo iptables -L -n | grep 8080

# 포트가 닫혀있으면 열기
sudo ufw allow 8080/tcp
```

### 6. 로그 확인

```bash
# Spring Boot 애플리케이션 로그 확인
tail -f /path/to/logs/application.log

# Docker 로그 확인
docker logs api-gateway -f

# Nginx 에러 로그 확인
sudo tail -f /var/log/nginx/api.hohyun.site.error.log

# Nginx 액세스 로그 확인
sudo tail -f /var/log/nginx/api.hohyun.site.access.log
```

## 빠른 진단 명령어

```bash
# 모든 상태를 한 번에 확인
echo "=== Spring Boot 프로세스 ===" && ps aux | grep java | grep -v grep
echo "=== 포트 8080 리스닝 ===" && sudo netstat -tlnp | grep 8080
echo "=== Nginx 상태 ===" && sudo systemctl status nginx | head -5
echo "=== Docker 컨테이너 ===" && docker ps | grep gateway
```

## 일반적인 해결 순서

1. **백엔드 서버가 실행 중인지 확인**
   ```bash
   ps aux | grep java
   ```

2. **포트 8080이 열려있는지 확인**
   ```bash
   sudo netstat -tlnp | grep 8080
   ```

3. **Nginx 설정이 올바른지 확인**
   ```bash
   sudo nginx -t
   ```

4. **Nginx 재시작**
   ```bash
   sudo systemctl restart nginx
   ```

5. **백엔드 서버 재시작**
   - Docker: `docker restart api-gateway`
   - 직접 실행: 애플리케이션 재시작

6. **로그 확인하여 오류 원인 파악**

## 로컬 개발 환경에서 테스트

로컬에서 백엔드를 실행하여 테스트:

```bash
cd api.hohyun.site
./gradlew :gateway:bootRun
```

그런 다음 브라우저에서 `http://localhost:8080/api/google/auth-url?frontend_url=...` 접속 테스트

