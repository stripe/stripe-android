@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  maestro startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and MAESTRO_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\maestro-cli-1.9.0.jar;%APP_HOME%\lib\maestro-orchestra-1.9.0.jar;%APP_HOME%\lib\maestro-orchestra-models-1.9.0.jar;%APP_HOME%\lib\maestro-client-1.9.0.jar;%APP_HOME%\lib\dadb-1.2.0.jar;%APP_HOME%\lib\apkanalyzer-30.3.0.jar;%APP_HOME%\lib\sdk-common-30.3.0.jar;%APP_HOME%\lib\maestro-ios-1.9.0.jar;%APP_HOME%\lib\kotlin-result-jvm-1.1.14.jar;%APP_HOME%\lib\okhttp-4.10.0.jar;%APP_HOME%\lib\grpc-okhttp-1.45.0.jar;%APP_HOME%\lib\okhttp-2.7.4.jar;%APP_HOME%\lib\okio-jvm-3.2.0.jar;%APP_HOME%\lib\sdklib-30.3.0.jar;%APP_HOME%\lib\repository-30.3.0.jar;%APP_HOME%\lib\shared-30.3.0.jar;%APP_HOME%\lib\ddmlib-30.3.0.jar;%APP_HOME%\lib\layoutlib-api-30.3.0.jar;%APP_HOME%\lib\dvlib-30.3.0.jar;%APP_HOME%\lib\common-30.3.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.6.20.jar;%APP_HOME%\lib\picocli-4.5.2.jar;%APP_HOME%\lib\jackson-dataformat-yaml-2.13.2.jar;%APP_HOME%\lib\jackson-annotations-2.13.2.jar;%APP_HOME%\lib\jackson-core-2.13.2.jar;%APP_HOME%\lib\jackson-module-kotlin-2.13.2.jar;%APP_HOME%\lib\jackson-databind-2.13.2.1.jar;%APP_HOME%\lib\jansi-2.4.0.jar;%APP_HOME%\lib\grpc-kotlin-stub-1.2.1.jar;%APP_HOME%\lib\protobuf-kotlin-3.19.4.jar;%APP_HOME%\lib\kotlinx-coroutines-core-1.3.3.jar;%APP_HOME%\lib\kotlin-reflect-1.5.31.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.6.20.jar;%APP_HOME%\lib\kotlin-stdlib-1.6.20.jar;%APP_HOME%\lib\grpc-stub-1.45.0.jar;%APP_HOME%\lib\grpc-netty-1.40.1.jar;%APP_HOME%\lib\grpc-protobuf-1.45.0.jar;%APP_HOME%\lib\slf4j-simple-1.7.36.jar;%APP_HOME%\lib\image-comparison-4.4.0.jar;%APP_HOME%\lib\grpc-protobuf-lite-1.45.0.jar;%APP_HOME%\lib\grpc-core-1.45.0.jar;%APP_HOME%\lib\grpc-api-1.45.0.jar;%APP_HOME%\lib\binary-resources-30.3.0.jar;%APP_HOME%\lib\baksmali-2.5.2.jar;%APP_HOME%\lib\util-2.5.2.jar;%APP_HOME%\lib\dexlib2-2.5.2.jar;%APP_HOME%\lib\jimfs-1.1.jar;%APP_HOME%\lib\guava-31.0.1-android.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\axml-2.1.2.jar;%APP_HOME%\lib\junit-platform-native-0.9.5.jar;%APP_HOME%\lib\snakeyaml-1.30.jar;%APP_HOME%\lib\javax.annotation-api-1.3.2.jar;%APP_HOME%\lib\error_prone_annotations-2.10.0.jar;%APP_HOME%\lib\netty-codec-http2-4.1.52.Final.jar;%APP_HOME%\lib\netty-handler-proxy-4.1.52.Final.jar;%APP_HOME%\lib\perfmark-api-0.23.0.jar;%APP_HOME%\lib\proto-google-common-protos-2.0.1.jar;%APP_HOME%\lib\aapt2-proto-7.0.0-beta04-7396180.jar;%APP_HOME%\lib\protos-30.3.0.jar;%APP_HOME%\lib\protobuf-java-3.19.4.jar;%APP_HOME%\lib\slf4j-api-1.7.36.jar;%APP_HOME%\lib\gson-2.9.0.jar;%APP_HOME%\lib\javax.inject-1.jar;%APP_HOME%\lib\kxml2-2.3.0.jar;%APP_HOME%\lib\bcpkix-jdk15on-1.67.jar;%APP_HOME%\lib\bcprov-jdk15on-1.67.jar;%APP_HOME%\lib\jaxb-runtime-2.3.2.jar;%APP_HOME%\lib\trove4j-1.0.20181211.jar;%APP_HOME%\lib\xercesImpl-2.12.0.jar;%APP_HOME%\lib\annotations-30.3.0.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.6.20.jar;%APP_HOME%\lib\junit-platform-console-1.7.2.jar;%APP_HOME%\lib\junit-platform-reporting-1.7.2.jar;%APP_HOME%\lib\junit-platform-launcher-1.7.2.jar;%APP_HOME%\lib\junit-jupiter-5.7.2.jar;%APP_HOME%\lib\junit-jupiter-engine-5.7.2.jar;%APP_HOME%\lib\junit-platform-engine-1.7.2.jar;%APP_HOME%\lib\junit-jupiter-params-5.7.2.jar;%APP_HOME%\lib\junit-jupiter-api-5.7.2.jar;%APP_HOME%\lib\junit-platform-commons-1.7.2.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\grpc-context-1.45.0.jar;%APP_HOME%\lib\failureaccess-1.0.1.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\checker-qual-3.12.0.jar;%APP_HOME%\lib\checker-compat-qual-2.5.5.jar;%APP_HOME%\lib\j2objc-annotations-1.3.jar;%APP_HOME%\lib\netty-codec-http-4.1.52.Final.jar;%APP_HOME%\lib\netty-handler-4.1.52.Final.jar;%APP_HOME%\lib\netty-codec-socks-4.1.52.Final.jar;%APP_HOME%\lib\netty-codec-4.1.52.Final.jar;%APP_HOME%\lib\netty-transport-4.1.52.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.52.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.52.Final.jar;%APP_HOME%\lib\netty-common-4.1.52.Final.jar;%APP_HOME%\lib\annotations-4.1.1.4.jar;%APP_HOME%\lib\animal-sniffer-annotations-1.19.jar;%APP_HOME%\lib\jna-platform-5.6.0.jar;%APP_HOME%\lib\commons-compress-1.20.jar;%APP_HOME%\lib\httpmime-4.5.6.jar;%APP_HOME%\lib\httpclient-4.5.6.jar;%APP_HOME%\lib\httpcore-4.4.13.jar;%APP_HOME%\lib\stax-ex-1.8.1.jar;%APP_HOME%\lib\jakarta.xml.bind-api-2.3.2.jar;%APP_HOME%\lib\txw2-2.3.2.jar;%APP_HOME%\lib\istack-commons-runtime-3.0.8.jar;%APP_HOME%\lib\FastInfoset-1.2.16.jar;%APP_HOME%\lib\jakarta.activation-api-1.2.1.jar;%APP_HOME%\lib\xml-apis-1.4.01.jar;%APP_HOME%\lib\jcommander-1.64.jar;%APP_HOME%\lib\apiguardian-api-1.1.0.jar;%APP_HOME%\lib\jna-5.6.0.jar;%APP_HOME%\lib\javax.activation-1.2.0.jar;%APP_HOME%\lib\opentest4j-1.2.0.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\commons-codec-1.10.jar


@rem Execute maestro
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %MAESTRO_OPTS%  -classpath "%CLASSPATH%" maestro.cli.AppKt %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable MAESTRO_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%MAESTRO_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
