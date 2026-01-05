@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
echo ========================================
echo API Gateway 서버 시작
echo Spring Boot Gateway
echo ========================================
echo.

REM 현재 스크립트 위치에서 api.hohyun.site로 이동
cd /d "%~dp0"

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
    set loaded_count=0
    for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
        set /a line_num+=1
        set "line=%%a=%%b"
        REM 빈 줄과 주석 줄 건너뛰기
        if not "!line!"=="" (
            REM = 기호가 있는지 확인 (환경 변수 형식)
            echo !line! | findstr /C:"=" >nul
            if not errorlevel 1 (
                REM 키와 값 분리
                for /f "tokens=1,* delims==" %%c in ("!line!") do (
                    set "env_key=%%c"
                    set "env_value=%%d"
                    REM 앞뒤 공백 제거
                    for /f "tokens=*" %%e in ("!env_key!") do set "env_key=%%e"
                    for /f "tokens=*" %%e in ("!env_value!") do set "env_value=%%e"
                    REM 환경 변수 설정
                    if not "!env_key!"=="" (
                        set "!env_key!=!env_value!"
                        set /a loaded_count+=1
                    )
                )
            )
        )
    )
    echo [.env 파일 로드 완료] (총 %line_num% 줄 중 %loaded_count% 개 환경 변수 로드)
    echo.
) else (
    echo [경고] .env 파일이 없습니다.
    echo 환경 변수를 수동으로 설정하거나 .env 파일을 생성하세요.
    echo.
    echo .env 파일 예시:
    echo   SPRING_DATASOURCE_URL=jdbc:postgresql://your-host/database
    echo   SPRING_DATASOURCE_USERNAME=your-username
    echo   SPRING_DATASOURCE_PASSWORD=your-password
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
    echo [문제 해결]
    echo 1. .env 파일이 api.hohyun.site 디렉토리에 있는지 확인
    echo 2. .env 파일 형식이 KEY=VALUE 형식인지 확인
    echo 3. 환경 변수 테스트: test_env.bat 실행
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

REM 환경 변수 확인 (디버깅용)
echo [환경 변수 확인]
echo AI_SERVICE_RAG_URL: %AI_SERVICE_RAG_URL%
echo AI_SERVICE_VISION_URL: %AI_SERVICE_VISION_URL%
if defined SPRING_DATASOURCE_URL (
    echo SPRING_DATASOURCE_URL: [설정됨]
    REM 값의 일부만 표시 (보안)
    for /f "tokens=1-3 delims=/" %%a in ("%SPRING_DATASOURCE_URL%") do (
        echo   URL 시작: %%a//%%b/...
    )
) else (
    echo SPRING_DATASOURCE_URL: [설정 안 됨]
)
if defined SPRING_DATASOURCE_USERNAME (
    echo SPRING_DATASOURCE_USERNAME: [설정됨]
) else (
    echo SPRING_DATASOURCE_USERNAME: [설정 안 됨]
)
if defined SPRING_DATA_REDIS_HOST (
    echo SPRING_DATA_REDIS_HOST: [설정됨]
) else (
    echo SPRING_DATA_REDIS_HOST: [설정 안 됨]
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

REM 환경 변수를 Gradle에 전달하기 위해 설정
REM Spring Boot는 시스템 환경 변수를 읽으므로, 여기서 설정한 변수들이 전달됨

REM Spring Boot 프로파일 설정 (선택사항)
REM 로컬 개발용 설정을 사용하려면 아래 주석을 해제하세요
REM set SPRING_PROFILES_ACTIVE=local

REM 환경 변수를 Gradle에 명시적으로 전달
REM Windows에서는 set 명령으로 설정한 환경 변수가 자식 프로세스에 자동 전달됨
REM 하지만 명시적으로 전달하는 것이 더 안전함

echo [Gradle 실행 중...]
echo 환경 변수가 Gradle 프로세스에 전달됩니다.
echo.
echo [디버깅] 주요 환경 변수 확인:
if defined SPRING_DATASOURCE_URL (
    echo   SPRING_DATASOURCE_URL: 설정됨
) else (
    echo   SPRING_DATASOURCE_URL: 설정 안 됨 - 서버 시작 실패 가능
)
if defined SPRING_DATASOURCE_USERNAME (
    echo   SPRING_DATASOURCE_USERNAME: 설정됨
) else (
    echo   SPRING_DATASOURCE_USERNAME: 설정 안 됨
)
echo.

REM Gradle로 Spring Boot 실행
REM Windows 배치 파일에서 설정한 환경 변수는 자식 프로세스에 자동 전달됨
echo [Gradle 빌드 및 실행 시작...]
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
