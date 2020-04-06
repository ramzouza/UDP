cd C:\Github\UDP\target
Start-Process -FilePath java -ArgumentList '-jar httpf.jar -v' 
Start-Process -FilePath java -ArgumentList '-jar httpc.jar GET http://localhost:8007/'
Start-Process -FilePath java -ArgumentList '-jar httpc.jar GET http://localhost:8007/bob.txt/'
Start-Process -FilePath java -ArgumentList '-jar httpc.jar POST -h Content_Length:12 -d Hello World! http://localhost:8007/folder5/bob2.txt '
