# SSH 타임아웃 문제 해결 가이드

## 🔍 확인해야 할 사항

### 1. EC2 인스턴스 상태 확인

```bash
# EC2 콘솔에서 확인:
- 인스턴스 상태: "running"인지 확인
- 상태 확인: 2/2 체크 통과했는지 확인
- CPU/메모리 사용률: 과도한 리소스 사용 여부
```

### 2. EC2 보안 그룹 설정 확인

**인바운드 규칙:**
```
SSH (22) - 221.148.97.228/32 ✅ (본인 IP)
```

**확인 사항:**
- GitHub Actions runner의 IP가 변경되었을 수 있음
- 일시적으로 `0.0.0.0/0`로 열어서 테스트 (테스트 후 다시 제한)

### 3. EC2 네트워크 ACL 확인

- VPC의 네트워크 ACL이 SSH 트래픽을 차단하지 않는지 확인

### 4. EC2 인스턴스의 SSH 데몬 설정 확인

EC2에 직접 접속하여 확인:

```bash
# SSH 데몬 설정 확인
sudo cat /etc/ssh/sshd_config | grep -E "ClientAliveInterval|ClientAliveCountMax|TCPKeepAlive"

# 권장 설정:
# ClientAliveInterval 60
# ClientAliveCountMax 3
# TCPKeepAlive yes
```

### 5. EC2 시스템 리소스 확인

```bash
# CPU 사용률 확인
top

# 메모리 확인
free -h

# 디스크 공간 확인
df -h

# Docker 컨테이너 상태
sudo docker ps -a
sudo docker stats
```

### 6. EC2 로그 확인

```bash
# 시스템 로그 확인
sudo journalctl -u ssh -n 50

# 커널 로그 확인
dmesg | tail -50
```

### 7. GitHub Actions Runner 네트워크 확인

- GitHub Actions의 runner가 EC2에 접근할 수 있는 네트워크 경로 확인
- 방화벽이나 프록시가 없는지 확인

## 🔧 즉시 시도해볼 수 있는 해결책

### 방법 1: SSH Keep-Alive 설정 추가

워크플로우에 SSH keep-alive 설정 추가

### 방법 2: 타임아웃 허용 (배포는 성공)

`continue-on-error: true`로 설정하여 SSH 타임아웃이 있어도 배포 성공으로 표시

### 방법 3: 명령어를 여러 단계로 분리

SSH 액션을 여러 개의 작은 명령어로 분리하여 각각 타임아웃 설정

## 📊 현재 상황 분석

**좋은 소식:**
- ✅ 배포 스크립트는 성공적으로 완료됨
- ✅ 컨테이너가 정상적으로 실행됨
- ✅ Health check 통과

**문제:**
- ❌ SSH 연결 종료 시 타임아웃 발생
- ❌ GitHub Actions가 실패로 표시됨

**결론:**
배포 자체는 성공했지만, SSH 세션 종료 과정에서 네트워크 문제가 발생하고 있습니다.

