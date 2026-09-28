@echo off
setlocal
title Stop Raksha Demo Backend

echo Stopping any backend processes running on port 8000...

for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8000 " ^| findstr "LISTENING"') do (
    echo Terminating PID: %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo Backend server on port 8000 stopped.
ping 127.0.0.1 -n 2 >nul
