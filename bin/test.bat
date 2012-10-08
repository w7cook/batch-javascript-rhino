set CLASSPATH=build\js-batch.jar;lib\batches.jar
javac src\*.java && java -cp %CLASSPATH%;src BatchCompiler bin\test.js
