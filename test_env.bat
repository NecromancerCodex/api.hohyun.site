@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
echo ========================================
echo 환경 변수 테스트
echo ========================================
echo.

cd /d "%~dp0"

REM .env 파일 읽기 테스트
if exist ".env" (
    echo [.env 파일 발견]
    echo.
    echo [.env 파일 내용 (처음 10줄)]:
    type .env | more +1 | head -n 10
    echo.
    echo [환경 변수 로드 테스트...]
    set loaded_count=0
    for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
        set "line=%%a=%%b"
        if not "!line!"=="" (
            echo !line! | findstr /C:"=" >nul
            if not errorlevel 1 (
                for /f "tokens=1,* delims==" %%c in ("!line!") do (
                    set "env_key=%%c"
                    set "env_value=%%d"
                    for /f "tokens=*" %%e in ("!env_key!") do set "env_key=%%e"
                    for /f "tokens=*" %%e in ("!env_value!") do set "env_value=%%e"
                    if not "!env_key!"=="" (
                        set "!env_key!=!env_value!"
                        set /a loaded_count+=1
                        echo   [!loaded_count!] !env_key! = [설정됨]
                    )
                )
            )
        )
    )
    echo.
    echo [총 %loaded_count% 개 환경 변수 로드]
    echo.
) else (
    echo [오류] .env 파일이 없습니다.
    echo.
)

REM 환경 변수 확인
echo [현재 설정된 환경 변수 확인]
if defined SPRING_DATASOURCE_URL (
    echo   SPRING_DATASOURCE_URL: [설정됨]
    echo     값: %SPRING_DATASOURCE_URL%
) else (
    echo   SPRING_DATASOURCE_URL: [설정 안 됨]
)
if defined SPRING_DATASOURCE_USERNAME (
    echo   SPRING_DATASOURCE_USERNAME: [설정됨]
) else (
    echo   SPRING_DATASOURCE_USERNAME: [설정 안 됨]
)
if defined AI_SERVICE_RAG_URL (
    echo   AI_SERVICE_RAG_URL: [설정됨] = %AI_SERVICE_RAG_URL%
) else (
    echo   AI_SERVICE_RAG_URL: [설정 안 됨]
)
if defined AI_SERVICE_VISION_URL (
    echo   AI_SERVICE_VISION_URL: [설정됨] = %AI_SERVICE_VISION_URL%
) else (
    echo   AI_SERVICE_VISION_URL: [설정 안 됨]
)
echo.

pause

