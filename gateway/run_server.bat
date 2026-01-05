@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
echo ========================================
echo API Gateway 서버 시작
echo Spring Boot Gateway
echo ========================================
echo.

REM 현재 스크립트 위치에서 api.hohyun.site로 이동
cd /d "%~dp0\.."

REM Gradle Wrapper 확인
if not exist "gradlew.bat" (
    echo [오류] gradlew.bat을 찾을 수 없습니다.
    echo api.hohyun.site 디렉토리에서 실행해주세요.
    pause
    exit /b 1
)

REM .env 파일이 있으면 읽어서 환경 변수로 설정
if exist ".env" (
    echo [.env 파일 로드 중...]
    set line_num=0
    for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
        set /a line_num+=1
        if not "%%a"=="" (
            REM 공백 제거
            set "key=%%a"
            set "value=%%b"
            REM 값의 앞뒤 공백 제거
            for /f "tokens=*" %%c in ("!value!") do set "value=%%c"
            REM 환경 변수 설정
            set "!key!=!value!"
        )
    )
    echo [.env 파일 로드 완료] (총 %line_num% 줄 처리)
    echo.
) else (
    echo [경고] .env 파일이 없습니다.
    echo 환경 변수를 수동으로 설정하거나 .env 파일을 생성하세요.
    echo.
)

REM 환경 변수 설정 (기본값 또는 .env에서 로드된 값)
if not defined AI_SERVICE_RAG_URL set AI_SERVICE_RAG_URL=http://localhost:8001
if not defined AI_SERVICE_VISION_URL set AI_SERVICE_VISION_URL=http://localhost:8002

REM 필수 환경 변수 확인
if not defined SPRING_DATASOURCE_URL (
    echo [경고] SPRING_DATASOURCE_URL이 설정되지 않았습니다.
    echo 데이터베이스 연결이 필요합니다.
    echo.
    echo .env 파일 예시:
    echo   SPRING_DATASOURCE_URL=jdbc:postgresql://your-host/database
    echo   SPRING_DATASOURCE_USERNAME=your-username
    echo   SPRING_DATASOURCE_PASSWORD=your-password
    echo   SPRING_DATA_REDIS_HOST=your-redis-host
    echo   SPRING_DATA_REDIS_PORT=6379
    echo   SPRING_DATA_REDIS_PASSWORD=your-redis-password
    echo   JWT_SECRET=your-jwt-secret
    echo.
    echo 계속하시겠습니까? (Y/N)
    set /p continue=
    if /i not "%continue%"=="Y" (
        echo 서버 시작을 취소했습니다.
        pause
        exit /b 1
    )
    echo.
)

echo [환경 변수 확인]
echo AI_SERVICE_RAG_URL: %AI_SERVICE_RAG_URL%
echo AI_SERVICE_VISION_URL: %AI_SERVICE_VISION_URL%
if defined SPRING_DATASOURCE_URL (
    echo SPRING_DATASOURCE_URL: [설정됨]
) else (
    echo SPRING_DATASOURCE_URL: [설정 안 됨]
)
echo.
echo [서버 시작 중...]
echo 포트: 8080
echo API 문서: http://localhost:8080/docs
echo 헬스 체크: http://localhost:8080/actuator/health
echo.
echo 프록시 엔드포인트:
echo   - Chat Service: http://localhost:8080/api/rag/llama/rag
echo   - YOLO: http://localhost:8080/api/yolo/detect
echo   - Diffusers: http://localhost:8080/api/diffusers/api/v1/generate
echo.
echo 서버를 중지하려면 Ctrl+C를 누르세요.
echo.

echo [Gradle 실행 중...]
echo 환경 변수가 Gradle 프로세스에 전달됩니다.
echo.

REM Gradle로 Spring Boot 실행
call gradlew.bat :gateway:bootRun

if errorlevel 1 (
    echo.
    echo [오류] 서버 시작 실패
    echo.
    echo 문제 해결 방법:
    echo 1. .env 파일의 환경 변수가 올바른지 확인
    echo 2. 데이터베이스 연결 정보 확인
    echo 3. 포트 8080이 사용 중인지 확인
    echo 4. 로그를 확인하여 구체적인 오류 메시지 확인
    echo.
)

pause
