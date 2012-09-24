cd rhino1_7R4
C:\Users\brian-window\apache-ant-1.8.4\bin\ant.bat jar && (del ..\js.jar || echo "") && copy build\rhino1_7R4\js.jar ..\. && cd .. && javac BatchCompiler.java -cp js.jar && java -cp js.jar;. BatchCompiler test.js
