javac -cp build\js-batch.jar;lib\batches.jar src\*.java && java -cp build\js-batch.jar;lib\batches.jar;src BatchCompiler bin\test.js > bin\compiled-test.js
java -cp "rhino1_7R4\js.jar;lib\batches.jar;lib\jackson-core-asl-1.3.2.jar" org.mozilla.javascript.tools.shell.Main "bin\rhino-prelude.js"
